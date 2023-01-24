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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import onlyoffice.constants.Format;
import onlyoffice.constants.Formats;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

@Named
public class DocumentManager {
    private static final Logger log = LogManager.getLogger("onlyoffice.DocumentManager");

    private final AttachmentUtil attachmentUtil;
    private final ConfigurationManager configurationManager;

    @Inject
    public DocumentManager(AttachmentUtil attachmentUtil, ConfigurationManager configurationManager) {
        this.attachmentUtil = attachmentUtil;
        this.configurationManager = configurationManager;
    }

    public List<Format> enrichmentSupportedFormats(List<Format> supportedFormats) {
        List<Format> extendSupportedFormats = new ArrayList<>();
        Map<String, Boolean> customizableEditingTypes = configurationManager.getCustomizableEditingTypes();
        for (Map.Entry<String, Boolean> customizableEditingType : customizableEditingTypes.entrySet()) {
            extendSupportedFormats = supportedFormats.stream()
                    .peek(format -> {
                        if (format.getName().equals(customizableEditingType.getKey())) {
                            format.setEdit(customizableEditingType.getValue());
                        }
                    })
                    .collect(Collectors.toList());
        }
        return extendSupportedFormats;
    }

    public long GetMaxFileSize() {
        long size;
        try {
            String filesizeMax = configurationManager.getProperty("filesize-max");
            size = Long.parseLong(filesizeMax);
        } catch (Exception ex) {
            size = 0;
        }

        return size > 0 ? size : 5 * 1024 * 1024;
    }

    public boolean isEditable(String ext) {
        List<Format> supportedFormats = Formats.getSupportedFormats();
        List<Format> enrichmentSupportedFormats = this.enrichmentSupportedFormats(supportedFormats);
        for (Format format : enrichmentSupportedFormats) {
            if (format.getName().equals(ext)) {
                return format.isEdit();
            }
        }
        return false;
    }

    public String getDefaultExtForEditableFormats(String ext) {
        List<Format> supportedFormats = Formats.getSupportedFormats();
        for (Format format : supportedFormats) {
            if (format.getName().equals(ext)) {
                return this.getDefaultExtByType(format.getType().name().toLowerCase());
            }
        }
        return ext;
    }

    public boolean isFillForm(String ext) {
        List<Format> supportedFormats = Formats.getSupportedFormats();
        boolean isFillForm = false;

        for (Format format : supportedFormats) {
            if (format.getName().equals(ext)) {
                isFillForm = format.isFillForm();
                break;
            }
        }

        return isFillForm;
    }

    public String getDocType(String ext) {
        List<Format> supportedFormats = Formats.getSupportedFormats();

        for (Format format : supportedFormats) {
            if (format.getName().equals(ext)) {

                String type = format.getType().name().toLowerCase().equals("form") ? "word" : format.getType().name().toLowerCase();

                return type;
            }
        }

        return null;
    }

    public String getDefaultExtByType(String type) {
        switch (type) {
            case "word":
                return "docx";
            case "cell":
                return "xlsx";
            case "slide":
                return "pptx";
            case "form":
                return "docxf";
            default:
                return null;
        }
    }

    public String getKeyOfFile(Long attachmentId) {
        String key = attachmentUtil.getProperty(attachmentId, "onlyoffice-collaborative-editor-key");

        if (key == null || key.isEmpty()) {
            key = UUID.randomUUID().toString().replace("-", "");
            attachmentUtil.setProperty(attachmentId, "onlyoffice-collaborative-editor-key", key);
        }

        return key;
    }

    public String CreateHash(String str) {
        try {
            String secret = configurationManager.getProperty("files.docservice.secret");

            String payload = GetHashHex(str + secret) + "?" + str;

            String base64 = Base64.getEncoder().encodeToString(payload.getBytes("UTF-8"));
            return base64;
        } catch (Exception ex) {
            log.error(ex);
        }
        return "";
    }

    public String ReadHash(String base64) {
        try {
            String str = new String(Base64.getDecoder().decode(base64), "UTF-8");

            String secret = configurationManager.getProperty("files.docservice.secret");

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

    public String getMimeType(String name) {
        Path path = new File(name).toPath();
        String mimeType = null;
        try {
            mimeType = Files.probeContentType(path);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return mimeType != null ? mimeType : "application/octet-stream";
    }
}
