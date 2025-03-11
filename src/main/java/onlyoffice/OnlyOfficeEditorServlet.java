/**
 *
 * (c) Copyright Ascensio System SIA 2023
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package onlyoffice;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.atlassian.plugin.webresource.assembler.UrlModeUtils;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
import com.atlassian.webresource.api.assembler.WebResourceAssemblerFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import onlyoffice.sdk.manager.url.UrlManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Scanned
public class OnlyOfficeEditorServlet extends HttpServlet {
    @JiraImport
    private final JiraAuthenticationContext jiraAuthenticationContext;
    @JiraImport
    private final TemplateRenderer templateRenderer;
    @JiraImport
    private final LocaleManager localeManager;
    @JiraImport
    private final I18nResolver i18n;
    @JiraImport
    private final WebResourceUrlProvider webResourceUrlProvider;
    @JiraImport
    private final WebResourceAssemblerFactory webResourceAssemblerFactory;
    private final AttachmentUtil attachmentUtil;

    private final DocumentManager documentManager;
    private final UrlManager urlManager;
    private final JwtManager jwtManager;
    private final SettingsManager settingsManager;
    private final ConfigService configService;
    private final DocumentServerClient documentServerClient;

    @Inject
    public OnlyOfficeEditorServlet(final JiraAuthenticationContext jiraAuthenticationContext,
                                   final I18nResolver i18n, final TemplateRenderer templateRenderer,
                                   final LocaleManager localeManager,
                                   final WebResourceUrlProvider webResourceUrlProvider,
                                   final WebResourceAssemblerFactory webResourceAssemblerFactory,
                                   final AttachmentUtil attachmentUtil,
                                   final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.i18n = i18n;

        this.templateRenderer = templateRenderer;
        this.localeManager = localeManager;
        this.webResourceUrlProvider = webResourceUrlProvider;
        this.webResourceAssemblerFactory = webResourceAssemblerFactory;
        this.attachmentUtil = attachmentUtil;

        this.documentManager = docsIntegrationSdkContext.getDocumentManager();
        this.urlManager = (UrlManager) docsIntegrationSdkContext.getUrlManager();
        this.jwtManager = docsIntegrationSdkContext.getJwtManager();
        this.settingsManager = docsIntegrationSdkContext.getSettingsManager();
        this.configService = docsIntegrationSdkContext.getConfigService();
        this.documentServerClient = docsIntegrationSdkContext.getDocumentServerClient();
    }

    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeEditorServlet");
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        String attachmentIdString = request.getParameter("attachmentId");

        if (!jiraAuthenticationContext.isLoggedInUser()) {
            String currentURL = request.getRequestURI() + "?" + request.getQueryString();
            String query = "?permissionViolation=true&os_destination=" + URLEncoder.encode(currentURL, "UTF-8");
            response.sendRedirect("/login.jsp" + query);
            return;
        }

        try {
            Long attachmentId = Long.parseLong(attachmentIdString);
            Attachment attachment = attachmentUtil.getAttachment(attachmentId);
            String fileName = documentManager.getDocumentName(String.valueOf(attachmentId));
            DocumentType documentType = documentManager.getDocumentType(fileName);

            if (attachment == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (!attachmentUtil.checkAccess(attachmentId, user, false)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN); //ToDo
                return;
            }

            Map<String, Object> context = new HashMap<String, Object>();
            context.put("attachmentId", attachmentId);
            context.put("docTitle", documentManager.getDocumentName(String.valueOf(attachmentId)));
            context.put("favicon", webResourceUrlProvider.getStaticPluginResourceUrl(
                    "onlyoffice.onlyoffice-jira-app:editor-page-resources",
                    documentType + ".ico", UrlMode.ABSOLUTE));
            context.put("docserviceApiUrl", urlManager.getDocumentServerApiUrl());
            context.put("saveAsAsHtml", urlManager.getSaveAsObject(attachmentId, user).toString());

            if (documentType != null) {

                Config config = configService.createConfig(
                        attachmentId.toString(),
                        Mode.EDIT,
                        request.getHeader("User-Agent")
                );

                config.getEditorConfig().setLang(localeManager.getLocaleFor(user).toLanguageTag());

                if (settingsManager.isSecurityEnabled()) {
                    config.setToken(jwtManager.createToken(config));
                }

                String shardKey = config.getDocument().getKey();

                ObjectMapper mapper = createObjectMapper();
                context.put("docserviceApiUrl", urlManager.getDocumentServerApiUrl(shardKey));
                context.put("configAsHtml", mapper.writeValueAsString(config));
                context.put("demo", settingsManager.isDemoActive());

            } else {
                context.put("errorMessage", i18n.getText("onlyoffice.connector.error.NotSupportedFormat") + "(."
                        + documentManager.getExtension(fileName) + ")");
            }

            WebResourceAssembler webResourceAssembler =
                    webResourceAssemblerFactory.create().includeSuperbatchResources(true).build();
            webResourceAssembler.resources().requireWebResource("onlyoffice.onlyoffice-jira-app:editor-page-resources");
            webResourceAssembler.assembled().drainIncludedResources()
                    .writeHtmlTags(response.getWriter(), UrlModeUtils.convert(UrlMode.AUTO));
            response.setContentType("text/html;charset=UTF-8");

            templateRenderer.render("templates/editor.vm", context, response.getWriter());
        } catch (Exception e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(JSONObject.class, new JsonSerializer<JSONObject>() {
            @Override
            public void serialize(final JSONObject jsonObject, final JsonGenerator jsonGenerator,
                                  final SerializerProvider serializerProvider) throws IOException {
                jsonGenerator.writeObject(jsonObject.toMap());
            }
        });
        objectMapper.registerModule(module);

        return objectMapper;
    }
}
