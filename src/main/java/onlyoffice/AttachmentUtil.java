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
 */

package onlyoffice;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.ofbiz.FieldMap;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.util.AttachmentException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.opensymphony.module.propertyset.PropertySet;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Named
public class AttachmentUtil {
    private final Logger log = LogManager.getLogger("onlyoffice.AttachmentUtil");
    private static final int TYPE_STRING = 5;
    @ComponentImport
    private final AttachmentManager attachmentManager;
    @ComponentImport
    private final PermissionManager permissionManager;

    private final OfBizDelegator ofBizDelegator;

    @Inject
    public AttachmentUtil(final AttachmentManager attachmentManager, final PermissionManager permissionManager) {

        this.attachmentManager = attachmentManager;
        this.permissionManager = permissionManager;

        ofBizDelegator = ComponentAccessor.getOfBizDelegator();
    }

    public boolean checkAccess(final Long attachmentId, final ApplicationUser user, final boolean forEdit) {
        return checkAccess(attachmentManager.getAttachment(attachmentId), user, forEdit);
    }

    public boolean checkAccess(final Attachment attachment, final ApplicationUser user, final boolean forEdit) {
        if (user == null) {
            return false;
        }

        Issue issue = attachment.getIssue();
        if (forEdit) {
            return permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user)
                    && permissionManager.hasPermission(ProjectPermissions.CREATE_ATTACHMENTS, issue, user);
        } else {
            return permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user);
        }
    }

    public void saveAttachment(final Long attachmentId, final File file, final ApplicationUser user)
            throws IllegalArgumentException, AttachmentException {

        Attachment oldAttachment = attachmentManager.getAttachment(attachmentId);

        String newFileName = getCorrectAttachmentName(oldAttachment.getFilename(), oldAttachment.getIssue());

        CreateAttachmentParamsBean createAttachmentParamsBean = new CreateAttachmentParamsBean.Builder(file,
                newFileName, oldAttachment.getMimetype(), user, oldAttachment.getIssue()).build();

        attachmentManager.createAttachment(createAttachmentParamsBean);
    }

    public ChangeItemBean saveAttachment(final Long attachmentId, final File file, final String ext,
                                         final ApplicationUser user)
            throws IllegalArgumentException, AttachmentException {

        Attachment oldAttachment = attachmentManager.getAttachment(attachmentId);

        String oldFileName = oldAttachment.getFilename();
        String newFileName = oldFileName.substring(0, oldFileName.lastIndexOf(".") + 1) + ext;

        newFileName = getCorrectAttachmentName(newFileName, oldAttachment.getIssue());

        CreateAttachmentParamsBean createAttachmentParamsBean = new CreateAttachmentParamsBean.Builder(file,
                newFileName, oldAttachment.getMimetype(), user, oldAttachment.getIssue()).build();

        return attachmentManager.createAttachment(createAttachmentParamsBean);
    }

    public void getAttachmentData(final DownloadFileStreamConsumer consumer, final Long attachmentId)
            throws IOException {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        attachmentManager.streamAttachmentContent(attachment, consumer);
    }

    public Long getFilesize(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getFilesize();
    }

    public String getMediaType(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getMimetype();
    }

    public String getFileName(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        return attachment.getFilename();
    }

    public String getFileExt(final Long attachmentId) {
        String fileName = getFileName(attachmentId);
        return fileName.substring(fileName.lastIndexOf(".") + 1).trim().toLowerCase();
    }

    public String getIssueKey(final Long attachmentId) {
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        Issue issue = attachment.getIssue();
        return issue.getKey();
    }

    public String getCorrectAttachmentName(final String fileName, final Issue issue) {
        Collection<Attachment> attachments = issue.getAttachments();

        String sanitizedFileName = fileName.replaceAll("[*?:\"<>/|\\\\]", "_");

        String basename = sanitizedFileName.substring(0, sanitizedFileName.lastIndexOf('.'));
        String ext = sanitizedFileName.substring(sanitizedFileName.lastIndexOf("."));

        int count = 0;
        Boolean exist = true;

        String result = sanitizedFileName;

        while (exist) {
            exist = false;
            for (Attachment attachment : attachments) {
                if (attachment.getFilename().equals(result)) {
                    count++;
                    result = basename + "-" + count + ext;
                    exist = true;
                    break;
                }
            }
        }

        return result;
    }

    public String getProperty(final Long attachmentId, final String key) {
        String property = null;
        Attachment attachment = attachmentManager.getAttachment(attachmentId);
        PropertySet properties = attachment.getProperties();
        if (properties != null && properties.exists(key)) {
            property = properties.getString(key);
        }

        return property;
    }

    public void setProperty(final Long attachmentId, final String key, final String value) {
        FieldMap fieldMap = FieldMap.build(
                "entityId", attachmentId,
                "propertyKey", key,
                "type", TYPE_STRING,
                "entityName", "FileAttachment"
        );

        List<GenericValue> properties = ofBizDelegator.findByAnd("OSPropertyEntry", fieldMap);

        GenericValue property = null;

        if (properties != null && !properties.isEmpty()) {
            property = properties.stream().max((p1, p2) -> p1.getLong("id").compareTo(p2.getLong("id"))).get();
        } else {
            property = ofBizDelegator.createValue("OSPropertyEntry", fieldMap);
        }

        GenericValue propertyValue = ofBizDelegator.makeValue("OSPropertyString",
                FieldMap.build("id", property.getLong("id"), "value", value));

        ofBizDelegator.storeAll(Collections.singletonList(propertyValue));
    }

    public void removeProperty(final Long attachmentId, final String key) {
        FieldMap fieldMap = FieldMap.build(
                "entityId", attachmentId,
                "propertyKey", key
        );

        List<GenericValue> properties = ofBizDelegator.findByAnd("OSPropertyEntry", fieldMap);

        if (properties != null && !properties.isEmpty()) {
            ofBizDelegator.removeAll(properties);
            for (GenericValue property : properties) {
                ofBizDelegator.removeById("OSPropertyString", property.getLong("id"));
            }
        }
    }
}
