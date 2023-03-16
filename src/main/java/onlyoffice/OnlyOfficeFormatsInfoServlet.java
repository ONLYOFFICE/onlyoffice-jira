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

import onlyoffice.constants.Format;
import onlyoffice.constants.Formats;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class OnlyOfficeFormatsInfoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LogManager.getLogger("onlyoffice.OnlyOfficeFormatsInfoServlet");
    private final DocumentManager documentManager;

    @Inject
    public OnlyOfficeFormatsInfoServlet(final DocumentManager documentManager) {
        this.documentManager = documentManager;
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            List<Format> supportedFormats = Formats.getSupportedFormats();
            List<Format> enrichmentFormats = documentManager.enrichmentSupportedFormats(supportedFormats);
            JSONArray enrichmentFormatsJson = Formats.getFormatsAsJson(enrichmentFormats);
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(enrichmentFormatsJson.toString());
        } catch (JSONException e) {
            throw new IOException(e.getMessage(), e);
        }
    }
}