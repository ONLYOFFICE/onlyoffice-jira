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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.context.DocsIntegrationSdkContext;
import com.onlyoffice.manager.document.DocumentManager;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class OnlyOfficeFormatsInfoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeFormatsInfoServlet");

    private final DocumentManager documentManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OnlyOfficeFormatsInfoServlet(final DocsIntegrationSdkContext docsIntegrationSdkContext) {
        this.documentManager = docsIntegrationSdkContext.getDocumentManager();
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            Map<String, Object> result = new HashMap<>();

            result.put("supportedFormats", documentManager.getFormats());
            result.put("lossyEditableMap", documentManager.getLossyEditableMap());

            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(result));
        } catch (JSONException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}
