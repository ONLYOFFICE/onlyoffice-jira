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
 */

package onlyoffice;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import onlyoffice.constants.Format;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static onlyoffice.constants.Formats.getSupportedFormats;

@Named
public class ConfigurationManager {
    private final Logger log = LogManager.getLogger("onlyoffice.ConfigurationManager");
    private static final String CONFIGURATION_PATH = "onlyoffice-config.properties";

    private final PluginSettings pluginSettings;
    private final DemoManager demoManager;

    @Inject
    public ConfigurationManager(final PluginSettingsFactory pluginSettingsFactory, final DemoManager demoManager) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
        this.demoManager = demoManager;
    }

    public Properties getProperties() throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIGURATION_PATH);
        if (inputStream != null) {
            properties.load(inputStream);
        }
        return properties;
    }

    public String getProperty(final String propertyName) {
        try {
            Properties properties = getProperties();
            String property = properties.getProperty(propertyName);
            return property;
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.error(e.toString() + "\n" + sw.toString());
            return null;
        }
    }

    public Boolean getBooleanPluginSetting(final String key, final Boolean defaultValue) {
        String setting = (String) pluginSettings.get("onlyoffice." + key);
        if (setting == null || setting.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(setting);
    }

    public List<String> getListDefaultProperty(final String nameProperty) {
        try {
            Properties properties = getProperties();
            String property = properties.getProperty(nameProperty);

            return Arrays.asList(property.split("\\|"));
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            log.error(e.toString() + "\n" + sw.toString());
            return null;
        }

    }

    public CloseableHttpClient getHttpClient() throws Exception {
        Integer timeout = (int) TimeUnit.SECONDS.toMillis(Long.parseLong(getProperty("timeout")));

        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).build();

        CloseableHttpClient httpClient;

        if (getBooleanPluginSetting("ignoreCertificate", false) && !demoManager.isActive()) {
            SSLContextBuilder builder = new SSLContextBuilder();

            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(final X509Certificate[] chain, final String authType)
                        throws CertificateException {
                    return true;
                }
            });

            SSLConnectionSocketFactory sslConnectionSocketFactory =
                    new SSLConnectionSocketFactory(builder.build(), new HostnameVerifier() {
                        @Override
                        public boolean verify(final String hostname, final SSLSession session) {
                            return true;
                        }
                    });

            httpClient =
                    HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).setDefaultRequestConfig(config)
                            .build();
        } else {
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        }

        return httpClient;
    }

    public Map<String, Boolean> getCustomizableEditingTypes() {
        Map<String, Boolean> customizableEditingTypes = new HashMap<>();
        List<String> editingTypes = null;

        String editingTypesString = (String) pluginSettings.get("onlyoffice.editingTypes");

        if (editingTypesString != null && !editingTypesString.isEmpty()) {
            editingTypes = Arrays.asList(
                    editingTypesString.substring(1, editingTypesString.length() - 1).replace("\"", "").split(","));
        } else {
            editingTypes = Arrays.asList("csv", "txt");
        }

        for (Format format : getSupportedFormats()) {
            if (format.isCustomizable()) {
                customizableEditingTypes.put(format.getName(), editingTypes.contains(format.getName()));
            }
        }

        return customizableEditingTypes;
    }
}
