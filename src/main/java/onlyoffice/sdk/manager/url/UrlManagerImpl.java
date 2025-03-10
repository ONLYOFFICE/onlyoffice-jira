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
import com.atlassian.jira.user.ApplicationUser;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.DefaultUrlManager;
import com.onlyoffice.model.settings.SettingsConstants;
import onlyoffice.AttachmentUtil;
import onlyoffice.utils.SecurityUtils;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UrlManagerImpl extends DefaultUrlManager implements UrlManager {
    private static final String CALLBACK_SERVLET = "/plugins/servlet/onlyoffice/save";
    private static final String API_SERVLET = "/plugins/servlet/onlyoffice/api";
    private static final String TEST_SERVLET = "/plugins/servlet/onlyoffice/test";

    private final AttachmentUtil attachmentUtil;

    public UrlManagerImpl(final SettingsManager settingsManager, final AttachmentUtil attachmentUtil) {
        super(settingsManager);

        this.attachmentUtil = attachmentUtil;
    }

    @Override
    public String getFileUrl(final String fileId) {
        String hash = SecurityUtils.createHash(fileId);

        try {
            return getJiraBaseUrl(true)
                    + CALLBACK_SERVLET
                    + "?vkey=" + URLEncoder.encode(hash, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getCallbackUrl(final String fileId) {
        String hash = SecurityUtils.createHash(fileId);

        try {
            return getJiraBaseUrl(true)
                    + CALLBACK_SERVLET
                    + "?vkey=" + URLEncoder.encode(hash, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
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
    public JSONObject getSaveAsObject(final Long attachmentId, final ApplicationUser user) {
        JSONObject saveAs = new JSONObject();
        saveAs.put("uri", getJiraBaseUrl(false) + API_SERVLET + "?type=save-as");
        saveAs.put("available", attachmentUtil.checkAccess(attachmentId, user, true));

        return saveAs;
    }
}
