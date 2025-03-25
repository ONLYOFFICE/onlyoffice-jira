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

package onlyoffice.action;

import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import onlyoffice.utils.AttachmentUtil;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class OnlyOfficeCreateFile extends AbstractIssueSelectAction {
    private final Logger log = LogManager.getLogger(this.getClass());

    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final AttachmentUtil attachmentUtil;
    private final DocumentManager documentManager;

    private String fileType;
    private String fileName;

    public OnlyOfficeCreateFile(final JiraAuthenticationContext jiraAuthenticationContext,
                                final AttachmentUtil attachmentUtil,
                                final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.attachmentUtil = attachmentUtil;
        this.documentManager = docsIntegrationSdkContext.getDocumentManager();
    }

    @Override
    public String doDefault() throws IOException {
        if (!hasIssuePermission(ProjectPermissions.BROWSE_PROJECTS, getIssueObject())
                || !hasIssuePermission(ProjectPermissions.CREATE_ATTACHMENTS, getIssueObject())) {
            HttpServletResponse response = this.getHttpResponse();
            if (jiraAuthenticationContext.isLoggedInUser()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }

        return INPUT;
    }

    @Override
    protected void doValidation() {
        if (!hasIssuePermission(ProjectPermissions.BROWSE_PROJECTS, getIssueObject())
                || !hasIssuePermission(ProjectPermissions.CREATE_ATTACHMENTS, getIssueObject())) {
            addErrorMessage(getText("onlyoffice.connector.dialog.create-file.error.permission"));
            return;
        }

        DocumentType documentType = DocumentType.valueOf(fileType.toUpperCase());
        String fileExt = documentManager.getDefaultExtension(documentType);

        if (fileName == null || fileName.isEmpty() || fileExt == null) {
            addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
            return;
        }
    }

    @Override
    public String doExecute() throws Exception {
        DocumentType documentType = DocumentType.valueOf(fileType.toUpperCase());
        String extension = documentManager.getDefaultExtension(documentType);
        String newFileName = attachmentUtil.getNewAttachmentFileName(
                fileName + "." + extension,
                getIssueObject()
        );

        File newBlankFile = null;
        try (InputStream newBlankFileInputStream = documentManager.getNewBlankFile(extension, getLocale())) {
            newBlankFile = Files.createTempFile(null, null).toFile();

            FileUtils.copyInputStreamToFile(newBlankFileInputStream, newBlankFile);

            ChangeItemBean changeItemBean = attachmentUtil.createNewAttachment(
                    newFileName,
                    newBlankFile,
                    getIssueObject()
            );

            return returnCompleteWithInlineRedirect(
                    "/plugins/servlet/onlyoffice/doceditor?attachmentId=" + changeItemBean.getTo());
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
            return INPUT;
        } finally {
            if (newBlankFile != null) {
                Files.deleteIfExists(newBlankFile.toPath());
            }
        }
    }

    public void setFileType(final String fileType) {
        this.fileType = fileType;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }
}
