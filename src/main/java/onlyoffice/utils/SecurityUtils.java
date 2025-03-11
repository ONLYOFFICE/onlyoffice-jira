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

package onlyoffice.utils;

import org.apache.commons.codec.binary.Hex;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Properties;

public final class SecurityUtils {
    private static final String CONFIGURATION_PATH = "onlyoffice-config.properties";

    private static Properties securityProperties;
    private static boolean isInit = false;

    private SecurityUtils() { };

    public static Properties getSecurityProperties() {
        if (!isInit) {
            init();
            isInit = true;
        }

        return securityProperties;
    }

    public static String readHash(final String base64) {
        try {
            String str = new String(Base64.getDecoder().decode(base64), "UTF-8");

            String secret = getSecurityProperties().getProperty("files.docservice.secret");

            String[] payloadParts = str.split("\\?");

            String payload = getHashHex(payloadParts[1] + secret);
            if (payload.equals(payloadParts[0])) {
                return payloadParts[1];
            }
        } catch (Exception ex) { }
        return "";
    }

    public static String createHash(final String str) {
        try {
            String secret = getSecurityProperties().getProperty("files.docservice.secret");

            String payload = getHashHex(str + secret) + "?" + str;

            String base64 = Base64.getEncoder().encodeToString(payload.getBytes("UTF-8"));
            return base64;
        } catch (Exception ex) {
            throw  new RuntimeException(ex);
        }
    }

    private static String getHashHex(final String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(str.getBytes());
            String hex = new String(Hex.encodeHex(digest));

            return hex;
        } catch (Exception ex) {
            throw  new RuntimeException(ex);
        }
    }

    private static void init() {
        Properties properties = new Properties();
        try {
            InputStream inputStream = SecurityUtils.class
                    .getClassLoader()
                    .getResourceAsStream(CONFIGURATION_PATH);
            properties.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        securityProperties = properties;
    }
}
