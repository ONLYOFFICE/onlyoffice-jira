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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Named
public class ConfigurationManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.ConfigurationManager");
    private static final String CONFIGURATION_PATH = "onlyoffice-config.properties";

    private final PluginSettings pluginSettings;

    @Inject
    public ConfigurationManager(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
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

}
