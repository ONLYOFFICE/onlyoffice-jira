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

package onlyoffice.sdk.service.config;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.common.User;
import com.onlyoffice.model.documenteditor.config.document.Permissions;
import com.onlyoffice.service.documenteditor.config.DefaultConfigService;
import onlyoffice.utils.AttachmentUtil;

public class ConfigServiceImpl extends DefaultConfigService {
    private final AttachmentUtil attachmentUtil;

    public ConfigServiceImpl(final DocumentManager documentManager, final UrlManager urlManager,
                             final JwtManager jwtManager, final SettingsManager settingsManager,
                             final AttachmentUtil attachmentUtil) {
        super(documentManager, urlManager, jwtManager, settingsManager);

        this.attachmentUtil = attachmentUtil;
    }

    @Override
    public Permissions getPermissions(final String fileId) {
        JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        String fileName = getDocumentManager().getDocumentName(fileId);

        Boolean editPermission = attachmentUtil.checkAccess(Long.parseLong(fileId), user, true);
        Boolean isEditable = super.getDocumentManager().isEditable(fileName);

        return Permissions.builder()
                .edit(editPermission && isEditable)
                .protect(user != null)
                .chat(user != null)
                .build();
    }

    @Override
    public User getUser() {
        JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        if (user != null) {
            return User.builder()
                    .id(user.getKey())
                    .name(user.getDisplayName())
                    .build();
        } else {
            return super.getUser();
        }
    }
}
