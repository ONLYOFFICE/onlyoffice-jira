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
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
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

public class OnlyOfficeAPIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeAPIServlet");


    @JiraImport
    private final JiraAuthenticationContext jiraAuthenticationContext;
    @JiraImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private final AttachmentUtil attachmentUtil;
    private final ParsingUtil parsingUtil;
    private final UrlManager urlManager;
    private final SSLUtil ssl;

    @Inject
    public OnlyOfficeAPIServlet(JiraAuthenticationContext jiraAuthenticationContext, AttachmentUtil attachmentUtil,
                                ParsingUtil parsingUtil, UrlManager urlManager, PluginSettingsFactory pluginSettingsFactory, 
                                SSLUtil ssl) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.attachmentUtil = attachmentUtil;
        this.parsingUtil = parsingUtil;
        this.urlManager = urlManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.ssl = ssl;
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try {
            PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
            String isCert = (String) pluginSettings.get("onlyoffice.isCert");
            ssl.checkCert(isCert);
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);
            response.sendError(500, error);
            return;
        }

        String type = request.getParameter("type");
        if (type != null) {
            switch (type.toLowerCase())
            {
                case "save-as":
                    saveAs(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    private void saveAs (HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String body = parsingUtil.getBody(request.getInputStream());

        HttpURLConnection connection = null;
        Path tempFile = null;

        try {
            JSONObject bodyJson = new JSONObject(body);

            String downloadUrl = bodyJson.getString("url");
            String fileType = bodyJson.getString("fileType");
            String attachmentId = bodyJson.getString("attachmentId");

            if (downloadUrl.isEmpty() || fileType.isEmpty() || attachmentId.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Long attachmentIdAsLong = Long.parseLong(attachmentId);

            if (!attachmentUtil.checkAccess(attachmentIdAsLong, user, true)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            downloadUrl = urlManager.replaceDocEditorURLToInternal(downloadUrl);

            URL url = new URL(downloadUrl);

            connection = (HttpURLConnection) url.openConnection();
            int size = connection.getContentLength();
            log.info("size = " + size);

            InputStream stream = connection.getInputStream();

            tempFile = Files.createTempFile(null, null);
            FileUtils.copyInputStreamToFile(stream, tempFile.toFile());

            ChangeItemBean changeItemBean = attachmentUtil.saveAttachment(attachmentIdAsLong, tempFile.toFile(), fileType, user);

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"attachmentId\":\"" + changeItemBean.getTo() + "\", \"fileName\":\"" + changeItemBean.getToString() + "\"}");

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);

            throw new IOException(ex.getMessage(), ex);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }

            if (tempFile != null && Files.exists(tempFile)) {
                Files.delete(tempFile);
            }
        }
    }
}