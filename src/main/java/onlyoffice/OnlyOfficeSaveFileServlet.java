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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONArray;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;

import javax.inject.Inject;

@Scanned
public class OnlyOfficeSaveFileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeSaveFileServlet");

    @JiraImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @JiraImport
    private final JiraAuthenticationContext jiraAuthenticationContext;
    @JiraImport
    private final TemplateRenderer templateRenderer;

    private final UserManager userManager;
    private final JwtManager jwtManager;
    private final PluginSettings settings;
    private final AttachmentUtil attachmentUtil;

    @Inject
    public OnlyOfficeSaveFileServlet(PluginSettingsFactory pluginSettingsFactory,
    JiraAuthenticationContext jiraAuthenticationContext,
    JwtManager jwtManager, AttachmentUtil attachmentUtil, TemplateRenderer templateRenderer) {
        
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.jiraAuthenticationContext = jiraAuthenticationContext;

        settings = pluginSettingsFactory.createGlobalSettings();
        this.jwtManager = jwtManager;
        this.attachmentUtil = attachmentUtil;
        this.templateRenderer = templateRenderer;
        
        userManager = ComponentAccessor.getUserManager();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String vkey = request.getParameter("vkey");
        log.info("vkey = " + vkey);
        String attachmentIdString = DocumentManager.ReadHash(vkey);

        Long attachmentId = Long.parseLong(attachmentIdString);
        log.info("attachmentId " + attachmentId);

        String contentType = attachmentUtil.getMediaType(attachmentId);
        response.setContentType(contentType);

        response.setContentLength(attachmentUtil.getFilesize(attachmentId).intValue());

        OutputStream output = response.getOutputStream();
        attachmentUtil.getAttachmentData(new DownloadFileStreamConsumer(output), attachmentId);
        output.close();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain; charset=utf-8");

        String vkey = request.getParameter("vkey");
        log.info("vkey = " + vkey);
        String attachmentIdString = DocumentManager.ReadHash(vkey);

        String error = "";
        try {
            processData(attachmentIdString, request);
        } catch (Exception e) {
            error = e.getMessage();
        }

        PrintWriter writer = response.getWriter();
        if (error.isEmpty()) {
            writer.write("{\"error\":0}");
        } else {
            response.setStatus(500);
            writer.write("{\"error\":1,\"message\":\"" + error + "\"}");
        }

        log.info("error = " + error);
    }

    private void processData(String attachmentIdString, HttpServletRequest request) throws Exception {
        log.info("attachmentId = " + attachmentIdString);
        InputStream requestStream = request.getInputStream();
        if (attachmentIdString.isEmpty()) {
            throw new IllegalArgumentException("attachmentId is empty");
        }

        HttpURLConnection connection = null;
        try {
            Long attachmentId = Long.parseLong(attachmentIdString);

            String body = getBody(requestStream);
            log.info("body = " + body);
            if (body.isEmpty()) {
                throw new IllegalArgumentException("requestBody is empty");
            }

            JSONObject jsonObj = new JSONObject(body);

            if (jwtManager.jwtEnabled()) {
                String token = jsonObj.optString("token");
                Boolean inBody = true;

                if (token == null || token == "") {
                    String jwth = (String) settings.get("onlyoffice.jwtHeader");
                    String header = (String) request.getHeader(jwth == null || jwth.isEmpty() ? "Authorization" : jwth);
                    token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : header;
                    inBody = false;
                }

                if (token == null || token == "") {
                    throw new SecurityException("Try save without JWT");
                }

                if (!jwtManager.verify(token)) {
                    throw new SecurityException("Try save with wrong JWT");
                }

                JSONObject bodyFromToken = new JSONObject(
                        new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"));

                if (inBody) {
                    jsonObj = bodyFromToken;
                } else {
                    jsonObj = bodyFromToken.getJSONObject("payload");
                }
            }

            long status = jsonObj.getLong("status");
            log.info("status = " + status);

            // MustSave, Corrupted
            if (status == 2 || status == 3) {
                ApplicationUser user = null;
                JSONArray users = jsonObj.getJSONArray("users");
                if (users.length() > 0) {
                    String userName = users.getString(0);

                    user = userManager.getUserByName(userName);
                    log.info("user = " + user);
                }

                if (user == null || !attachmentUtil.checkAccess(attachmentId, user, true)) {
                    throw new SecurityException("Try save without access: " + user);
                }

                String downloadUrl = jsonObj.getString("url");
                log.info("downloadUri = " + downloadUrl);

                URL url = new URL(downloadUrl);

                connection = (HttpURLConnection) url.openConnection();
                int size = connection.getContentLength();
                log.info("size = " + size);

                InputStream stream = connection.getInputStream();

                Path tempFile = Files.createTempFile(null, null);
                FileUtils.copyInputStreamToFile(stream, tempFile.toFile());

                attachmentUtil.saveAttachment(attachmentId, tempFile.toFile(), size, user);

                Files.delete(tempFile);
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);

            throw ex;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getBody(InputStream stream) {
        try(Scanner scanner = new Scanner(stream)) {
            try(Scanner scannerUseDelimiter = scanner.useDelimiter("\\A")) {
                return scanner.hasNext() ? scanner.next() : "";
            }
        }
    }
}
