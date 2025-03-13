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

package onlyoffice.sdk.manager.url;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceUrlProvider;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.DefaultUrlManager;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import com.onlyoffice.model.settings.SettingsConstants;
import onlyoffice.AttachmentUtil;
import onlyoffice.sdk.manager.security.JwtManager;

import java.util.HashMap;
import java.util.Map;

public class UrlManagerImpl extends DefaultUrlManager implements UrlManager {
    private static final String CALLBACK_SERVLET = "/plugins/servlet/onlyoffice/save";
    private static final String API_SERVLET = "/plugins/servlet/onlyoffice/api";
    private static final String TEST_SERVLET = "/plugins/servlet/onlyoffice/test";

    private final WebResourceUrlProvider webResourceUrlProvider;
    private final AttachmentUtil attachmentUtil;
    private final JwtManager jwtManager;

    public UrlManagerImpl(final SettingsManager settingsManager, final WebResourceUrlProvider webResourceUrlProvider,
                          final AttachmentUtil attachmentUtil, final JwtManager jwtManager) {
        super(settingsManager);

        this.webResourceUrlProvider = webResourceUrlProvider;
        this.attachmentUtil = attachmentUtil;
        this.jwtManager = jwtManager;
    }

    @Override
    public String getFileUrl(final String fileId) {
        JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        Map<String, String> params = new HashMap<>();

        if (user != null) {
            params.put("userKey", user.getKey());
        }
        params.put("attachmentId", fileId);
        params.put("action", "download");

        return getJiraBaseUrl(true)
                + CALLBACK_SERVLET
                + "?token=" + jwtManager.createInternalToken(params);
    }

    @Override
    public String getCallbackUrl(final String fileId) {
        JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        Map<String, String> params = new HashMap<>();
        if (user != null) {
            params.put("userKey", user.getKey());
        }
        params.put("attachmentId", fileId);
        params.put("action", "download");

        return getJiraBaseUrl(true)
                + CALLBACK_SERVLET
                + "?token=" + jwtManager.createInternalToken(params);
    }

    @Override
    public String getGobackUrl(final String fileId) {
        String issueKey = attachmentUtil.getIssueKey(Long.parseLong(fileId));

        return getJiraBaseUrl(false) + "/browse/" + issueKey;
    }

    @Override
    public String getTestConvertUrl(final String url) {
        return getJiraBaseUrl(true) + TEST_SERVLET;
    }

    private String getJiraBaseUrl(final Boolean inner) {
        String productInnerUrl = getSettingsManager().getSetting(SettingsConstants.PRODUCT_INNER_URL);

        if (inner && productInnerUrl != null && !productInnerUrl.isEmpty()) {
            return sanitizeUrl(productInnerUrl);
        } else {
            return sanitizeUrl(ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
        }
    }

    @Override
    public String getFaviconUrl(final DocumentType documentType) {
        return  webResourceUrlProvider.getStaticPluginResourceUrl(
                "onlyoffice.onlyoffice-jira-app:editor-page-resources",
                documentType + ".ico", UrlMode.ABSOLUTE);
    }

    @Override
    public String getSaveAsUrl(final Long attachmentId) {
        return getJiraBaseUrl(false) + API_SERVLET + "?type=save-as";
    }
}
