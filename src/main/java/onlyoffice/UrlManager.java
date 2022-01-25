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

import com.atlassian.jira.user.ApplicationUser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class UrlManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.UrlManager");

    private static final String callbackServler = "plugins/servlet/onlyoffice/save";
    private final String APIServlet = "plugins/servlet/onlyoffice/api";

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private final PluginSettings pluginSettings;
    private final AttachmentUtil attachmentUtil;

    @Inject
    public UrlManager(PluginSettingsFactory pluginSettingsFactory, AttachmentUtil attachmentUtil) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.attachmentUtil = attachmentUtil;
    }

    public String getPublicDocEditorUrl() {
        String url = (String) pluginSettings.get("onlyoffice.apiUrl");
        return (url == null || url.isEmpty()) ? "" : url;
    }


    public String getInnerDocEditorUrl() {
        String url = (String) pluginSettings.get("onlyoffice.docInnerUrl");
        if (url == null || url.isEmpty()) {
            return getPublicDocEditorUrl();
        } else {
            return url;
        }
    }

    public String GetFileUri(Long attachmentId) throws Exception {
        String hash = DocumentManager.CreateHash(Long.toString(attachmentId));

        String callbackUrl = getJiraBaseUrl() + callbackServler + "?vkey=" + URLEncoder.encode(hash, "UTF-8");
        log.info("fileUrl " + callbackUrl);

        return callbackUrl;
    }

    public String getCallbackUrl(Long attachmentId) throws Exception {
        String hash = DocumentManager.CreateHash(Long.toString(attachmentId));

        String callbackUrl = getJiraBaseUrl() + callbackServler + "?vkey=" + URLEncoder.encode(hash, "UTF-8");
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

    public String replaceDocEditorURLToInternal(String url) {
        String innerDocEditorUrl = getInnerDocEditorUrl();
        String publicDocEditorUrl = getPublicDocEditorUrl();
        if (!publicDocEditorUrl.equals(innerDocEditorUrl)) {
            url = url.replace(publicDocEditorUrl, innerDocEditorUrl);
        }
        return url;
    }

    public JSONObject getSaveAsObject(Long attachmentId, ApplicationUser user) throws JSONException {
        JSONObject saveAs = new JSONObject();
        saveAs.put("uri", getJiraBaseUrl() + APIServlet + "?type=save-as");
        saveAs.put("available", attachmentUtil.checkAccess(attachmentId, user, true));

        return saveAs;
    }

}