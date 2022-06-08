package onlyoffice;


import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
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
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.net.URL;
import java.util.UUID;

@Named
public class ConversionManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.ConversionManager");

    @JiraImport
    private final PluginSettingsFactory pluginSettingsFactory;
    private final JwtManager jwtManager;

    @Inject
    public ConversionManager(   PluginSettingsFactory pluginSettingsFactory, JwtManager jwtManager) {
                
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
                                    String filePass, Boolean isAsync, String lang) 
                                    throws Exception {

        String DocumentConverterUrl = docUrl + "ConvertService.ashx";
        
        String title = filename + "." + toExtension;
        title = title == null || title.isEmpty() ? UUID.randomUUID().toString() : title;

        String documentRevisionId = GenerateRevisionId(documentUri + title);
        try (CloseableHttpClient httpClient = GetClient()) {
            JSONObject body = new JSONObject();
            body.put("region", lang);
            body.put("url", documentUri);
            body.put("outputtype", toExtension);
            body.put("filetype", fromExtension);
            body.put("title", title);
            body.put("key", documentRevisionId);
            body.put("password", filePass);
            if (isAsync) body.put("async", true);

            StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
                HttpPost request = new HttpPost(DocumentConverterUrl);
                request.setEntity(requestEntity);
                request.setHeader("Accept", "application/json");

            if (jwtManager.jwtEnabled()) {
                String token = jwtManager.createToken(body);
                JSONObject payloadBody = new JSONObject();
                payloadBody.put("payload", body);
                String headerToken = jwtManager.createToken(body);
                body.put("token", token);
                request.setHeader(jwtManager.getJwtHeader(), "Bearer " + headerToken);
            }

            log.debug("Sending POST to Docserver: " + body.toString());
            try(CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();

                if(status != HttpStatus.SC_OK) {
                    throw new HttpException("Docserver returned code " + status);
                } else {
                    String content = IOUtils.toString(response.getEntity().getContent(), "utf-8");

                    log.debug("Docserver returned: " + content);
                    JSONObject callBackJSon = null;
                    try{
                        callBackJSon = new JSONObject(content);
                    } catch (Exception e) {
                        throw new Exception("Couldn't convert JSON from docserver: " + e.getMessage());
                    }

                    if (!callBackJSon.isNull("error") && callBackJSon.getInt("error") == -8) throw new SecurityException();
                    
                    if (callBackJSon.isNull("endConvert") || !callBackJSon.getBoolean("endConvert") || callBackJSon.isNull("fileUrl")) {
                        throw new Exception("'endConvert' is false or 'fileUrl' is empty");
                    }
                    return callBackJSon.getString("fileUrl");
                }
            }
        }
    }

    public static String GenerateRevisionId(String expectedKey) {
        if (expectedKey.length() > 20)
            expectedKey = Integer.toString(expectedKey.hashCode());

        String key = expectedKey.replace("[^0-9-.a-zA-Z_=]", "_");

        return key.substring(0, Math.min(key.length(), 20));
    }

    private CloseableHttpClient GetClient() throws Exception {
        CloseableHttpClient httpClient;

        String cert = "false";
        if (cert.equals("true")) {
            log.debug("Ignoring SSL");
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            });
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
            httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        } else {
            httpClient = HttpClients.createDefault();
        }

        return httpClient;
    }
}
