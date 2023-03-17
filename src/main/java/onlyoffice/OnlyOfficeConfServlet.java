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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.sal.api.user.UserProfile;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;

import javax.inject.Inject;

@Scanned
public class OnlyOfficeConfServlet extends HttpServlet {
    @JiraImport
    private final UserManager userManager;
    @JiraImport
    private final TemplateRenderer templateRenderer;
    private final PluginSettings pluginSettings;

    private final JwtManager jwtManager;
    private final ConfigurationManager configurationManager;
    private final DemoManager demoManager;

    @Inject
    public OnlyOfficeConfServlet(final UserManager userManager, final PluginSettingsFactory pluginSettingsFactory,
                                 final JwtManager jwtManager, final TemplateRenderer templateRenderer,
                                 final ConfigurationManager configurationManager, final DemoManager demoManager) {
        this.userManager = userManager;
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.jwtManager = jwtManager;
        this.templateRenderer = templateRenderer;
        this.configurationManager = configurationManager;
        this.demoManager = demoManager;
    }

    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeConfServlet");
    private static final long serialVersionUID = 1L;

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        UserProfile user = userManager.getRemoteUser(request);
        if (user == null || !userManager.isSystemAdmin(user.getUserKey())) {
            String baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl");
            response.sendRedirect(baseUrl);
            return;
        }

        String apiUrl = (String) pluginSettings.get("onlyoffice.apiUrl");
        String docInnerUrl = (String) pluginSettings.get("onlyoffice.docInnerUrl");
		String confUrl = (String) pluginSettings.get("onlyoffice.confUrl");
        String jwtSecret = (String) pluginSettings.get("onlyoffice.jwtSecret");
        Boolean ignoreCertificate = configurationManager.getBooleanPluginSetting("ignoreCertificate", false);
        Boolean demoEnable = demoManager.isEnable();
        Boolean demoTrialIsOver = demoManager.trialIsOver();
        Map<String, Boolean> defaultCustomizableEditingTypes = configurationManager.getCustomizableEditingTypes();

        if (apiUrl == null || apiUrl.isEmpty()) { apiUrl = ""; }
		if (docInnerUrl == null || docInnerUrl.isEmpty()) { docInnerUrl = ""; }
		if (confUrl == null || confUrl.isEmpty()) { confUrl = ""; }
		if (jwtSecret == null || jwtSecret.isEmpty()) { jwtSecret = ""; }

        response.setContentType("text/html;charset=UTF-8");

        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put("docserviceApiUrl", apiUrl);
        defaults.put("docserviceInnerUrl", docInnerUrl);
		defaults.put("docserviceConfUrl", confUrl);
        defaults.put("docserviceJwtSecret", jwtSecret);
        defaults.put("ignoreCertificate", ignoreCertificate);
        defaults.put("demoEnable", demoEnable);
        defaults.put("demoTrialIsOver", demoTrialIsOver);
        defaults.put("pathApiUrl", configurationManager.getProperty("files.docservice.url.api"));
        defaults.put("defaultCustomizableEditingTypes", defaultCustomizableEditingTypes);

        templateRenderer.render("templates/configure.vm", defaults, response.getWriter());
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        UserKey userKey = userManager.getRemoteUser(request).getUserKey();
        if (userKey == null || !userManager.isSystemAdmin(userKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String body = getBody(request.getInputStream());
        if (body.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String apiUrl;
        String docInnerUrl;
		String confUrl;
        String jwtSecret;
        Boolean demoEnable;

        try {
            JSONObject jsonObj = new JSONObject(body);

            demoEnable = jsonObj.getBoolean("demoEnable");
            pluginSettings.put("onlyoffice.demo", demoEnable.toString());

            if (demoEnable) {
                demoManager.activate();
            }

            if (demoManager.isActive()) {
                apiUrl = demoManager.getUrl();
                docInnerUrl = demoManager.getUrl();
            } else {
                apiUrl = AppendSlash(jsonObj.getString("apiUrl"));
                docInnerUrl = AppendSlash(jsonObj.getString("docInnerUrl"));
                jwtSecret = jsonObj.getString("jwtSecret");
                Boolean ignoreCertificate = jsonObj.getBoolean("ignoreCertificate");

                pluginSettings.put("onlyoffice.apiUrl", apiUrl);
                pluginSettings.put("onlyoffice.docInnerUrl", docInnerUrl);
                pluginSettings.put("onlyoffice.jwtSecret", jwtSecret);
                pluginSettings.put("onlyoffice.ignoreCertificate", ignoreCertificate.toString());
            }

            confUrl = AppendSlash(jsonObj.getString("confUrl"));
            JSONArray editingTypes = jsonObj.getJSONArray("editingTypes");

            pluginSettings.put("onlyoffice.confUrl", confUrl);
            pluginSettings.put("onlyoffice.editingTypes", editingTypes.toString());

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String error = ex.toString() + "\n" + sw.toString();
            log.error(error);

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\": false, \"message\": \"jsonparse\"}");
            return;
        }

        log.debug("Checking docserv url");
        if (!CheckDocServUrl((docInnerUrl == null || docInnerUrl.isEmpty()) ? apiUrl : docInnerUrl)) {
            response.getWriter().write("{\"success\": false, \"message\": \"docservunreachable\"}");
            return;
        }

        try {
            log.debug("Checking docserv commandservice");
            if (!CheckDocServCommandService((docInnerUrl == null || docInnerUrl.isEmpty()) ? apiUrl : docInnerUrl)) {
                response.getWriter().write("{\"success\": false, \"message\": \"docservcommand\"}");
                return;
            }
        } catch (SecurityException ex) {
            response.getWriter().write("{\"success\": false, \"message\": \"jwterror\"}");
            return;
        }

        response.getWriter().write("{\"success\": true}");
    }

    private String AppendSlash(final String str) {
        if (str == null || str.isEmpty() || str.endsWith("/")) {
            return str;
        }

        return str + "/";
    }

    private String getBody(final InputStream stream) {
        try(Scanner scanner = new Scanner(stream)) {
            try(Scanner scannerUseDelimiter = scanner.useDelimiter("\\A")) {
                return scanner.hasNext() ? scanner.next() : "";
            }
        }
    }

    private Boolean CheckDocServUrl(final String url) {
        try (CloseableHttpClient httpClient = configurationManager.getHttpClient()) {
            HttpGet request = new HttpGet(url + "healthcheck");
            try (CloseableHttpResponse response = httpClient.execute(request)) {

                String content = IOUtils.toString(response.getEntity().getContent(), "utf-8").trim();
                if (content.equalsIgnoreCase("true")) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("/healthcheck error: " + e.getMessage());
        }

        return false;
    }

    private Boolean CheckDocServCommandService(final String url) throws SecurityException {
        Integer errorCode = -1;
        try (CloseableHttpClient httpClient = configurationManager.getHttpClient()) {
            JSONObject body = new JSONObject();
            body.put("c", "version");

            HttpPost request = new HttpPost(url + "coauthoring/CommandService.ashx");

            if (jwtManager.jwtEnabled()) {
                String token = jwtManager.createToken(body);
                JSONObject payloadBody = new JSONObject();
                payloadBody.put("payload", body);
                String headerToken = jwtManager.createToken(body);
                body.put("token", token);
                String header = jwtManager.getJwtHeader();
                request.setHeader(header, "Bearer " + headerToken);
            }

            StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(requestEntity);
            request.setHeader("Accept", "application/json");

            log.debug("Sending POST to Docserver: " + body.toString());
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();

                if (status != HttpStatus.SC_OK) {
                    return false;
                } else {
                    String content = IOUtils.toString(response.getEntity().getContent(), "utf-8");
                    log.debug("/CommandService content: " + content);
                    JSONObject callBackJson = null;
                    callBackJson = new JSONObject(content);

                    if (callBackJson.isNull("error")) {
                        return false;
                    }

                    errorCode = callBackJson.getInt("error");
                }
            }
        } catch (Exception e) {
            log.debug("/CommandService error: " + e.getMessage());
            return false;
        }

        if (errorCode == 6) {
            throw new SecurityException();
        } else if (errorCode != 0) {
            return false;
        } else {
            return true;
        }
    }

    private String getBoolAsAttribute(final String value) {
        return value.equals("true") ? "checked=\"\"" : "";
    }
}
