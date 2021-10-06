/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.atlassian.jira.util.io.InputStreamConsumer;

import org.apache.commons.io.IOUtils;

public class DownloadFileStreamConsumer implements InputStreamConsumer<Object> {

    private final OutputStream out;

    public DownloadFileStreamConsumer(OutputStream out) {
        this.out = out;
    }

    @Override
    public Object withInputStream(InputStream in) throws IOException {
        IOUtils.copy(in, out);
        return null;
    }
    
}
