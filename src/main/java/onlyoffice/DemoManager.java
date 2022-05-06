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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.jira.config.util.AttachmentPathManager;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Date;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Scanned
public class DemoManager {
    private static final JSONObject demoConf = new JSONObject();
    static {
        demoConf.put("confUrl", "https://onlinedocs.onlyoffice.com/");
        demoConf.put("jwtHeader", "AuthorizationJWT");
        demoConf.put("jwtSecret", "sn2puSUF7muF5Jas");
        demoConf.put("trial", "30");
    }
    @JiraImport
    private final PluginSettingsFactory pluginSettingsFactory;

    @JiraImport
    private final AttachmentPathManager AttachmentPathManager;

    private final PluginSettings pluginSettings;

    @Inject
    public DemoManager(PluginSettingsFactory pluginSettingsFactory, AttachmentPathManager AttachmentPathManager) {
        this.AttachmentPathManager = AttachmentPathManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        pluginSettings = pluginSettingsFactory.createGlobalSettings();
    }

    public static String init(String demoActevate, String rootAttachPath) throws Exception {
        String DemoDir = "ONLIOFFICE";
        String Config = "config.json";
        String path = rootAttachPath + "/" + DemoDir + "/" + Config;
        String data;
        String date;

        new File(rootAttachPath + "/" + DemoDir).mkdirs();
        File file = new File(path);

        if (file.createNewFile()){
            createTrialData(file, false);
        }

        JSONObject jsonObject = (JSONObject) readJson(path);
        date = jsonObject.get("date").toString();
        if (date.equals("none")) {
            if(demoActevate == "true"){
                createTrialData(file, true);
                jsonObject = (JSONObject) readJson(path);
            }
        }

        FileWriter writer = new FileWriter (rootAttachPath + "/" + DemoDir + "/test.json");
        writer.write(demoConf.toJSONString());
        writer.close();

        return jsonObject.get("date").toString();
    }

    public static void createTrialData(File file, Boolean activate) throws Exception {
        Date date = new Date();

        JSONObject dataObject = new JSONObject();
        if (activate){
            dataObject.put("date", date.getTime());
        } else {
            dataObject.put("date", "none");
        }

        FileWriter writer = new FileWriter (file);
        writer.write(dataObject.toJSONString());
        writer.close();
    }

    public static Boolean istrial(String date) {
        Long delataDays = (long)1000*60*60*24*(Integer.parseInt(demoConf.get("trial").toString()) + 1);

	    Date dateNow = new Date();
        Date dateEndTrial = new Date(Long.parseLong(date) + delataDays);

        if (dateNow.before(dateEndTrial)){
            return true;
        }
        return false;
    }

    public static String[] getDemoConf() throws Exception {
        String address = demoConf.get("confUrl").toString();
        String jwtHeader = demoConf.get("jwtHeader").toString();
        String jwtsecret = demoConf.get("jwtSecret").toString();

        return new String[] { address, jwtHeader, jwtsecret};
    }

    public static Object readJson(String filename) throws Exception {
        FileReader reader = new FileReader(filename);
        JSONParser parser = new JSONParser();

        return parser.parse(reader);
    }
}
