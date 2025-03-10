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

package onlyoffice.sdk.configuration;

import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.configuration.DocsIntegrationSdkConfiguration;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.service.convert.ConvertService;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import com.onlyoffice.service.documenteditor.config.ConfigService;
import com.onlyoffice.service.settings.SettingsValidationService;
import onlyoffice.AttachmentUtil;
import onlyoffice.sdk.manager.document.DocumentManagerImpl;
import onlyoffice.sdk.manager.settings.SettingsManagerImpl;
import onlyoffice.sdk.manager.url.UrlManagerImpl;
import onlyoffice.sdk.service.callback.CallbackServiceImpl;
import onlyoffice.sdk.service.config.ConfigServiceImpl;
import onlyoffice.sdk.service.settings.SettingsValidationServiceImpl;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class JiraDocsIntegrationSdkConfiguration implements DocsIntegrationSdkConfiguration {
    @JiraImport
    private final PluginSettingsFactory pluginSettingsFactory;
    private final AttachmentUtil attachmentUtil;
    private final I18nResolver i18n;
    private DocumentServerClient documentServerClient;
    private DocumentManager documentManager;
    private ConvertService convertService;

    @Inject
    public JiraDocsIntegrationSdkConfiguration(final PluginSettingsFactory pluginSettingsFactory,
                                               final AttachmentUtil attachmentUtil, final I18nResolver i18n) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.attachmentUtil = attachmentUtil;
        this.i18n = i18n;
    }

    @Override
    public DocumentServerClient documentServerClient(final SettingsManager settingsManager,
                                              final UrlManager urlManager, final JwtManager jwtManager) {
        this.documentServerClient = DocsIntegrationSdkConfiguration.super
                .documentServerClient(settingsManager, urlManager, jwtManager);

        return this.documentServerClient;
    }

    @Override
    public SettingsManager settingsManager() {
        return new SettingsManagerImpl(pluginSettingsFactory);
    }

    @Override
    public DocumentManager documentManager(final SettingsManager settingsManager) {
        this.documentManager = new DocumentManagerImpl(settingsManager, attachmentUtil);

        return this.documentManager;
    }

    @Override
    public UrlManager urlManager(final SettingsManager settingsManager) {
        return new UrlManagerImpl(settingsManager, attachmentUtil);
    }

    @Override
    public ConfigService configService(final DocumentManager documentManager, final UrlManager urlManager,
                                       final JwtManager jwtManager, final SettingsManager settingsManager) {
        return new ConfigServiceImpl(documentManager, urlManager, jwtManager,
                settingsManager, attachmentUtil);
    }

    @Override
    public ConvertService convertService(final DocumentManager documentManager, final UrlManager urlManager,
                                         final DocumentServerClient documentServerClient) {
        this.convertService = DocsIntegrationSdkConfiguration.super
                .convertService(documentManager, urlManager, documentServerClient);

        return this.convertService;
    }

    @Override
    public CallbackService callbackService(final JwtManager jwtManager, final SettingsManager settingsManager) {
        return new CallbackServiceImpl(
                jwtManager,
                settingsManager,
                attachmentUtil,
                this.documentServerClient,
                this.documentManager,
                this.convertService
        );
    }

    @Override
    public SettingsValidationService settingsValidationService(final DocumentServerClient documentServerClient,
                                                               final UrlManager urlManager) {
        return new SettingsValidationServiceImpl(documentServerClient, urlManager, i18n);
    }
}
