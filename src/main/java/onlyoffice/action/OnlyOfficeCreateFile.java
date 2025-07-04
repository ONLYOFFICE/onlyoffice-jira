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

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.annotations.security.AnonymousSiteAccess;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@AnonymousSiteAccess
@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class OnlyOfficeCreateFile extends AbstractIssueSelectAction {
    private final ApplicationProperties applicationProperties;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final DocumentManager documentManager;

    private String documentType;
    private String fileName;

    public OnlyOfficeCreateFile(final ApplicationProperties applicationProperties,
                                final JiraAuthenticationContext jiraAuthenticationContext,
                                final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.applicationProperties = applicationProperties;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.documentManager = docsIntegrationSdkContext.getDocumentManager();
    }

    @Override
    public String doDefault() throws IOException {
        if (!applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS)
                || !hasIssuePermission(ProjectPermissions.BROWSE_PROJECTS, getIssueObject())
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
        if (!applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS)
                || !hasIssuePermission(ProjectPermissions.BROWSE_PROJECTS, getIssueObject())
                || !hasIssuePermission(ProjectPermissions.CREATE_ATTACHMENTS, getIssueObject())) {
            addErrorMessage(getText("onlyoffice.connector.dialog.create-file.error.permission"));
            return;
        }

        DocumentType dt = DocumentType.valueOf(documentType.toUpperCase());
        String fileExt = documentManager.getDefaultExtension(dt);

        if (fileName == null || fileName.isEmpty() || fileExt == null) {
            addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
            return;
        }
    }

    @Override
    public String doExecute() {
        return returnCompleteWithInlineRedirect(
                "/plugins/servlet/onlyoffice/doceditor?issueId=" + getId()
                + "&documentType=" + documentType
                + "&fileName=" + fileName
        );
    }

    public void setDocumentType(final String documentType) {
        this.documentType = documentType;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }
}
