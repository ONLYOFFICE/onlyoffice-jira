/**
 *
 * (c) Copyright Ascensio System SIA 2025
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
import com.atlassian.plugin.webresource.UrlMode;
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
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import onlyoffice.sdk.manager.url.UrlManager;
import onlyoffice.utils.AttachmentUtil;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class OnlyOfficeEditorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final TemplateRenderer templateRenderer;
    private final LocaleManager localeManager;
    private final I18nResolver i18nResolver;
    private final WebResourceAssemblerFactory webResourceAssemblerFactory;
    private final AttachmentUtil attachmentUtil;

    private final DocumentManager documentManager;
    private final UrlManager urlManager;
    private final JwtManager jwtManager;
    private final SettingsManager settingsManager;
    private final ConfigService configService;

    public OnlyOfficeEditorServlet(final JiraAuthenticationContext jiraAuthenticationContext,
                                   final I18nResolver i18nResolver, final TemplateRenderer templateRenderer,
                                   final LocaleManager localeManager,
                                   final WebResourceAssemblerFactory webResourceAssemblerFactory,
                                   final AttachmentUtil attachmentUtil,
                                   final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.i18nResolver = i18nResolver;

        this.templateRenderer = templateRenderer;
        this.localeManager = localeManager;
        this.webResourceAssemblerFactory = webResourceAssemblerFactory;
        this.attachmentUtil = attachmentUtil;

        this.documentManager = docsIntegrationSdkContext.getDocumentManager();
        this.urlManager = (UrlManager) docsIntegrationSdkContext.getUrlManager();
        this.jwtManager = docsIntegrationSdkContext.getJwtManager();
        this.settingsManager = docsIntegrationSdkContext.getSettingsManager();
        this.configService = docsIntegrationSdkContext.getConfigService();
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        String attachmentIdString = request.getParameter("attachmentId");
        Long attachmentId = Long.parseLong(attachmentIdString);
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
        Map<String, Object> context = getDefaultContext();

        Attachment attachment = attachmentUtil.getAttachment(attachmentId);

        if (attachment == null || !attachmentUtil.checkAccess(attachmentId, user, false)) {
            if (jiraAuthenticationContext.isLoggedInUser()) {
                context.put("errorMessage", i18nResolver.getText("onlyoffice.connector.error.AccessDenied"));
                render(context, response);
            } else {
                sendRedirectToLogin(request, response);
            }
            return;
        }

        String fileName = documentManager.getDocumentName(String.valueOf(attachmentId));
        DocumentType documentType = documentManager.getDocumentType(fileName);

        context.put("attachmentId", attachmentId);
        context.put("docTitle", fileName);

        if (documentType == null) {
            context.put(
                    "errorMessage",
                    i18nResolver.getText("onlyoffice.connector.error.NotSupportedFormat")
                            + "(." + documentManager.getExtension(fileName) + ")"
            );

            render(context, response);
            return;
        }

        Config config = configService.createConfig(
                attachmentId.toString(),
                Mode.EDIT,
                request.getHeader("User-Agent")
        );

        config.getEditorConfig().setLang(localeManager.getLocaleFor(user).toLanguageTag());

        if (settingsManager.isSecurityEnabled()) {
            config.setToken(jwtManager.createToken(config));
        }

        ObjectMapper mapper = createObjectMapper();
        String shardKey = config.getDocument().getKey();

        context.put("docserviceApiUrl", urlManager.getDocumentServerApiUrl(shardKey));
        context.put("configAsHtml", mapper.writeValueAsString(config));
        context.put("demo", settingsManager.isDemoActive());
        context.put("favicon", urlManager.getFaviconUrl(documentType));

        if (config.getDocument().getPermissions().getEdit()) {
            context.put("saveAsUrl", urlManager.getSaveAsUrl(attachmentId));
        }

        render(context, response);
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

    private Map<String, Object> getDefaultContext() {
        Map<String, Object> context = new HashMap<>();

        context.put("docserviceApiUrl", urlManager.getDocumentServerApiUrl());

        return context;
    }

    private void render(final Map<String, Object> context, final HttpServletResponse response) throws IOException {
        WebResourceAssembler webResourceAssembler =
                webResourceAssemblerFactory.create().includeSuperbatchResources(true).build();
        webResourceAssembler.resources().requireWebResource("onlyoffice.onlyoffice-jira-app:editor-page-resources");
        webResourceAssembler.assembled().drainIncludedResources()
                .writeHtmlTags(response.getWriter(), UrlModeUtils.convert(UrlMode.AUTO));

        response.setContentType("text/html;charset=UTF-8");
        templateRenderer.render("templates/editor.vm", context, response.getWriter());
    }

    private void sendRedirectToLogin(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        String currentURL = request.getRequestURI() + "?" + request.getQueryString();
        String query = "?permissionViolation=true&os_destination=" + URLEncoder.encode(currentURL, "UTF-8");
        response.sendRedirect("/login.jsp" + query);
    }
}
