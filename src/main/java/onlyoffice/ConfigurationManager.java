/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConfigurationManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.ConfigurationManager");

    public Properties GetProperties() {
        try {
            Properties properties = new Properties();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("onlyoffice-config.properties");
            if (inputStream != null) {
                properties.load(inputStream);
            }
            return properties;
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    public List<String> getListDefaultProperty(String nameProperty) {
        Properties properties = GetProperties();
        String property = properties.getProperty(nameProperty);

        return Arrays.asList(property.split("\\|"));
    }
}
