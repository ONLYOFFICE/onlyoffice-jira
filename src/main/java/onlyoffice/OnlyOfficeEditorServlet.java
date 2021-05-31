/**
 *
 * (c) Copyright Ascensio System SIA 2020
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;

import javax.inject.Inject;

@Scanned
public class OnlyOfficeEditorServlet extends HttpServlet {
    @JiraImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @JiraImport
    private final JiraAuthenticationContext jiraAuthenticationContext;
    @JiraImport
    private final TemplateRenderer templateRenderer;
    @JiraImport
    private final LocaleManager localeManager;
    
    private final JwtManager jwtManager;
    private final UrlManager urlManager;
    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;

    @Inject
    public OnlyOfficeEditorServlet(PluginSettingsFactory pluginSettingsFactory, JiraAuthenticationContext jiraAuthenticationContext,
        UrlManager urlManager, JwtManager jwtManager, DocumentManager documentManager,
        AttachmentUtil attachmentUtil, TemplateRenderer templateRenderer, LocaleManager localeManager) {

        this.pluginSettingsFactory = pluginSettingsFactory;
        this.jiraAuthenticationContext = jiraAuthenticationContext;

        this.jwtManager = jwtManager;
        this.urlManager = urlManager;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.templateRenderer = templateRenderer;
        this.localeManager = localeManager;
    }

    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeEditorServlet");
    private static final long serialVersionUID = 1L;

    private Properties properties;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!jiraAuthenticationContext.isLoggedInUser()) {
            return;
        }

        String apiUrl = urlManager.getPublicDocEditorUrl();
        if (apiUrl == null || apiUrl.isEmpty()) {
            apiUrl = "";
        }

        ConfigurationManager configurationManager = new ConfigurationManager();
        properties = configurationManager.GetProperties();

        String callbackUrl = "";
        String fileUrl = "";
        String key = "";
        String fileName = "";
        String errorMessage = "";
        ApplicationUser user = null;

        String attachmentIdString = request.getParameter("attachmentId");
        Long attachmentId;

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
                log.error("access deny");
                errorMessage = "You don not have enough permission to view the file";
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);
            errorMessage = ex.toString();
        }

        response.setContentType("text/html;charset=UTF-8");

        Map<String, Object> defaults = getTemplateConfig(apiUrl, callbackUrl, fileUrl, key, fileName, user, errorMessage);
        templateRenderer.render("templates/editor.vm", defaults, response.getWriter());
    }

    private Map<String, Object> getTemplateConfig(String apiUrl, String callbackUrl, String fileUrl, String key, String fileName,
            ApplicationUser user, String errorMessage) throws UnsupportedEncodingException {

        Map<String, Object> defaults = new HashMap<String, Object>();
        Map<String, String> config = new HashMap<String, String>();

        String docTitle = fileName.trim();
        String docExt = docTitle.substring(docTitle.lastIndexOf(".") + 1).trim().toLowerCase();

        config.put("docserviceApiUrl", apiUrl + properties.getProperty("files.docservice.url.api"));
        config.put("errorMessage", errorMessage);
        config.put("docTitle", docTitle);

        JSONObject responseJson = new JSONObject();
        JSONObject documentObject = new JSONObject();
        JSONObject editorConfigObject = new JSONObject();
        JSONObject userObject = new JSONObject();
        JSONObject permObject = new JSONObject();

        try {
            responseJson.put("type", "desktop");
            responseJson.put("width", "100%");
            responseJson.put("height", "100%");
            responseJson.put("documentType", documentManager.getDocType(docExt));

            responseJson.put("document", documentObject);
            documentObject.put("title", docTitle);
            documentObject.put("url", fileUrl);
            documentObject.put("fileType", docExt);
            documentObject.put("key", key);
            documentObject.put("permissions", permObject);
            permObject.put("edit", callbackUrl != null && !callbackUrl.isEmpty());

            responseJson.put("editorConfig", editorConfigObject);
            
            editorConfigObject.put("lang", localeManager.getLocaleFor(user).toLanguageTag());
            editorConfigObject.put("mode", "edit");
            editorConfigObject.put("callbackUrl", callbackUrl);

            if (user != null) {
                editorConfigObject.put("user", userObject);
                userObject.put("id", user.getUsername());
                userObject.put("name", user.getDisplayName());
            }

            if (jwtManager.jwtEnabled()) {
                responseJson.put("token", jwtManager.createToken(responseJson));
            }

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

        return defaults;
    }
}
