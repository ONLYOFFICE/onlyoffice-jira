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

import com.atlassian.annotations.security.AnonymousSiteAccess;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.document.DocumentManager;
import onlyoffice.utils.ParsingUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

@AnonymousSiteAccess
public class OnlyOfficeAPIServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeAPIServlet");

    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final AttachmentUtil attachmentUtil;
    private final DocumentServerClient documentServerClient;
    private final DocumentManager documentManager;

    public OnlyOfficeAPIServlet(final JiraAuthenticationContext jiraAuthenticationContext,
                                final AttachmentUtil attachmentUtil,
                                final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.attachmentUtil = attachmentUtil;

        this.documentServerClient = docsIntegrationSdkContext.getDocumentServerClient();
        this.documentManager = docsIntegrationSdkContext.getDocumentManager();
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
        String body = ParsingUtils.getBody(request.getInputStream());
        JSONObject bodyJson = new JSONObject(body);

        String downloadUrl = bodyJson.getString("url");
        String fileType = bodyJson.getString("fileType");
        String attachmentIdString = bodyJson.getString("attachmentId");

        Long attachmentId = Long.parseLong(attachmentIdString);

        if (downloadUrl.isEmpty() || fileType.isEmpty() || attachmentIdString.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
        Attachment attachment = attachmentUtil.getAttachment(attachmentId);

        if (attachment == null) {
            if (jiraAuthenticationContext.isLoggedInUser()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
            return;
        }

        if (!attachmentUtil.checkAccess(attachmentId, user, true)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String fileName = documentManager.getDocumentName(attachmentIdString);
        String baseFileName = documentManager.getBaseName(fileName);
        String newFileName = attachmentUtil.getNewAttachmentFileName(
                baseFileName + "." + fileType,
                attachment.getIssue()
        );

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(null, null);

            documentServerClient.getFile(
                    downloadUrl,
                    Files.newOutputStream(tempFile)
            );

            ChangeItemBean changeItemBean = attachmentUtil.createNewAttachment(
                    newFileName,
                    tempFile.toFile(),
                    attachment.getIssue()
            );

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"attachmentId\":\"" + changeItemBean.getTo() + "\", \"fileName\":\""
                    + changeItemBean.getToString() + "\"}");
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
