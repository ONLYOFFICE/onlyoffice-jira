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

package onlyoffice.servlet;

import com.atlassian.annotations.security.AnonymousSiteAccess;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Config;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.model.documenteditor.config.editorconfig.Mode;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import onlyoffice.sdk.manager.url.UrlManager;
import onlyoffice.utils.AttachmentUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@AnonymousSiteAccess
public class OnlyOfficeEditorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final TemplateRenderer templateRenderer;
    private final LocaleManager localeManager;
    private final I18nResolver i18nResolver;
    private final AttachmentUtil attachmentUtil;

    private final DocumentManager documentManager;
    private final UrlManager urlManager;
    private final SettingsManager settingsManager;
    private final ConfigService configService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnlyOfficeEditorServlet(final JiraAuthenticationContext jiraAuthenticationContext,
                                   final I18nResolver i18nResolver, final TemplateRenderer templateRenderer,
                                   final LocaleManager localeManager,
                                   final AttachmentUtil attachmentUtil,
                                   final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.i18nResolver = i18nResolver;

        this.templateRenderer = templateRenderer;
        this.localeManager = localeManager;
        this.attachmentUtil = attachmentUtil;

        this.documentManager = docsIntegrationSdkContext.getDocumentManager();
        this.urlManager = (UrlManager) docsIntegrationSdkContext.getUrlManager();
        this.settingsManager = docsIntegrationSdkContext.getSettingsManager();
        this.configService = docsIntegrationSdkContext.getConfigService();
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        String attachmentIdString = request.getParameter("attachmentId");
        String actionDataString = request.getParameter("actionData");
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
        Map<String, Object> context = getDefaultContext();

        if (attachmentIdString == null || attachmentIdString.isEmpty()) {
            String issueIdString = request.getParameter("issueId");

            Issue issue = null;
            if (issueIdString != null && !issueIdString.isEmpty()) {
                try {
                    Long issueId = Long.parseLong(issueIdString);
                    issue = attachmentUtil.getIssue(issueId);
                } catch (NumberFormatException ignored) {
                }
            }

            if (issue == null || !attachmentUtil.checkCreateAccess(issue, user)) {
                if (jiraAuthenticationContext.isLoggedInUser()) {
                    context.put("errorMessage", i18nResolver.getText("attachment.service.error.create.no.permission"));
                    render(context, response);
                } else {
                    sendRedirectToLogin(request, response);
                }
                return;
            }

            context.put("issueId", issueIdString);
            context.put("documentType", request.getParameter("documentType"));
            context.put("fileName", request.getParameter("fileName"));
            render(context, response);
            return;
        }

        Long attachmentId = Long.parseLong(attachmentIdString);
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

        Issue issue = attachment.getIssue();
        String fileName = documentManager.getDocumentName(String.valueOf(attachmentId));
        DocumentType documentType = documentManager.getDocumentType(fileName);

        context.put("issueId", issue.getId());
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
        config.getEditorConfig().setActionLink(getActionLink(actionDataString));

        String shardKey = config.getDocument().getKey();

        context.put("docserviceApiUrl", urlManager.getDocumentServerApiUrl(shardKey));
        context.put("documentType", documentType.toString().toLowerCase());
        context.put("config", objectMapper.writeValueAsString(config));
        context.put("demo", settingsManager.isDemoActive());
        context.put("favicon", urlManager.getFaviconUrl(documentType));
        context.put("canSaveAs", config.getDocument().getPermissions().getEdit());

        render(context, response);
    }

    private Map<String, Object> getDefaultContext() {
        Map<String, Object> context = new HashMap<>();

        context.put("docserviceApiUrl", urlManager.getDocumentServerApiUrl());
        context.put("apiPath", urlManager.getApiPath());

        return context;
    }

    private void render(final Map<String, Object> context, final HttpServletResponse response) throws IOException {
        templateRenderer.render("templates/editor.vm", context, response.getWriter());
    }

    private void sendRedirectToLogin(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        String currentURL = request.getRequestURI() + "?" + request.getQueryString();
        String query = "?permissionViolation=true&os_destination=" + URLEncoder.encode(currentURL, "UTF-8");
        response.sendRedirect("/login.jsp" + query);
    }

    private Object getActionLink(final String actionData) throws JsonProcessingException {
        if (Objects.isNull(actionData) || actionData.isEmpty()) {
            return null;
        }

        return objectMapper.readValue(actionData, Object.class);
    }
}
