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

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.user.ApplicationUser;
import onlyoffice.constants.Format;
import onlyoffice.constants.Formats;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Named
public class ConversionManager {
    private final Logger log = LogManager.getLogger("onlyoffice.convert.ConvertManager");

    private final UrlManager urlManager;
    private final JwtManager jwtManager;
    private final ConfigurationManager configurationManager;
    private final DocumentManager documentManager;
    private final LocaleManager localeManager;

    @Inject
    public ConversionManager(UrlManager urlManager, JwtManager jwtManager,
                             ConfigurationManager configurationManager,
                             DocumentManager documentManager, LocaleManager localeManager) {
        this.urlManager = urlManager;
        this.jwtManager = jwtManager;
        this.configurationManager = configurationManager;
        this.documentManager = documentManager;
        this.localeManager = localeManager;
    }

    public String getTargetExt(String ext) {
        List<Format> supportedFormats = Formats.getSupportedFormats();

        for (Format format : supportedFormats) {
            if (format.getName().equals(ext)) {
                switch(format.getType()) {
                    case FORM:
                        if (format.getConvertTo().contains("oform")) return "oform";
                        break;
                    case WORD:
                        if (format.getConvertTo().contains("docx")) return "docx";
                        break;
                    case CELL:
                        if (format.getConvertTo().contains("xlsx")) return "xlsx";
                        break;
                    case SLIDE:
                        if (format.getConvertTo().contains("pptx")) return "pptx";
                        break;
                    default:
                        break;
                }
            }
        }

        return null;
    }

    public List<String> getTargetExtList(String ext) {
        List<Format> supportedFormats = Formats.getSupportedFormats();

        for (Format format : supportedFormats) {
            if (format.getName().equals(ext)) {
                return format.getConvertTo();
            }
        }

        return null;
    }

    public JSONObject convert(Long attachmentId, String ext, String convertToExt, ApplicationUser user) throws Exception {
        String url = urlManager.GetFileUri(attachmentId);
        String region = localeManager.getLocaleFor(user).toLanguageTag();
        return this.convert(attachmentId, null, ext, convertToExt, url, region, true);
    }

    public JSONObject convert(Long attachmentId, String title, String currentExt, String convertToExt, String url, String region, boolean async) throws Exception {
        try (CloseableHttpClient httpClient = configurationManager.getHttpClient()) {
            JSONObject body = new JSONObject();
            body.put("async", async);
            body.put("embeddedfonts", true);
            body.put("filetype", currentExt);
            body.put("outputtype", convertToExt);
            body.put("key", documentManager.getKeyOfFile(attachmentId));
            body.put("title", title);
            body.put("url", url);
            body.put("region", region);

            StringEntity requestEntity = new StringEntity(body.toString(), ContentType.APPLICATION_JSON);
            String conversionServiceUrl = urlManager.getInnerDocEditorUrl() + configurationManager.getProperties().getProperty("files.docservice.url.convert");

            HttpPost request = new HttpPost(conversionServiceUrl);

            request.setEntity(requestEntity);
            request.setHeader("Accept", "application/json");

            if (jwtManager.jwtEnabled()) {
                String token = jwtManager.createToken(body);
                JSONObject payloadBody = new JSONObject();
                payloadBody.put("payload", body);
                String headerToken = jwtManager.createToken(body);
                body.put("token", token);
                String header = jwtManager.getJwtHeader();
                request.setHeader(header, "Bearer " + headerToken);
            }

            log.debug("Sending POST to Conversion service: " + body.toString());
            JSONObject callBackJson = null;

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();

                if (status != HttpStatus.SC_OK) {
                    log.error("Conversion service returned code " + status + ". URL: " + conversionServiceUrl);
                    callBackJson.put("error", -10);
                } else {
                    InputStream is = response.getEntity().getContent();
                    String content = IOUtils.toString(is, String.valueOf(StandardCharsets.UTF_8));

                    log.debug("Conversion service returned: " + content);

                    callBackJson = new JSONObject(content);
                }
            }

            return callBackJson;
        }
    }
}
