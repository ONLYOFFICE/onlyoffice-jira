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

package onlyoffice.sdk.manager.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.onlyoffice.manager.security.DefaultJwtManager;
import com.onlyoffice.manager.settings.SettingsManager;

import java.util.Map;
import java.util.Random;

public class JwtManagerImpl extends DefaultJwtManager implements JwtManager {
    private static final int PLUGIN_SECRET_LENGTH = 32;

    public JwtManagerImpl(final SettingsManager settingsManager) {
        super(settingsManager);
    }

    public String createInternalToken(final Map<String, ?> payloadMap) {
        Algorithm algorithm = Algorithm.HMAC256(getPluginSecret());

        return JWT.create()
                .withPayload(payloadMap)
                .sign(algorithm);
    }

    public String verifyInternalToken(final String token) {
        return verifyToken(token, getPluginSecret());
    }

    private String getPluginSecret() {
        if (getSettingsManager().getSetting("plugin-secret") == null
                || getSettingsManager().getSetting("plugin-secret").isEmpty()) {
            Random random = new Random();
            char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();

            char[] randBuffer = new char[PLUGIN_SECRET_LENGTH];
            for (int i = 0; i < randBuffer.length; i++) {
                randBuffer[i] = numbersAndLetters[random.nextInt(numbersAndLetters.length)];
            }

            String secret = new String(randBuffer);

            getSettingsManager().setSetting("plugin-secret", secret);

            return secret;
        } else {
            return (String) getSettingsManager().getSetting("plugin-secret");
        }
    }
}
