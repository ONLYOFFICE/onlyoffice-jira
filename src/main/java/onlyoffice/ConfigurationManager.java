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

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

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
import java.util.*;

@Named
public class ConfigurationManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.ConfigurationManager");
    private static final String CONFIGURATION_PATH = "onlyoffice-config.properties";

    private final PluginSettings pluginSettings;
    private final DemoManager demoManager;

    @Inject
    public ConfigurationManager(PluginSettingsFactory pluginSettingsFactory, DemoManager demoManager) {
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

    public String getProperty(String propertyName){
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

    public Boolean getBooleanPluginSetting(String key, Boolean defaultValue) {
        String setting = (String) pluginSettings.get("onlyoffice." + key);
        if (setting == null || setting.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(setting);
    }

    public List<String> getListDefaultProperty(String nameProperty) {
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
        Integer timeout = Integer.parseInt(getProperty("timeout")) * 1000;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).build();

        CloseableHttpClient httpClient;

        if (getBooleanPluginSetting("ignoreCertificate", false) && !demoManager.isActive()) {
            SSLContextBuilder builder = new SSLContextBuilder();

            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            });

            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(builder.build(), new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).setDefaultRequestConfig(config).build();
        } else {
            httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        }

        return httpClient;
    }

    public List<String> getDefaultEditingTypes() {
        String editableTypes = getProperty("docservice.type.edit");
        return new ArrayList<>(Arrays.asList(editableTypes.split("\\|")));
    }

    public Map<String, Boolean> getCustomizableEditingTypes() {
        Map<String, Boolean> customizableEditingTypes = new HashMap<>();
        List<String> editingTypes = null;

        String editingTypesString = (String) pluginSettings.get("onlyoffice.editingTypes");

        if (editingTypesString != null && !editingTypesString.isEmpty()) {
            editingTypes = Arrays.asList(editingTypesString.substring(1, editingTypesString.length() - 1).replace("\"", "").split(","));
        } else {
            editingTypes = Arrays.asList("csv", "txt");
        }

        List<String> availableTypes = Arrays.asList(getProperty("docservice.type.edit.customizable").split("\\|"));

        for (String type : availableTypes) {
            customizableEditingTypes.put(type, editingTypes.contains(type));
        }

        return customizableEditingTypes;
    }
}
