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

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import onlyoffice.utils.ParsingUtils;
import onlyoffice.utils.SecurityUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

@Scanned
public class OnlyOfficeSaveFileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeSaveFileServlet");

    private final AttachmentUtil attachmentUtil;

    private final JwtManager jwtManager;
    private final SettingsManager settingsManager;
    private final CallbackService callbackService;

    @Inject
    public OnlyOfficeSaveFileServlet(final AttachmentUtil attachmentUtil,
                                     final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.attachmentUtil = attachmentUtil;

        this.jwtManager = docsIntegrationSdkContext.getJwtManager();
        this.settingsManager = docsIntegrationSdkContext.getSettingsManager();
        this.callbackService = docsIntegrationSdkContext.getCallbackService();
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (settingsManager.isSecurityEnabled()) {
            String jwth = settingsManager.getSecurityHeader();
            String header = (String) request.getHeader(jwth);
            String authorizationPrefix = settingsManager.getSecurityPrefix();
            String token = (header != null && header.startsWith(authorizationPrefix))
                    ? header.substring(authorizationPrefix.length()) : header;

            if (token == null || token == "") {
                throw new SecurityException("Expected JWT");
            }

            try {
                String payload = jwtManager.verify(token);
            } catch (Exception e) {
                throw new SecurityException("JWT verification failed!");
            }
        }

        String vkey = request.getParameter("vkey");
        log.info("vkey = " + vkey);
        String attachmentIdString = SecurityUtils.readHash(vkey);

        Long attachmentId = Long.parseLong(attachmentIdString);
        log.info("attachmentId " + attachmentId);

        String contentType = attachmentUtil.getMediaType(attachmentId);
        response.setContentType(contentType);

        response.setContentLength(attachmentUtil.getFilesize(attachmentId).intValue());

        OutputStream output = response.getOutputStream();
        attachmentUtil.getAttachmentData(new DownloadFileStreamConsumer(output), attachmentId);
        output.close();
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/plain; charset=utf-8");

        String vkey = request.getParameter("vkey");
        String attachmentIdString = SecurityUtils.readHash(vkey);

        String error = "";
        try {
            InputStream requestStream = request.getInputStream();

            String bodyString = ParsingUtils.getBody(requestStream);

            if (bodyString.isEmpty()) {
                throw new IllegalArgumentException("requestBody is empty");
            }

            ObjectMapper mapper = new ObjectMapper();
            Callback callback = mapper.readValue(bodyString, Callback.class);

            String authorizationHeader = request.getHeader(settingsManager.getSecurityHeader());
            callback = callbackService.verifyCallback(callback, authorizationHeader);

            callbackService.processCallback(callback, attachmentIdString);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            error = e.getMessage();
        }

        PrintWriter writer = response.getWriter();
        if (error.isEmpty()) {
            writer.write("{\"error\":0}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writer.write("{\"error\":1,\"message\":\"" + error + "\"}");
        }
    }
}
