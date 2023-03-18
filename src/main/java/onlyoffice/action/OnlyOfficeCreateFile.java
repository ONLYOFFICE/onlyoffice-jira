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

import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import onlyoffice.AttachmentUtil;
import onlyoffice.DocumentManager;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Scanned
@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class OnlyOfficeCreateFile extends AbstractIssueSelectAction {
    private final Logger log = LogManager.getLogger("onlyoffice.action.OnlyOfficeCreateFile");

    @JiraImport
    private final JiraAuthenticationContext jiraAuthenticationContext;
    @JiraImport
    private final PluginAccessor pluginAccessor;

    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;

    private String fileType;
    private String fileName;

    @Inject
    public OnlyOfficeCreateFile(final JiraAuthenticationContext jiraAuthenticationContext,
                                final PluginAccessor pluginAccessor,
                                final DocumentManager documentManager, final AttachmentUtil attachmentUtil) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.pluginAccessor = pluginAccessor;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
    }

    @Override
    public String doDefault() throws IOException {
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        if (user == null) {
            HttpServletResponse response = this.getHttpResponse();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
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

        String fileExt = documentManager.getDefaultExtByType(fileType);

        if (fileName == null || fileName.isEmpty() || fileExt == null) {
            addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
            return;
        }
    }

    @Override
    public String doExecute() throws Exception {
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        if (user == null) {
            HttpServletResponse response = this.getHttpResponse();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }

        InputStream demoFileStream = null;
        File demoFile = null;

        try {
            String fileExt = documentManager.getDefaultExtByType(fileType);
            String correctFileName =
                    attachmentUtil.getCorrectAttachmentName(fileName + "." + fileExt, getIssueObject());
            String mimeType = documentManager.getMimeType(correctFileName);
            String pathToDemoFile = "app_data/" + getLocale().toLanguageTag();

            if (pluginAccessor.getDynamicResourceAsStream(pathToDemoFile) == null) {
                pathToDemoFile = "app_data/en-US";
            }

            demoFileStream = pluginAccessor.getDynamicResourceAsStream(pathToDemoFile + "/new." + fileExt);
            demoFile = Files.createTempFile(null, null).toFile();

            FileUtils.copyInputStreamToFile(demoFileStream, demoFile);

            CreateAttachmentParamsBean createAttachmentParamsBean = new CreateAttachmentParamsBean.Builder(demoFile,
                    correctFileName, mimeType, user, getIssueObject()).build();

            ChangeItemBean changeItemBean = attachmentManager.createAttachment(createAttachmentParamsBean);

            return returnCompleteWithInlineRedirect(
                    "/plugins/servlet/onlyoffice/doceditor?attachmentId=" + changeItemBean.getTo());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
            return INPUT;
        } finally {
            if (demoFileStream != null) {
                demoFileStream.close();
            }

            if (demoFile != null && demoFile.exists()) {
                demoFile.delete();
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
