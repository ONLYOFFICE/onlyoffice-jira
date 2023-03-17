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

import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
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
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class OnlyOfficeAPIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeAPIServlet");


    @JiraImport
    private final JiraAuthenticationContext jiraAuthenticationContext;

    private final AttachmentUtil attachmentUtil;
    private final ParsingUtil parsingUtil;
    private final UrlManager urlManager;
    private final ConfigurationManager configurationManager;

    @Inject
    public OnlyOfficeAPIServlet(final JiraAuthenticationContext jiraAuthenticationContext,
                                final AttachmentUtil attachmentUtil,
                                final ParsingUtil parsingUtil, final UrlManager urlManager,
                                final ConfigurationManager configurationManager) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.attachmentUtil = attachmentUtil;
        this.parsingUtil = parsingUtil;
        this.urlManager = urlManager;
        this.configurationManager = configurationManager;
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        String type = request.getParameter("type");
        if (type != null) {
            switch (type.toLowerCase()) {
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

    private void saveAs(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String body = parsingUtil.getBody(request.getInputStream());

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

            try (CloseableHttpClient httpClient = configurationManager.getHttpClient()) {
                HttpGet httpGet = new HttpGet(downloadUrl);

                try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                    int status = httpResponse.getStatusLine().getStatusCode();
                    HttpEntity entity = httpResponse.getEntity();

                    if (status == HttpStatus.SC_OK) {
                        byte[] bytes = IOUtils.toByteArray(entity.getContent());
                        InputStream inputStream = new ByteArrayInputStream(bytes);

                        log.info("size = " + bytes.length);
                        tempFile = Files.createTempFile(null, null);
                        FileUtils.copyInputStreamToFile(inputStream, tempFile.toFile());

                        ChangeItemBean changeItemBean =
                                attachmentUtil.saveAttachment(attachmentIdAsLong, tempFile.toFile(), fileType, user);

                        response.setContentType("application/json");
                        PrintWriter writer = response.getWriter();
                        writer.write("{\"attachmentId\":\"" + changeItemBean.getTo() + "\", \"fileName\":\"" +
                                changeItemBean.getToString() + "\"}");
                    } else {
                        throw new HttpException("Document Server returned code " + status);
                    }
                }
            }

        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            if (tempFile != null && Files.exists(tempFile)) {
                Files.delete(tempFile);
            }
        }
    }
}
