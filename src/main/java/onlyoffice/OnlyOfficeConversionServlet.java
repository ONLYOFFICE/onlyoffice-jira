/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class OnlyOfficeConversionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeConversionServlet");

    @JiraImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @JiraImport
    private final JiraAuthenticationContext jiraAuthenticationContext;
    @JiraImport
    private final LocaleManager localeManager;

    private final AttachmentUtil attachmentUtil;
    private final ParsingUtil parsingUtil;
    private final UrlManager urlManager;
    private final ConversionManager conversionManager;

    @Inject
    public OnlyOfficeConversionServlet( JiraAuthenticationContext jiraAuthenticationContext, AttachmentUtil attachmentUtil,
                                        ParsingUtil parsingUtil, UrlManager urlManager, PluginSettingsFactory pluginSettingsFactory, 
                                        LocaleManager localeManager, ConversionManager conversionManager) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.attachmentUtil = attachmentUtil;
        this.parsingUtil = parsingUtil;
        this.urlManager = urlManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.localeManager = localeManager;
        this.conversionManager = conversionManager;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String ext = "";
        try {
            ext = request.getParameter("ext");
        
            String[] saveAsFormats = FileUtil.getSaveAsFormats(ext);
            JSONObject jsonSaveAsFormats = new JSONObject();
            String options = "";
            String format = "";

            for (int i = 0; i < saveAsFormats.length; i++) {
                format = saveAsFormats[i];
                options = options + "<option value='"+ format +"'>"+ format +"</option>";
            }

            jsonSaveAsFormats.put("options", options);

            response.setStatus(response.SC_OK);
            response.getWriter().write(jsonSaveAsFormats.toString());
            response.getWriter().flush();
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String error = ""; 
        try {
            ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
            String attachmentIdString = request.getParameter("attachmentId");

            Long attachmentId = Long.parseLong(attachmentIdString);
            String fileUrl = urlManager.GetFileUri(attachmentId);
            String filename = request.getParameter("filename");
            String originFormat = request.getParameter("originFormat");
            String fileExt = request.getParameter("fileExt");
            String lang = localeManager.getLocaleFor(user).toLanguageTag();
            Path tempFile = Files.createTempFile(null, null);

            PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
            String docUrl = (String) pluginSettings.get("onlyoffice.docInnerUrl");
            if (docUrl == null || docUrl.isEmpty()) { docUrl = (String) pluginSettings.get("onlyoffice.apiUrl"); }
            String jwtSecret = (String) pluginSettings.get("onlyoffice.jwtSecret");
            String convertFile = conversionManager.GetConvertedUri( docUrl, jwtSecret, fileUrl, filename, originFormat, 
                                                                    fileExt, "", true, lang, response);
            response.setStatus(response.SC_OK);
            response.getWriter().write(convertFile);
            response.getWriter().flush();
        } catch (Exception ex) {
            response.setStatus(response.SC_OK);
            response.getWriter().write(ex.toString());
            response.getWriter().flush();
            log.error(ex);
        }
    }

}