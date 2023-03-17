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


package onlyoffice;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URLEncoder;

@Named
public class UrlManager {
    private final Logger log = LogManager.getLogger("onlyoffice.UrlManager");

    private final String callbackServlet = "plugins/servlet/onlyoffice/save";
    private final String APIServlet = "plugins/servlet/onlyoffice/api";

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private final PluginSettings pluginSettings;
    private final AttachmentUtil attachmentUtil;
    private final DocumentManager documentManager;
    private final DemoManager demoManager;

    @Inject
    public UrlManager(final PluginSettingsFactory pluginSettingsFactory, final AttachmentUtil attachmentUtil,
                      final DocumentManager documentManager,
                      final DemoManager demoManager) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.attachmentUtil = attachmentUtil;
        this.documentManager = documentManager;
        this.demoManager = demoManager;
    }

    public String getPublicDocEditorUrl() {
        String url = (String) pluginSettings.get("onlyoffice.apiUrl");

        if (demoManager.isActive()) {
            url = demoManager.getUrl();
        }

        return (url == null || url.isEmpty()) ? "" : url;
    }


    public String getInnerDocEditorUrl() {
        String url = (String) pluginSettings.get("onlyoffice.docInnerUrl");
        if (url == null || url.isEmpty() || demoManager.isActive()) {
            return getPublicDocEditorUrl();
        } else {
            return url;
        }
    }

    public String GetFileUri(final Long attachmentId) throws Exception {
        String hash = documentManager.CreateHash(Long.toString(attachmentId));

        String callbackUrl = getJiraBaseUrl() + callbackServlet + "?vkey=" + URLEncoder.encode(hash, "UTF-8");
        log.info("fileUrl " + callbackUrl);

        return callbackUrl;
    }

    public String getCallbackUrl(final Long attachmentId) throws Exception {
        String hash = documentManager.CreateHash(Long.toString(attachmentId));

        String callbackUrl = getJiraBaseUrl() + callbackServlet + "?vkey=" + URLEncoder.encode(hash, "UTF-8");
        log.info("callbackUrl " + callbackUrl);

        return callbackUrl;
    }

    private String getJiraBaseUrl() {
        String url = (String) pluginSettings.get("onlyoffice.confUrl");
        if (url == null || url.isEmpty()) {
            return ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/";
        } else {
            return url;
        }
    }

    public String replaceDocEditorURLToInternal(final String url) {
        String innerDocEditorUrl = getInnerDocEditorUrl();
        String publicDocEditorUrl = getPublicDocEditorUrl();
        if (!publicDocEditorUrl.equals(innerDocEditorUrl)) {
            return url.replace(publicDocEditorUrl, innerDocEditorUrl);
        }
        return url;
    }

    public String getGobackUrl(final Long attachmentId) {
        String issueKey = attachmentUtil.getIssueKey(attachmentId);

        return ComponentAccessor.getApplicationProperties().getString("jira.baseurl") + "/browse/" + issueKey;
    }

    public JSONObject getSaveAsObject(final Long attachmentId, final ApplicationUser user) throws JSONException {
        JSONObject saveAs = new JSONObject();
        saveAs.put("uri", getJiraBaseUrl() + APIServlet + "?type=save-as");
        saveAs.put("available", attachmentUtil.checkAccess(attachmentId, user, true));

        return saveAs;
    }

}
