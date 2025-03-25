/**
 *
 * (c) Copyright Ascensio System SIA 2025
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

package onlyoffice.sdk.manager.document;

import com.onlyoffice.manager.document.DefaultDocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import onlyoffice.AttachmentUtil;

import java.util.UUID;

public class DocumentManagerImpl extends DefaultDocumentManager {
    private final AttachmentUtil attachmentUtil;

    public DocumentManagerImpl(final SettingsManager settingsManager, final AttachmentUtil attachmentUtil) {
        super(settingsManager);

        this.attachmentUtil = attachmentUtil;
    }

    @Override
    public String getDocumentKey(final String fileId, final boolean embedded) {
        Long attachmentId = Long.parseLong(fileId);

        String key = attachmentUtil.getProperty(attachmentId, "onlyoffice-collaborative-editor-key");

        if (key == null || key.isEmpty()) {
            key = UUID.randomUUID().toString().replace("-", "");
            attachmentUtil.setProperty(attachmentId, "onlyoffice-collaborative-editor-key", key);
        }

        return key;
    }

    @Override
    public String getDocumentName(final String fileId) {
        Long attachmentId = Long.parseLong(fileId);

        return attachmentUtil.getFileName(attachmentId);
    }
}
