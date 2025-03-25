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

package onlyoffice;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.service.documenteditor.callback.CallbackService;
import onlyoffice.sdk.manager.security.JwtManager;
import onlyoffice.utils.ParsingUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class OnlyOfficeSaveFileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final Logger log = LogManager.getLogger(this.getClass());

    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final UserManager userManager;
    private final AttachmentUtil attachmentUtil;

    private final JwtManager jwtManager;
    private final SettingsManager settingsManager;
    private final CallbackService callbackService;

    public OnlyOfficeSaveFileServlet(final JiraAuthenticationContext jiraAuthenticationContext,
                                     final UserManager userManager, final AttachmentUtil attachmentUtil,
                                     final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.userManager = userManager;
        this.attachmentUtil = attachmentUtil;

        this.jwtManager = (JwtManager) docsIntegrationSdkContext.getJwtManager();
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

            if (token == null || token.isEmpty()) {
                throw new SecurityException("Expected JWT");
            }

            try {
                String payload = jwtManager.verify(token);
            } catch (Exception e) {
                throw new SecurityException("JWT verification failed!");
            }
        }

        String token = request.getParameter("token");
        String payload;
        JSONObject bodyFromToken;

        try {
            payload = jwtManager.verifyInternalToken(token);
            bodyFromToken = new JSONObject(payload);

            if (!bodyFromToken.getString("action").equals("download")) {
                throw new SecurityException();
            }
        } catch (Exception e) {
            throw new SecurityException("Invalid link token!");
        }

        String userKey = bodyFromToken.has("userKey") ? bodyFromToken.getString("userKey") : null;
        String attachmentIdString = bodyFromToken.getString("attachmentId");
        Long attachmentId = Long.parseLong(attachmentIdString);
        ApplicationUser user = userManager.getUserByKey(userKey);

        if (attachmentUtil.getAttachment(attachmentId) == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!attachmentUtil.checkAccess(attachmentId, user, false)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

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

        String token = request.getParameter("token");
        String payload;
        JSONObject bodyFromToken;

        try {
            payload = jwtManager.verifyInternalToken(token);
            bodyFromToken = new JSONObject(payload);

            if (!bodyFromToken.getString("action").equals("download")) {
                throw new SecurityException();
            }
        } catch (Exception e) {
            throw new SecurityException("Invalid link token!");
        }

        String userKey = bodyFromToken.has("userKey") ? bodyFromToken.getString("userKey") : null;
        String attachmentIdString = bodyFromToken.getString("attachmentId");
        Long attachmentId = Long.parseLong(attachmentIdString);
        ApplicationUser user = userManager.getUserByKey(userKey);

        jiraAuthenticationContext.setLoggedInUser(user);

        if (attachmentUtil.getAttachment(attachmentId) == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!attachmentUtil.checkAccess(attachmentId, user, true)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

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
