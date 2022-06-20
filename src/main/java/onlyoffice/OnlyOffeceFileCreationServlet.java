/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.PluginAccessor;

@Scanned
public class OnlyOffeceFileCreationServlet extends HttpServlet {

    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOffeceFileCreationServlet");
    private static final long serialVersionUID = 1L;

    @ComponentImport
    private final AttachmentManager attachmentManager;
    @ComponentImport
    private final IssueManager issueManager;
    @ComponentImport
    private final PermissionManager permissionManager;

    @JiraImport
    private final JiraAuthenticationContext jiraAuthenticationContext;
    @JiraImport
    private final UserManager userManager;
    @JiraImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @JiraImport
    private final TemplateRenderer templateRenderer;
    @JiraImport
    private final LocaleManager localeManager;
    @JiraImport
    private final PluginAccessor pluginAccessor;

    @Inject
    public OnlyOffeceFileCreationServlet(UserManager userManager, PluginSettingsFactory pluginSettingsFactory,
            TemplateRenderer templateRenderer, LocaleManager localeManager, AttachmentManager attachmentManager, 
            IssueManager issueManager, JiraAuthenticationContext jiraAuthenticationContext,
            PluginAccessor pluginAccessor, PermissionManager permissionManager) {
                
        this.userManager = userManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.templateRenderer = templateRenderer;
        this.localeManager = localeManager;
        this.attachmentManager = attachmentManager;
        this.issueManager = issueManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.pluginAccessor = pluginAccessor;
        this.permissionManager = permissionManager;

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        if (user == null) {
            String currentURL= request.getRequestURI() + "?" + request.getQueryString();
            String query ="?permissionViolation=true&os_destination=" + URLEncoder.encode(currentURL, "UTF-8");
            response.sendRedirect ("/login.jsp" + query);
            return;
        }
        String issueString = request.getParameter("issueId");
        String ext = request.getParameter("fileExt");
        Long issueId = Long.parseLong(issueString);

        File file = null;
        String filename = request.getParameter("filename") + "." + ext;
        String contentType = FileUtil.getMimeType(filename);
        String locale = localeManager.getLocaleFor(user).toLanguageTag();

        Issue issue = issueManager.getIssueObject(issueId);
        if (!checkPermission(issue, user, true)) {
            response.setStatus(response.SC_FORBIDDEN);
            return;
        }

        String path = "/document-templates/" + locale + "/new." + ext;

        try {
            file = new File(".", "new." + ext);

            InputStream inputStream = pluginAccessor.getDynamicResourceAsStream(path);
            FileUtils.copyInputStreamToFile(inputStream, file);

            filename = FileUtil.getCorrectName(filename, issue.getAttachments());

            AttachmentUtil.createFileToAttachment(file, filename, contentType, user, issue, attachmentManager);

        } catch (Exception ex) {
            log.error(ex);
        } finally {
            file.delete();
        }

        return;
    }

    public boolean checkPermission(Issue issue, ApplicationUser user, boolean forEdit) {
        if (forEdit) {
            return permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user)
                && permissionManager.hasPermission(ProjectPermissions.CREATE_ATTACHMENTS, issue, user);
        } else {
            return permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user);
        }
    }
}
