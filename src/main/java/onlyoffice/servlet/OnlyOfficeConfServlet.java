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

package onlyoffice.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.sal.api.user.UserKey;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.settings.Settings;
import com.onlyoffice.model.settings.SettingsConstants;
import com.onlyoffice.model.settings.security.Security;
import com.onlyoffice.model.settings.validation.ValidationResult;
import onlyoffice.sdk.service.settings.SettingsValidationService;
import onlyoffice.utils.ParsingUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;


public class OnlyOfficeConfServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserManager userManager;
    private final TemplateRenderer templateRenderer;

    private final SettingsManager settingsManager;
    private final DocumentManager documentManager;
    private final SettingsValidationService settingsValidationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnlyOfficeConfServlet(final UserManager userManager, final TemplateRenderer templateRenderer,
                                 final DocsIntegrationSdkContext docsIntegrationSdk) {
        this.userManager = userManager;
        this.templateRenderer = templateRenderer;

        this.settingsManager = docsIntegrationSdk.getSettingsManager();
        this.documentManager = docsIntegrationSdk.getDocumentManager();
        this.settingsValidationService = (SettingsValidationService) docsIntegrationSdk.getSettingsValidationService();
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException {
        UserProfile user = userManager.getRemoteUser(request);
        if (user == null || !userManager.isSystemAdmin(user.getUserKey())) {
            String baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl");
            response.sendRedirect(baseUrl);
            return;
        }

        Boolean demoAvailable = settingsManager.isDemoAvailable();
        Map<String, Boolean> defaultCustomizableEditingTypes = documentManager.getLossyEditableMap();

        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put("demoAvailable", demoAvailable);
        defaults.put("pathApiUrl", settingsManager.getDocsIntegrationSdkProperties().getDocumentServer().getApiUrl());

        if (settingsManager.getSetting(SettingsConstants.LOSSY_EDIT) == null
                || settingsManager.getSetting(SettingsConstants.LOSSY_EDIT).isEmpty()) {
            defaultCustomizableEditingTypes.put("txt", true);
            defaultCustomizableEditingTypes.put("csv", true);
        }

        defaults.put("defaultCustomizableEditingTypes", defaultCustomizableEditingTypes);

        try {
            Map<String, String> settings = settingsManager.getSettings();

            if (settings.get("customization.help") == null || settings.get("customization.help").isEmpty()) {
                settings.put("customization.help", "true");
            }

            if (settings.get("customization.chat") == null || settings.get("customization.chat").isEmpty()) {
                settings.put("customization.chat", "true");
            }

            if (settings.get("customization.macros") == null || settings.get("customization.macros").isEmpty()) {
                settings.put("customization.macros", "true");
            }

            defaults.put("settings", settings);
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        templateRenderer.render("templates/configure.vm", defaults, response.getWriter());
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        UserKey userKey = userManager.getRemoteUser(request).getUserKey();
        if (userKey == null || !userManager.isSystemAdmin(userKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String body = ParsingUtils.getBody(request.getInputStream());
        if (body.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Settings settings = objectMapper.readValue(body, Settings.class);

        if (settings.getDemo() != null && settings.getDemo()) {
            settingsManager.enableDemo();
        } else {
            settingsManager.disableDemo();
        }

        if (settingsManager.isDemoActive()) {
            Security security = settings.getSecurity();
            security.setKey(null);
            security.setHeader(null);

            settings.setUrl(null);
            settings.setInnerUrl(null);
            settings.setSecurity(security);
        }

        try {
            settingsManager.setSettings(settings);
        } catch (IntrospectionException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        Map<String, ValidationResult> validationResults = settingsValidationService.validateSettings(settings);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("validationResults", validationResults);

        response.getWriter().write(objectMapper.writeValueAsString(responseMap));
    }
}
