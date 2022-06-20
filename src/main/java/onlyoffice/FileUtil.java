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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.atlassian.jira.issue.attachment.Attachment;

@Named
public class FileUtil {

    private static final Map<String, Format> formats = new HashMap<String, Format>(){{
        put("djvu",Format.DJYU);
        put("doc",Format.DOC);
        put("docm",Format.DOCM);
        put("docx",Format.DOCX);
        put("docxf",Format.DOCXF);
        put("dot",Format.DOT);
        put("dotx",Format.DOTX);
        put("epub",Format.EPUB);
        put("fb2",Format.FB2);
        put("htm",Format.HTM);
        put("html",Format.HTML);
        put("mht",Format.MHT);
        put("odt",Format.ODT);
        put("ott",Format.OTT);
        put("oxps",Format.OXPS);
        put("pdf",Format.PDF);
        put("rtf",Format.RFT);
        put("txt",Format.TXT);
        put("xps",Format.XPS);
        put("oform",Format.OFORM);

        put("csv",Format.CSV);
        put("fods",Format.FODS);
        put("ods",Format.ODS);
        put("ots",Format.OTS);
        put("xls",Format.XLS);
        put("xlsm",Format.XLSM);
        put("xlsx",Format.XLSX);
        put("xlt",Format.XLT);
        put("xltm",Format.XLTM);
        put("xltx",Format.XLTX);

        put("fodp",Format.FODP);
        put("odp",Format.ODP);
        put("otp",Format.OTP);
        put("pot",Format.POT);
        put("potm",Format.POTM);
        put("potx",Format.POTX);
        put("pps",Format.PPS);
        put("ppsm",Format.PPSM);
        put("ppsx",Format.PPSX);
        put("ppt",Format.PPT);
        put("pptm",Format.PPTM);
        put("pptx",Format.PPTX);
    }};

    private static final String[] extsNewDocs = new String[] {".docx", ".xlsx", ".pptx", ".docxf"};

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

    public static String getType(String fileName) {
        String ext = getExt(fileName);

        if (formats.containsKey(ext)) {
            return formats.get(ext).getType();
        }
        return "word";
    }
    
    public static String getMimeType(String fileName) {
        String ext = getExt(fileName);

        if (formats.containsKey(ext)) {
            return formats.get(ext).getMime();
        }
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }
    
    public static String getExt(String fileName) {

        int lastIndexOf = fileName.lastIndexOf(".") + 1;

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
                    fileName = basename + "-" + count + "." + ext;
                    exist = true;
                    break;
                }
            }
        }

        return fileName;
    }
}
