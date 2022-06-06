package onlyoffice;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import javax.inject.Inject;
import javax.inject.Named;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

@Named
public class ConversionManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.ConversionManager");

    @JiraImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private final JwtManager jwtManager;

    @Inject
    public ConversionManager(PluginSettingsFactory pluginSettingsFactory, JwtManager jwtManager) {
                
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.jwtManager = jwtManager;
    }

    private static int ConvertTimeout = 120000;

    public static class ConvertBody {

        public String region;
        public String url;
        public String outputtype;
        public String filetype;
        public String title;
        public String key;
        public Boolean async;
        public String token;
        public String password;
    }

    public String GetConvertedUri(  String docUrl, String jwtSecret,
                                    String documentUri, String filename, String fromExtension, String toExtension, 
                                    String filePass, Boolean isAsync, String lang, HttpServletResponse response) 
                                    throws Exception {

        String DocumentConverterUrl = docUrl + "ConvertService.ashx";
        
        String title = filename + "." + toExtension;
        title = title == null || title.isEmpty() ? UUID.randomUUID().toString() : title;

        String documentRevisionId = GenerateRevisionId(documentUri + title);
        JSONObject body = new JSONObject();
        body.put("region", lang);
        body.put("url", documentUri);
        body.put("outputtype", toExtension);
        body.put("filetype", fromExtension);
        body.put("title", title);
        body.put("key", documentRevisionId);
        body.put("password", filePass);
        if (isAsync) body.put("async", true);

        String headerToken = "";
        if (jwtSecret != null && !jwtSecret.isEmpty()) {
            JSONObject map = new JSONObject();
            map.put("region", body.get("region"));
            map.put("url", body.get("url"));
            map.put("outputtype", body.get("outputtype"));
            map.put("filetype", body.get("filetype"));
            map.put("title", body.get("title"));
            map.put("key", body.get("key"));
            map.put("password", body.get("password"));
            if (isAsync) map.put("async", body.get("async"));

            String token = jwtManager.createToken(map);
            body.put("token", token);

            JSONObject payloadMap = new JSONObject();
            payloadMap.put("payload", map);
            headerToken = jwtManager.createToken(payloadMap);
        }

        byte[] bodyByte = body.toString().getBytes(StandardCharsets.UTF_8);

        URL url = new URL(DocumentConverterUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setFixedLengthStreamingMode(bodyByte.length);
        connection.setRequestProperty("Accept", "application/json");
        connection.setConnectTimeout(ConvertTimeout);

        if (jwtSecret != null && !jwtSecret.isEmpty())
            connection.setRequestProperty(jwtManager.getJwtHeader(), "Bearer " + headerToken);
        connection.connect();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(bodyByte);
            os.flush();
        }
        String jsonString = null;
        try (InputStream stream = connection.getInputStream()) {
            if (stream == null)
                throw new Exception("Could not get an answer");
            jsonString = ConvertStreamToString(stream);
        } catch (Exception ex) {
            response.setHeader("__ERROR__", ex.toString());
        } finally {
            connection.disconnect();
        }
        return GetResponseUri(jsonString);
    }

    public static String GenerateRevisionId(String expectedKey) {
        if (expectedKey.length() > 20)
            expectedKey = Integer.toString(expectedKey.hashCode());

        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), 20));
    }

    private static String GetResponseUri(String jsonString) throws Exception {
        JSONObject jsonObj = ConvertStringToJSON(jsonString);

        Object error = jsonObj.get("error");
        if (error != null)
            log.error(Math.toIntExact((long)error));

        Boolean isEndConvert = (Boolean) jsonObj.get("endConvert");

        Long resultPercent = 0l;
        String responseUri = null;

        if (isEndConvert) {
            resultPercent = 100l;
            responseUri = (String) jsonObj.get("fileUrl");
        }
        else {
            resultPercent = (Long) jsonObj.get("percent");
            resultPercent = resultPercent >= 100l ? 99l : resultPercent;
        }

        return resultPercent >= 100l ? responseUri : "";
    }

    public static String ConvertStreamToString(InputStream stream) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line = bufferedReader.readLine();

        while (line != null) {
            stringBuilder.append(line);
            line = bufferedReader.readLine();
        }

        String result = stringBuilder.toString();

        return result;
    }

    public static JSONObject ConvertStringToJSON(String jsonString) throws ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(jsonString);
        JSONObject jsonObj = (JSONObject) obj;

        return jsonObj;
    }
}
