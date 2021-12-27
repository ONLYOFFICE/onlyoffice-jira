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

import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

@Named
public class DocumentManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.DocumentManager");

    private final AttachmentUtil attachmentUtil;

    @Inject
    public DocumentManager(AttachmentUtil attachmentUtil) {
        this.attachmentUtil = attachmentUtil;
    }

    public static long GetMaxFileSize() {
        long size;
        try {
            ConfigurationManager configurationManager = new ConfigurationManager();
            Properties properties = configurationManager.GetProperties();
            String filesizeMax = properties.getProperty("filesize-max");
            size = Long.parseLong(filesizeMax);
        } catch (Exception ex) {
            size = 0;
        }

        return size > 0 ? size : 5 * 1024 * 1024;
    }

    public static List<String> GetEditedExts() {
        ConfigurationManager configurationManager = new ConfigurationManager();
        List<String> editedExts = configurationManager.getListDefaultProperty("files.docservice.edited-docs");

        return editedExts;
    }

    public static List<String> GetFillFormExts() {
        ConfigurationManager configurationManager = new ConfigurationManager();
        List<String> fillformExts = configurationManager.getListDefaultProperty("files.docservice.fill-docs");

        return fillformExts;
    }

    public String getDocType(String ext) {
        ConfigurationManager configurationManager = new ConfigurationManager();
        List<String> wordFormats = configurationManager.getListDefaultProperty("files.docservice.type.word");
        List<String> cellFormats = configurationManager.getListDefaultProperty("files.docservice.type.cell");
        List<String> slideFormats = configurationManager.getListDefaultProperty("files.docservice.type.slide");

        if (wordFormats.contains(ext)) return "text";
        if (cellFormats.contains(ext)) return "spreadsheet";
        if (slideFormats.contains(ext)) return "presentation";

        return null;
    }

    public String getKeyOfFile(Long attachmentId) {
        String key = attachmentUtil.getProperty(attachmentId, "onlyoffice-collaborative-editor-key");

        if (key == null || key.isEmpty()) {
            key = UUID.randomUUID().toString().replace("-", "");
            attachmentUtil.setProperty(attachmentId, "onlyoffice-collaborative-editor-key", key);
        }

        return key;
    }

    public static String CreateHash(String str) {
        try {
            ConfigurationManager configurationManager = new ConfigurationManager();
            Properties properties = configurationManager.GetProperties();
            String secret = properties.getProperty("files.docservice.secret");

            String payload = GetHashHex(str + secret) + "?" + str;

            String base64 = Base64.getEncoder().encodeToString(payload.getBytes("UTF-8"));
            return base64;
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    public static String ReadHash(String base64) {
        try {
            String str = new String(Base64.getDecoder().decode(base64), "UTF-8");

            ConfigurationManager configurationManager = new ConfigurationManager();
            Properties properties = configurationManager.GetProperties();
            String secret = properties.getProperty("files.docservice.secret");

            String[] payloadParts = str.split("\\?");

            String payload = GetHashHex(payloadParts[1] + secret);
            if (payload.equals(payloadParts[0])) {
                return payloadParts[1];
            }
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    private static String GetHashHex(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(str.getBytes());
            String hex = new String(Hex.encodeHex(digest));

            return hex;
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }
}
