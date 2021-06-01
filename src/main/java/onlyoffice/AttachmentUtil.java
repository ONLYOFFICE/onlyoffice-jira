/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.util.AttachmentException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

@Named
public class AttachmentUtil {
    private static final Logger log = LogManager.getLogger("onlyoffice.AttachmentUtil");

    @ComponentImport
    private final AttachmentManager attachmentManager;
    @ComponentImport
    private final PermissionManager permissionManager;
    @ComponentImport
    private final IssueManager issueManager;
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private final PluginSettings pluginSettings;

    @Inject
    public AttachmentUtil(AttachmentManager attachmentManager,
        PermissionManager permissionManager, IssueManager issueManager, PluginSettingsFactory pluginSettingsFactory) {

        this.attachmentManager = attachmentManager;
        this.permissionManager = permissionManager;
        this.issueManager = issueManager;

        this.pluginSettingsFactory = pluginSettingsFactory;
        pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }

    public boolean checkAccess(Long attachmentId, ApplicationUser user, boolean forEdit) {
        return checkAccess(attachmentManager.getAttachment(attachmentId), user, forEdit);
    }

    public boolean checkAccess(Attachment attachment, ApplicationUser user, boolean forEdit) {
        if (user == null) {
            return false;
        }

        Issue issue = attachment.getIssue();
        if (forEdit) {
            return permissionManager.hasPermission(ProjectPermissions.EDIT_ISSUES, issue, user)
                && permissionManager.hasPermission(ProjectPermissions.CREATE_ATTACHMENTS, issue, user);
        } else {
            return permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user);
        }
    }

    public void saveAttachment(Long attachmentId, File file, int size, ApplicationUser user)
            throws IOException, IllegalArgumentException, RemoveException, AttachmentException {

        Attachment oldAttachment = attachmentManager.getAttachment(attachmentId);

        String newFileName = getCorrectAttachmentName(oldAttachment.getFilename(), oldAttachment.getIssue());

        CreateAttachmentParamsBean createAttachmentParamsBean = new CreateAttachmentParamsBean.Builder(file,
                newFileName, oldAttachment.getMimetype(), user, oldAttachment.getIssue()).build();

        attachmentManager.createAttachment(createAttachmentParamsBean);
    }

    public void getAttachmentData(DownloadFileStreamConsumer consumer, Long attachmentId) throws IOException {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        attachmentManager.streamAttachmentContent(attachment, consumer);
    }

    public Long getFilesize(Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getFilesize();
    }

    public String getMediaType(Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getMimetype();
    }

    public String getFileName(Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getFilename();
    }

    public String getHashCode(Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        int hashCode = attachment.hashCode();
        log.info("hashCode = " + hashCode);

        Timestamp ts = attachment.getCreated();
        return attachmentId + "_" + ts.toInstant().getEpochSecond() + "_" + hashCode;
    }

    public String getCorrectAttachmentName (String fileName, Issue issue) {
        Collection<Attachment> attachments = issue.getAttachments();

        String basename = fileName.substring(0, fileName.lastIndexOf('.'));
        String ext = fileName.substring(fileName.lastIndexOf("."));

        int count = 0;
        Boolean exist = true;

        while (exist) {
            exist = false;
            for (Attachment attachment : attachments) {
                if (attachment.getFilename().equals(fileName)) {
                    count++;
                    fileName = basename + "-" + count + ext;
                    exist = true;
                    break;
                }
            }
        }

        return fileName;
    }
}