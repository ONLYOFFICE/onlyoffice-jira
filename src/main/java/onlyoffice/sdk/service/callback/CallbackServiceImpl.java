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

package onlyoffice.sdk.service.callback;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.convertservice.ConvertRequest;
import com.onlyoffice.model.convertservice.ConvertResponse;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.service.convert.ConvertService;
import com.onlyoffice.service.documenteditor.callback.DefaultCallbackService;
import onlyoffice.AttachmentUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public class CallbackServiceImpl extends DefaultCallbackService {
    private final AttachmentUtil attachmentUtil;
    private final DocumentServerClient documentServerClient;
    private final DocumentManager documentManager;
    private final ConvertService convertService;

    public CallbackServiceImpl(final JwtManager jwtManager, final SettingsManager settingsManager,
                               final AttachmentUtil attachmentUtil, final DocumentServerClient documentServerClient,
                               final DocumentManager documentManager, final ConvertService convertService) {
        super(jwtManager, settingsManager);

        this.attachmentUtil = attachmentUtil;
        this.documentServerClient = documentServerClient;
        this.documentManager = documentManager;
        this.convertService = convertService;
    }

    @Override
    public void handlerSave(final Callback callback, final String fileId) throws Exception {
        String url = callback.getUrl();
        String fileType = callback.getFiletype();
        Long attachmentId = Long.parseLong(fileId);
        Attachment attachment = attachmentUtil.getAttachment(attachmentId);
        Issue issue = attachment.getIssue();
        String currentFileName = documentManager.getDocumentName(fileId);
        String currentFileType = documentManager.getExtension(currentFileName);
        String newFileName = attachmentUtil.getNewAttachmentFileName(currentFileName, issue);

        if (!currentFileType.equals(fileType)) {
            ConvertRequest convertRequest = ConvertRequest.builder()
                    .outputtype(fileType)
                    .url(url)
                    .build();

            ConvertResponse convertResponse = convertService.processConvert(convertRequest, fileId);
            url = convertResponse.getFileUrl();
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(null, null);

            documentServerClient.getFile(
                    url,
                    Files.newOutputStream(tempFile)
            );

            attachmentUtil.createNewAttachment(newFileName, tempFile.toFile(), issue);
            attachmentUtil.removeProperty(attachmentId, "onlyoffice-collaborative-editor-key");
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }
}
