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

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import javax.inject.Inject;
import javax.inject.Named;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Named
public class DemoManager {

    private final PluginSettings pluginSettings;

    private static final String URL = "https://onlinedocs.onlyoffice.com/";
    private static final String HEADER = "AuthorizationJWT";
    private static final String SECRET = "sn2puSUF7muF5Jas";
    private static final String TRIAL = "30";


    @Inject
    public DemoManager(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }

    public void activate() {
        String demoStart = (String) pluginSettings.get("onlyoffice.demoStart");
        if (demoStart == null || demoStart.isEmpty()) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            pluginSettings.put("onlyoffice.demoStart", dateFormat.format(date));
        }
    }

    public boolean isEnable() {
        String demo = (String) pluginSettings.get("onlyoffice.demo");

        if (demo == null || demo.isEmpty()) {
            return false;
        }

        return Boolean.parseBoolean(demo);
    }

    public Boolean trialIsOver() {
        String demoStart = (String) pluginSettings.get("onlyoffice.demoStart");

        if (demoStart != null && !demoStart.isEmpty()) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            try {
                Calendar date = Calendar.getInstance();
                date.setTime(dateFormat.parse(demoStart));
                date.add(Calendar.DATE, Integer.parseInt(TRIAL));

                return !date.after(Calendar.getInstance());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public Boolean isActive() {
        return isEnable() && !trialIsOver();
    }

    public String getUrl() {
        return URL;
    }

    public String getHeader() {
        return HEADER;
    }

    public String getSecret() {
        return SECRET;
    }
}