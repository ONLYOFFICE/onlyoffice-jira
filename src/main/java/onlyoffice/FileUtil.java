/**
 *
 * (c) Copyright Ascensio System SIA 2022
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

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.Collection;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import com.atlassian.jira.issue.attachment.Attachment;

@Named
public class FileUtil {
    private static final String[] extsDocument = new String[] { ".doc", ".docx", ".docm", ".dot", 
                                                                ".dotx", ".dotm", ".docxf", ".oform",
                                                                ".odt", ".fodt", ".ott", ".rtf", ".txt", 
                                                                ".html", ".htm", ".mht", ".xml", ".pdf", 
                                                                ".djvu", ".fb2", ".epub", ".xps"};

    private static final String[] extsSpreadsheet = new String[] {  ".xls", ".xlsx", ".xlsm", ".xlt", 
                                                                    ".xltx", ".xltm", ".ods", ".fods", 
                                                                    ".ots", ".csv"};

    private static final String[] extsPresentation = new String[] { ".pps", ".ppsx", ".ppsm", ".ppt", 
                                                                    ".pptx", ".pptm", ".pot", ".potx", 
                                                                    ".potm", ".odp", ".fodp", ".otp"};

    private static final String[] extsNewDocs = new String[] {".docx", ".xlsx", ".pptx", ".docxf"};
    
    public static String getType(String fileName) {
        String ext = getExt(fileName);

        if (Arrays.asList(extsDocument).contains(ext)) {
            return "word";
        }
        if (Arrays.asList(extsSpreadsheet).contains(ext)) {
            return "cell";
        }
        if (Arrays.asList(extsPresentation).contains(ext)) {
            return "slide";
        }
        return "word";
    }

    public static String getDefaultFileName(String fileName) {
        if ("word" == getType(fileName)) {
            return "Документ";
        }
        if ("cell" == getType(fileName)) {
            return "Таблица";
        }
        if ("slide" == getType(fileName)) {
            return "Презентация";
        }
        return "Документ";
    }
    
    public static String getMimeType(String fileName) {
        String ext = getExt(fileName);

        if (Arrays.asList(extsDocument).contains(ext)) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (Arrays.asList(extsSpreadsheet).contains(ext)) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (Arrays.asList(extsPresentation).contains(ext)) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }
    
    public static void InputStreamToFile(InputStream inputStream, File file)
        throws IOException  {
    
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
    
            int read;
            byte[] bytes = new byte[1024];
    
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }

    public static String getExt(String fileName) {

        int lastIndexOf = fileName.lastIndexOf(".");

        if (lastIndexOf == -1) {
            return "";
        }

        return fileName.substring(lastIndexOf);
    }

    public static String getBaseName(String fileName) {

        int lastIndexOf = fileName.lastIndexOf(".");

        if (lastIndexOf == -1) {
            return fileName;
        }

        return fileName.substring(0, lastIndexOf);
    }

    public static String getCorrectName(String fileName, Collection<Attachment> attachments) {

        String basename = getBaseName(fileName);
        String ext = getExt(fileName);

        int count = 0;
        Boolean exist = true;

        while (exist) {
            exist = false;
            for (Attachment attachment : attachments) {
                if (attachment.getFilename().equals(fileName)) {
                    count++;
                    fileName = basename + "-" + count + ext;
                    exist = true;
                    break;
                }
            }
        }

        return fileName;
    }
}
