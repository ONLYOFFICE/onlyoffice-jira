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

package onlyoffice.servlet;

import com.atlassian.annotations.security.AnonymousSiteAccess;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.document.DocumentManager;
import org.json.JSONException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@AnonymousSiteAccess
public class OnlyOfficeFormatsInfoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ApplicationProperties applicationProperties;
    private final DocumentManager documentManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnlyOfficeFormatsInfoServlet(final ApplicationProperties applicationProperties,
                                        final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.applicationProperties = applicationProperties;
        this.documentManager = docsIntegrationSdkContext.getDocumentManager();
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> result = new HashMap<>();

            if (applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS)) {
                result.put("supportedFormats", documentManager.getFormats());
                result.put("lossyEditableMap", documentManager.getLossyEditableMap());
            } else {
                result.put("supportedFormats", new ArrayList<>());
                result.put("lossyEditableMap", new ArrayList<>());
            }

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(result));
        } catch (JSONException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
