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

package onlyoffice.action;

import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.atlassian.jira.security.request.SupportedMethods;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.service.convert.ConvertService;
import onlyoffice.AttachmentUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import webwork.action.ServletActionContext;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class OnlyOfficeConversion extends AbstractIssueSelectAction {
    private final Logger log = LogManager.getLogger("onlyoffice.action.OnlyOfficeConversion");

    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final AttachmentUtil attachmentUtil;

    private final DocumentManager documentManager;
    private final DocumentServerClient documentServerClient;
    private final ConvertService convertService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String attachmentId;
    private String fileName;
    private String targetFileType;
    private String actionType;

    public OnlyOfficeConversion(final JiraAuthenticationContext jiraAuthenticationContext,
                                final AttachmentUtil attachmentUtil,
                                final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.attachmentUtil = attachmentUtil;

        this.documentManager = docsIntegrationSdkContext.getDocumentManager();
        this.documentServerClient = docsIntegrationSdkContext.getDocumentServerClient();
        this.convertService =  docsIntegrationSdkContext.getConvertService();
    }

    @Override
    public String doDefault() throws IOException {
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
        Long attachmentId = Long.parseLong(this.attachmentId);
        Attachment attachment = attachmentUtil.getAttachment(attachmentId);

        if (attachment == null || !attachmentUtil.checkAccess(attachmentId, user, false)) {
            HttpServletResponse response = this.getHttpResponse();
            if (jiraAuthenticationContext.isLoggedInUser()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        return INPUT;
    }

    @Override
    protected void doValidation() {
        HttpServletResponse response = this.getHttpResponse();

        Long attachmentId = Long.parseLong(this.attachmentId);
        String fileName = documentManager.getDocumentName(this.attachmentId);
        String defaultConvertExtension = documentManager.getDefaultConvertExtension(fileName);
        List<String> convertExtensionList = documentManager.getConvertExtensionList(fileName);

        if (!attachmentUtil.checkAccess(attachmentId, getLoggedInUser(), false)) {
            addErrorMessage(getText("onlyoffice.connector.dialog.conversion.message.error.permission"));
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (fileName == null || fileName.isEmpty()) {
            addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        if (actionType.equals("conversion")) {
            if (!attachmentUtil.checkAccess(attachmentId, getLoggedInUser(), true)) {
                addErrorMessage(getText("onlyoffice.connector.dialog.conversion.message.error.permission"));
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (defaultConvertExtension == null) {
                addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }
        }

        if (actionType.equals("download-as")) {
            if (targetFileType == null || targetFileType.isEmpty()) {
                addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (convertExtensionList == null
                    || convertExtensionList.isEmpty()
                    || !convertExtensionList.contains(targetFileType)) {
                addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }
        }
    }

    @Override
    public String doExecute() throws Exception {
        String fileName = documentManager.getDocumentName(this.attachmentId);
        String convertToExt = documentManager.getDefaultConvertExtension(fileName);
        String region = getLocale().toLanguageTag();
        Map<String, Object> responseBody = new HashMap<>();


        if (actionType.equals("download-as") && !this.targetFileType.isEmpty()) {
            convertToExt = this.targetFileType;
        }

        ConvertRequest convertRequest = ConvertRequest.builder()
                .async(true)
                .outputtype(convertToExt)
                .region(region)
                .build();

        ConvertResponse convertResponse = convertService.processConvert(convertRequest, attachmentId);

        if (actionType.equals("conversion")
                && convertResponse.getEndConvert() != null
                && convertResponse.getEndConvert()) {
            String fileUrl = convertResponse.getFileUrl();
            String newFileName = attachmentUtil.getNewAttachmentFileName(
                    this.fileName + "." + convertToExt,
                    getIssueObject()
            );

            Path tempFile = null;
            try {
                tempFile = Files.createTempFile(null, null);

                documentServerClient.getFile(fileUrl, Files.newOutputStream(tempFile));

                ChangeItemBean changeItemBean = attachmentUtil.createNewAttachment(
                        newFileName,
                        tempFile.toFile(),
                        getIssueObject()
                );

                Map<String, String> createdFile = new HashMap<>();
                createdFile.put(
                        "fileUrl",
                        "/plugins/servlet/onlyoffice/doceditor?attachmentId=" + changeItemBean.getTo()
                );
                createdFile.put("fileName", newFileName);

                responseBody.put("createdFile", createdFile);
            } finally {
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile);
                }
            }
        }

        responseBody.put("convertResponse", convertResponse);

        HttpServletResponse response = ServletActionContext.getResponse();
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(responseBody));
        response.setStatus(HttpServletResponse.SC_OK);
        return "none";
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public void setAttachmentId(final String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public void setTargetFileType(final String targetFileType) {
        this.targetFileType = targetFileType;
    }

    public void setActionType(final String actionType) {
        this.actionType = actionType;
    }

    public String getFileName() {
        String fileName = documentManager.getDocumentName(this.attachmentId);

        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    public String getFileType() {
        String fileName = documentManager.getDocumentName(this.attachmentId);

        return documentManager.getExtension(fileName);
    }

    public String getTargetFileType() {
        String fileName = documentManager.getDocumentName(this.attachmentId);

        return documentManager.getDefaultConvertExtension(fileName);
    }

    public List<String> getTargetFileTypeList() {
        String fileName = documentManager.getDocumentName(this.attachmentId);

        return documentManager.getConvertExtensionList(fileName);
    }

    public boolean isConvertibleToDefault() {
        String fileName = documentManager.getDocumentName(this.attachmentId);

        String targetExt = documentManager.getDefaultConvertExtension(fileName);

        return targetExt != null && attachmentUtil.checkAccess(
                Long.parseLong(this.attachmentId),
                getLoggedInUser(),
                true
        );
    }

}
