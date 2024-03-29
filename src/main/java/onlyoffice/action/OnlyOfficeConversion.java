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

package onlyoffice.action;

import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.jira.security.request.SupportedMethods;
import onlyoffice.AttachmentUtil;
import onlyoffice.ConfigurationManager;
import onlyoffice.ConversionManager;
import onlyoffice.DocumentManager;
import onlyoffice.UrlManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import webwork.action.ServletActionContext;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;

@Scanned
@SupportedMethods({RequestMethod.GET, RequestMethod.POST})
public class OnlyOfficeConversion extends AbstractIssueSelectAction {
    private final Logger log = LogManager.getLogger("onlyoffice.action.OnlyOfficeConversion");

    @JiraImport
    private final JiraAuthenticationContext jiraAuthenticationContext;

    private final DocumentManager documentManager;
    private final AttachmentUtil attachmentUtil;
    private final ConversionManager conversionManager;
    private final UrlManager urlManager;
    private final ConfigurationManager configurationManager;

    private String attachmentId;
    private String fileName;
    private String targetFileType;
    private String actionType;

    @Inject
    public OnlyOfficeConversion(final JiraAuthenticationContext jiraAuthenticationContext,
                                final DocumentManager documentManager, final AttachmentUtil attachmentUtil,
                                final ConversionManager conversionManager, final UrlManager urlManager,
                                final ConfigurationManager configurationManager) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.documentManager = documentManager;
        this.attachmentUtil = attachmentUtil;
        this.conversionManager = conversionManager;
        this.urlManager = urlManager;
        this.configurationManager = configurationManager;
    }

    @Override
    public String doDefault() throws IOException {
        ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

        if (user == null) {
            HttpServletResponse response = this.getHttpResponse();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }

        return INPUT;
    }

    @Override
    protected void doValidation() {
        HttpServletResponse response = this.getHttpResponse();

        Long attachmentId = Long.parseLong(this.attachmentId);
        String ext = attachmentUtil.getFileExt(attachmentId);

        if (!attachmentUtil.checkAccess(attachmentId, getLoggedInUser(), false)) {
            addErrorMessage(getText("onlyoffice.connector.dialog.conversion.message.error.permission"));
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (fileName == null || fileName.isEmpty()) {
            addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        if (actionType.equals("conversion")) {
            if (!attachmentUtil.checkAccess(attachmentId, getLoggedInUser(), true)) {
                addErrorMessage(getText("onlyoffice.connector.dialog.conversion.message.error.permission"));
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (conversionManager.getTargetExt(ext) == null) {
                addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }
        }

        if (actionType.equals("download-as")) {
            if (targetFileType == null || targetFileType.isEmpty()) {
                addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (
                    conversionManager.getTargetExtList(ext) == null
                            || conversionManager.getTargetExtList(ext).isEmpty()
                            || !conversionManager.getTargetExtList(ext).contains(targetFileType)
            ) {
                addErrorMessage(getText("onlyoffice.connector.error.Unknown"));
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }
        }
    }

    @Override
    public String doExecute() throws Exception {
        Long attachmentId = Long.parseLong(this.attachmentId);
        String ext = attachmentUtil.getFileExt(attachmentId);
        String targetExt = conversionManager.getTargetExt(ext);
        String url = urlManager.getFileUri(attachmentId);
        String lang = getLocale().toLanguageTag();

        if (actionType.equals("download-as") && !this.targetFileType.isEmpty()) {
            targetExt = this.targetFileType;
        }

        JSONObject convertResult =
                conversionManager.convert(attachmentId, this.fileName + "." + targetExt, ext, targetExt, url, lang,
                        true);

        if (convertResult.has("endConvert") && convertResult.getBoolean("endConvert")
                && actionType.equals("conversion")) {
            String fileName =
                    attachmentUtil.getCorrectAttachmentName(this.fileName + "." + targetExt, getIssueObject());
            String mimeType = documentManager.getMimeType(fileName);
            String fileUrl = convertResult.getString("fileUrl");
            File tempFile = Files.createTempFile(null, null).toFile();

            try (CloseableHttpClient httpClient = configurationManager.getHttpClient()) {
                HttpGet httpGet = new HttpGet(fileUrl);

                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

                    int status = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();

                    if (status == HttpStatus.SC_OK) {
                        byte[] bytes = IOUtils.toByteArray(entity.getContent());
                        InputStream inputStream = new ByteArrayInputStream(bytes);

                        FileUtils.copyInputStreamToFile(inputStream, tempFile);

                        CreateAttachmentParamsBean createAttachmentParamsBean =
                                new CreateAttachmentParamsBean.Builder(tempFile,
                                fileName, mimeType, getLoggedInUser(), getIssueObject()).build();

                        ChangeItemBean changeItemBean = attachmentManager.createAttachment(createAttachmentParamsBean);

                        convertResult.put("fileUrl",
                                "/plugins/servlet/onlyoffice/doceditor?attachmentId=" + changeItemBean.getTo());
                        convertResult.put("fileName", fileName);
                    } else {
                        log.error("Conversion service returned code " + status + ". URL: " + fileUrl);
                        convertResult.put("error", conversionManager.STATUS_NOT_OK);
                    }
                }
            } catch (Exception e) {
                throw e;
            } finally {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }

        HttpServletResponse response = ServletActionContext.getResponse();
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(convertResult.toString());
        response.setStatus(HttpServletResponse.SC_OK);
        return "none";
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public void setAttachmentId(final String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public void setTargetFileType(final String targetFileType) {
        this.targetFileType = targetFileType;
    }

    public void setActionType(final String actionType) {
        this.actionType = actionType;
    }

    public String getFileName() {
        Long attachmentId = Long.parseLong(this.attachmentId);
        String fileName = attachmentUtil.getFileName(attachmentId);

        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    public String getFileType() {
        Long attachmentId = Long.parseLong(this.attachmentId);

        return attachmentUtil.getFileExt(attachmentId);
    }

    public String getTargetFileType() {
        return conversionManager.getTargetExt(getFileType());
    }

    public List<String> getTargetFileTypeList() {
        return conversionManager.getTargetExtList(getFileType());
    }

    public boolean isConvertibleToDefault() {
        Long attachmentId = Long.parseLong(this.attachmentId);
        String targetExtList = conversionManager.getTargetExt(getFileType());

        return targetExtList != null && attachmentUtil.checkAccess(attachmentId, getLoggedInUser(), true);
    }

}
