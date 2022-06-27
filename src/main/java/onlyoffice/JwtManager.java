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

import org.json.JSONObject;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.Base64;
import java.util.Base64.Encoder;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.jira.config.util.AttachmentPathManager;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class JwtManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.UrlManager");

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    @JiraImport
    private final AttachmentPathManager attachmentPathManager;

    private final PluginSettings settings;

    @Inject
    public JwtManager(PluginSettingsFactory pluginSettingsFactory, AttachmentPathManager attachmentPathManager) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.attachmentPathManager = attachmentPathManager;
        settings = pluginSettingsFactory.createGlobalSettings();
    }

    public Boolean jwtEnabled() {
        try {
            checkDemo();
        } catch (Exception ex) {
            log.error(ex);
        }

        return settings.get("onlyoffice.jwtSecret") != null
                && !((String) settings.get("onlyoffice.jwtSecret")).isEmpty();
    }

    public String createToken(JSONObject payload) throws Exception {
        JSONObject header = new JSONObject();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Encoder enc = Base64.getUrlEncoder();

        String encHeader = enc.encodeToString(header.toString().getBytes("UTF-8")).replace("=", "");
        String encPayload = enc.encodeToString(payload.toString().getBytes("UTF-8")).replace("=", "");
        String hash = calculateHash(encHeader, encPayload);

        return encHeader + "." + encPayload + "." + hash;
    }

    public Boolean verify(String token) {
        if (!jwtEnabled())
            return false;

        String[] jwt = token.split("\\.");
        if (jwt.length != 3) {
            return false;
        }

        try {
            String hash = calculateHash(jwt[0], jwt[1]);
            if (!hash.equals(jwt[2]))
                return false;
        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    public String getJwtHeader() {
        try {
            checkDemo();
        } catch (Exception ex) {
            log.error(ex);
        }

        String header = (String) settings.get("onlyoffice.jwtHeader");
        return header == null || header.isEmpty() ? "Authorization" : header;
    }

    private String calculateHash(String header, String payload) throws Exception {
        Mac hasher;
        hasher = getHasher();
        return Base64.getUrlEncoder().encodeToString(hasher.doFinal((header + "." + payload).getBytes("UTF-8")))
                .replace("=", "");
    }

    private Mac getHasher() throws Exception {
        checkDemo();

        String jwts = (String) settings.get("onlyoffice.jwtSecret");

        Mac sha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(jwts.getBytes("UTF-8"), "HmacSHA256");
        sha256.init(secret_key);

        return sha256;
    }

    public void checkDemo() throws Exception {
        String isDemo = (String) settings.get("onlyoffice.isDemo");
        String demoDate = "";

        if (isDemo.equals("true")) {
            DemoManager.init(isDemo, settings);
            demoDate = (String) settings.get("onlyoffice.trial.date");

            if (!DemoManager.istrial(demoDate)){
                returnSettings();
            }
        }
    }

    public void returnSettings() throws Exception {
        String jwtSecret = (String) settings.get("onlyoffice.oldJwtSecret");
        settings.put("onlyoffice.jwtSecret", jwtSecret);
        return;
    }

}
