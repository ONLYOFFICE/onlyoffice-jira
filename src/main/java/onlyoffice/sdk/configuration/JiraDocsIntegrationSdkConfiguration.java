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

package onlyoffice.sdk.configuration;

import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.webresource.api.WebResourceUrlProvider;
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
import onlyoffice.utils.AttachmentUtil;
import onlyoffice.sdk.manager.document.DocumentManagerImpl;
import onlyoffice.sdk.manager.security.JwtManagerImpl;
import onlyoffice.sdk.manager.settings.SettingsManagerImpl;
import onlyoffice.sdk.manager.url.UrlManagerImpl;
import onlyoffice.sdk.service.callback.CallbackServiceImpl;
import onlyoffice.sdk.service.config.ConfigServiceImpl;
import onlyoffice.sdk.service.settings.SettingsValidationServiceImpl;

public class JiraDocsIntegrationSdkConfiguration implements DocsIntegrationSdkConfiguration {
    private final PluginSettingsFactory pluginSettingsFactory;
    private final AttachmentUtil attachmentUtil;
    private final I18nResolver i18nResolver;
    private final WebResourceUrlProvider webResourceUrlProvider;
    private DocumentServerClient documentServerClient;
    private DocumentManager documentManager;
    private JwtManager jwtManager;
    private ConvertService convertService;

    public JiraDocsIntegrationSdkConfiguration(final PluginSettingsFactory pluginSettingsFactory,
                                               final AttachmentUtil attachmentUtil, final I18nResolver i18nResolver,
                                               final WebResourceUrlProvider webResourceUrlProvider) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.attachmentUtil = attachmentUtil;
        this.i18nResolver = i18nResolver;
        this.webResourceUrlProvider = webResourceUrlProvider;
    }

    @Override
    public DocumentServerClient documentServerClient(final SettingsManager settingsManager,
                                              final UrlManager urlManager) {
        this.documentServerClient = DocsIntegrationSdkConfiguration.super
                .documentServerClient(settingsManager, urlManager);

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
    public JwtManager jwtManager(final SettingsManager settingsManager) {
        this.jwtManager = new JwtManagerImpl(settingsManager);

        return this.jwtManager;
    }

    @Override
    public UrlManager urlManager(final SettingsManager settingsManager) {
        return new UrlManagerImpl(
                settingsManager,
                webResourceUrlProvider,
                attachmentUtil,
                (onlyoffice.sdk.manager.security.JwtManager) jwtManager,
                documentManager
        );
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
        return new SettingsValidationServiceImpl(documentServerClient, urlManager, i18nResolver);
    }
}
