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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final JwtManager jwtManager;
    private final UrlManager urlManager;
    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final ConfigurationManager configurationManager;
    private final DemoManager demoManager;

    @Inject
    public OnlyOfficeEditorServlet(final JiraAuthenticationContext jiraAuthenticationContext,
                                   final I18nResolver i18n, final UrlManager urlManager, final JwtManager jwtManager,
                                   final DocumentManager documentManager, final AttachmentUtil attachmentUtil,
                                   final TemplateRenderer templateRenderer, final LocaleManager localeManager,
                                   final WebResourceUrlProvider webResourceUrlProvider,
                                   final WebResourceAssemblerFactory webResourceAssemblerFactory,
                                   final ConfigurationManager configurationManager, final DemoManager demoManager) {

        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.i18n = i18n;

        this.jwtManager = jwtManager;
        this.urlManager = urlManager;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.templateRenderer = templateRenderer;
        this.localeManager = localeManager;
        this.webResourceUrlProvider = webResourceUrlProvider;
        this.webResourceAssemblerFactory = webResourceAssemblerFactory;
        this.configurationManager = configurationManager;
        this.demoManager = demoManager;
    }

    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeEditorServlet");
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (!jiraAuthenticationContext.isLoggedInUser()) {
            String currentURL = request.getRequestURI() + "?" + request.getQueryString();
            String query = "?permissionViolation=true&os_destination=" + URLEncoder.encode(currentURL, "UTF-8");
            response.sendRedirect("/login.jsp" + query);
            return;
        }

        String apiUrl = urlManager.getPublicDocEditorUrl();
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "";
        }

        String callbackUrl = "";
        String fileUrl = "";
        String key = "";
        String fileName = "";
        String errorMessage = "";
        ApplicationUser user = null;
        String userAgent = request.getHeader("User-Agent").toLowerCase();
        String regex =
                "android|avantgo|playbook|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od|ad)|iris|kindle|lge|maemo|midp|mmp|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)|plucker|pocket|psp|symbian|treo|up.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i";
        String type = "desktop";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(userAgent);

        String attachmentIdString = request.getParameter("attachmentId");
        Long attachmentId = null;

        if (userAgent != null) {
            if (matcher.find()) {
                type = "mobile";
            }
        }

        try {
            attachmentId = Long.parseLong(attachmentIdString);
            log.info("attachmentId " + attachmentId);

            user = jiraAuthenticationContext.getLoggedInUser();
            log.info("user " + user);
            if (attachmentUtil.checkAccess(attachmentId, user, false)) {
                key = documentManager.getKeyOfFile(attachmentId);

                fileName = attachmentUtil.getFileName(attachmentId);

                fileUrl = urlManager.GetFileUri(attachmentId);

                if (attachmentUtil.checkAccess(attachmentId, user, true)) {
                    callbackUrl = urlManager.getCallbackUrl(attachmentId);
                }
            } else {
                log.info("User don't have enough permission to view the file");
                errorMessage = i18n.getText("onlyoffice.connector.error.AccessDenied");
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, error);
        }

        response.setContentType("text/html;charset=UTF-8");

        Map<String, Object> defaults =
                getTemplateConfig(attachmentId, apiUrl, callbackUrl, fileUrl, key, fileName, user, errorMessage, type);
        WebResourceAssembler webResourceAssembler =
                webResourceAssemblerFactory.create().includeSuperbatchResources(true).build();
        webResourceAssembler.resources().requireWebResource("onlyoffice.onlyoffice-jira-app:editor-page-resources");
        webResourceAssembler.assembled().drainIncludedResources()
                .writeHtmlTags(response.getWriter(), UrlModeUtils.convert(UrlMode.AUTO));
        templateRenderer.render("templates/editor.vm", defaults, response.getWriter());
    }

    private Map<String, Object> getTemplateConfig(final Long attachmentId, final String apiUrl,
                                                  final String callbackUrl, final String fileUrl, final String key,
                                                  final String fileName,
                                                  final ApplicationUser user, final String errorMessage,
                                                  final String type) throws UnsupportedEncodingException {

        Map<String, Object> defaults = new HashMap<String, Object>();
        Map<String, String> config = new HashMap<String, String>();

        String docTitle = fileName.trim();
        String docExt = attachmentUtil.getFileExt(attachmentId);
        String documentType = documentManager.getDocType(docExt);
        String errorMessageLocal = errorMessage;

        JSONObject responseJson = new JSONObject();
        JSONObject documentObject = new JSONObject();
        JSONObject editorConfigObject = new JSONObject();
        JSONObject customizationObject = new JSONObject();
        JSONObject goBackObject = new JSONObject();
        JSONObject userObject = new JSONObject();
        JSONObject permObject = new JSONObject();

        try {
            responseJson.put("type", type);
            responseJson.put("width", "100%");
            responseJson.put("height", "100%");
            if (errorMessageLocal == null || errorMessageLocal.isEmpty()) {
                if (documentType != null) {
                    responseJson.put("documentType", documentType);

                    responseJson.put("document", documentObject);
                    documentObject.put("title", docTitle);
                    documentObject.put("url", fileUrl);
                    documentObject.put("fileType", docExt);
                    documentObject.put("key", key);
                    documentObject.put("permissions", permObject);

                    responseJson.put("editorConfig", editorConfigObject);

                    editorConfigObject.put("lang", localeManager.getLocaleFor(user).toLanguageTag());
                    editorConfigObject.put("customization", customizationObject);
                    customizationObject.put("goback", goBackObject);
                    goBackObject.put("url", urlManager.getGobackUrl(attachmentId));

                    Boolean canEdit = (documentManager.isEditable(docExt) || documentManager.isFillForm(docExt))
                            && callbackUrl != null && !callbackUrl.isEmpty();

                    if (canEdit) {
                        permObject.put("edit", true);
                        editorConfigObject.put("mode", "edit");
                        editorConfigObject.put("callbackUrl", callbackUrl);
                    } else {
                        permObject.put("edit", false);
                        editorConfigObject.put("mode", "view");
                    }

                    if (user != null) {
                        editorConfigObject.put("user", userObject);
                        userObject.put("id", user.getUsername());
                        userObject.put("name", user.getDisplayName());
                    }

                    if (jwtManager.jwtEnabled()) {
                        responseJson.put("token", jwtManager.createToken(responseJson));
                    }
                } else {
                    errorMessageLocal =
                            i18n.getText("onlyoffice.connector.error.NotSupportedFormat") + " (." + docExt + ")";
                }
            }

            config.put("docserviceApiUrl", apiUrl + configurationManager.getProperty("files.docservice.url.api"));
            config.put("saveAsAsHtml", urlManager.getSaveAsObject(attachmentId, user).toString());
            config.put("attachmentId", attachmentId.toString());
            config.put("errorMessage", errorMessageLocal);
            config.put("docTitle", docTitle);
            config.put("favicon", webResourceUrlProvider.getStaticPluginResourceUrl(
                    "onlyoffice.onlyoffice-jira-app:editor-page-resources",
                    documentType + ".ico", UrlMode.ABSOLUTE));

            // AsHtml at the end disables automatic html encoding
            config.put("jsonAsHtml", responseJson.toString());
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);
        }

        defaults.putAll(config);
        defaults.put("demo", demoManager.isActive());

        return defaults;
    }
}
