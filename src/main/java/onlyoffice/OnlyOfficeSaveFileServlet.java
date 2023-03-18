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

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Scanned
public class OnlyOfficeSaveFileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeSaveFileServlet");

    private static final int STATUS_EDITING = 1;
    private static final int STATUS_MUST_SAVE = 2;
    private static final int STATUS_CORRUPTED = 3;
    private static final int STATUS_CLOSED = 4;
    private static final int STATUS_FORCE_SAVE = 6;
    private static final int STATUS_CORRUPTED_FORCE_SAVE = 7;

    private final UserManager userManager;
    private final JwtManager jwtManager;
    private final PluginSettings settings;
    private final AttachmentUtil attachmentUtil;
    private final UrlManager urlManager;
    private final ParsingUtil parsingUtil;
    private final DocumentManager documentManager;
    private final ConfigurationManager configurationManager;
    private final ConversionManager conversionManager;

    @Inject
    public OnlyOfficeSaveFileServlet(final PluginSettingsFactory pluginSettingsFactory, final JwtManager jwtManager,
                                     final AttachmentUtil attachmentUtil, final UrlManager urlManager,
                                     final ParsingUtil parsingUtil, final DocumentManager documentManager,
                                     final ConfigurationManager configurationManager,
                                     final ConversionManager conversionManager) {
        settings = pluginSettingsFactory.createGlobalSettings();
        this.jwtManager = jwtManager;
        this.attachmentUtil = attachmentUtil;
        this.urlManager = urlManager;
        this.parsingUtil = parsingUtil;
        this.documentManager = documentManager;
        this.configurationManager = configurationManager;
        this.conversionManager = conversionManager;

        userManager = ComponentAccessor.getUserManager();
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (jwtManager.jwtEnabled()) {
            String jwth = jwtManager.getJwtHeader();
            String header = (String) request.getHeader(jwth);
            String authorizationPrefix = "Bearer ";
            String token = (header != null && header.startsWith(authorizationPrefix))
                    ? header.substring(authorizationPrefix.length()) : header;

            if (token == null || token == "") {
                throw new SecurityException("Expected JWT");
            }

            if (!jwtManager.verify(token)) {
                throw new SecurityException("JWT verification failed");
            }
        }

        String vkey = request.getParameter("vkey");
        log.info("vkey = " + vkey);
        String attachmentIdString = documentManager.readHash(vkey);

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
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain; charset=utf-8");

        String vkey = request.getParameter("vkey");
        log.info("vkey = " + vkey);
        String attachmentIdString = documentManager.readHash(vkey);

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
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.write("{\"error\":1,\"message\":\"" + error + "\"}");
        }

        log.info("error = " + error);
    }

    private void processData(final String attachmentIdString, final HttpServletRequest request) throws Exception {
        log.info("attachmentId = " + attachmentIdString);
        InputStream requestStream = request.getInputStream();
        if (attachmentIdString.isEmpty()) {
            throw new IllegalArgumentException("attachmentId is empty");
        }

        Path tempFile = null;

        try {
            Long attachmentId = Long.parseLong(attachmentIdString);

            String body = parsingUtil.getBody(requestStream);
            log.info("body = " + body);
            if (body.isEmpty()) {
                throw new IllegalArgumentException("requestBody is empty");
            }

            JSONObject jsonObj = new JSONObject(body);

            if (jwtManager.jwtEnabled()) {
                String token = jsonObj.optString("token");
                Boolean inBody = true;

                if (token == null || token == "") {
                    String jwth = jwtManager.getJwtHeader();
                    String header = (String) request.getHeader(jwth);
                    String authorizationPrefix = "Bearer ";
                    token = (header != null && header.startsWith(authorizationPrefix))
                            ? header.substring(authorizationPrefix.length()) : header;
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
            if (status == STATUS_MUST_SAVE || status == STATUS_CORRUPTED) {
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
                downloadUrl = urlManager.replaceDocEditorURLToInternal(downloadUrl);
                log.info("downloadUri = " + downloadUrl);

                String attachmentExt = attachmentUtil.getFileExt(attachmentId);
                String extDownloadUrl = jsonObj.getString("filetype");

                if (!extDownloadUrl.equals(attachmentExt)) {
                    JSONObject response = conversionManager.convert(attachmentId, downloadUrl, attachmentExt, user);
                    downloadUrl = response.getString("fileUrl");
                }

                try (CloseableHttpClient httpClient = configurationManager.getHttpClient()) {
                    HttpGet httpGet = new HttpGet(downloadUrl);

                    try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        HttpEntity entity = response.getEntity();

                        if (statusCode == HttpStatus.SC_OK) {
                            byte[] bytes = IOUtils.toByteArray(entity.getContent());
                            InputStream inputStream = new ByteArrayInputStream(bytes);

                            tempFile = Files.createTempFile(null, null);
                            FileUtils.copyInputStreamToFile(inputStream, tempFile.toFile());

                            attachmentUtil.saveAttachment(attachmentId, tempFile.toFile(), user);
                            attachmentUtil.removeProperty(attachmentId, "onlyoffice-collaborative-editor-key");
                        } else {
                            throw new HttpException("Document Server returned code " + status);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);

            throw ex;
        } finally {
            if (tempFile != null && Files.exists(tempFile)) {
                Files.delete(tempFile);
            }
        }
    }
}
