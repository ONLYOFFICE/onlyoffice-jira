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

    private static final Map<String, Object> formats = new HashMap<String, Object>();
    static {
        Map<String, Object> format = new HashMap<String, Object>();
//document
    //"djvu"
        format.put("type", "word");
        formats.put("djvu", format);
    //"doc"
        format = new HashMap<String, Object>();
        format.put("mime", "application/msword"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("doc", format);
    //"docm"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.ms-word.document.macroEnabled.12"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("docm", format);
    //"docx"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"); format.put("type", "word"); format.put("edit", true); format.put("def", true); format.put("saveas", new String[] {"odt", "pdf", "rtf", "txt", "docxf"});
        formats.put("docx", format);
    //"docxf"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.wordprocessingml.document.docxf"); format.put("type", "word"); format.put("edit", true); format.put("def", true); format.put("createForm", true); format.put("saveas", new String[] {"odt", "pdf", "rtf", "txt"});
        formats.put("docxf", format);
    //"dot"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("dot", format);
    //"dotm"
    //"dotx"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.wordprocessingml.template"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("dotx", format);
    //"epub"
        format = new HashMap<String, Object>();
        format.put("mime", "application/epub+zip"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("epub", format);
    //"fb2"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("fb2", format);
    //"fodt"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("fodt", format);
    //"htm"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"); format.put("type", "word"); format.put("conv", true);
        formats.put("htm", format);
    //"html"
        format = new HashMap<String, Object>();
        format.put("mime", "text/html"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("html", format);
    //"mht"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("mht", format);
    //"odt"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.oasis.opendocument.text"); format.put("type", "word"); format.put("conv", true); format.put("editable", true); format.put("saveas", new String[] {"docx", "pdf", "rtf", "txt"});
        formats.put("odt", format);
    //"ott" 
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.oasis.opendocument.text-template"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("ott", format);
    //"oxps"
        format = new HashMap<String, Object>();
        format.put("type", "word");
        formats.put("oxps", format);
    //"pdf"
        format = new HashMap<String, Object>();
        format.put("mime", "application/pdf"); format.put("type", "word");
        formats.put("pdf", format);
    //"rtf"
        format = new HashMap<String, Object>();
        format.put("mime", "text/rtf"); format.put("type", "word"); format.put("conv", true); format.put("editable", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "txt"});
        formats.put("rtf", format);
    //"txt"
        format = new HashMap<String, Object>();
        format.put("mime", "text/plain"); format.put("type", "word"); format.put("edit", true); format.put("editable", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf"});
        formats.put("txt", format);
    //"xps"
        format = new HashMap<String, Object>();
        format.put("type", "word");
        formats.put("xps", format);
    //"xml"
        format = new HashMap<String, Object>();
        format.put("mime", "application/xml"); format.put("type", "word"); format.put("conv", true); format.put("saveas", new String[] {"docx", "odt", "pdf", "rtf", "txt"});
        formats.put("xml", format);
    //"oform"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.wordprocessingml.document.oform"); format.put("type", "word"); format.put("def", true); format.put("fillForms", true);
        formats.put("oform", format);

//presentation  
    //"fodp"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.presentationml.presentation"); format.put("type", "slide"); format.put("conv", true); format.put("saveas", new String[] {"pdf", "pptx", "odp"});
        formats.put("fodp", format);
    //"odp"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.oasis.opendocument.presentation"); format.put("type", "slide"); format.put("conv", true); format.put("editable", true); format.put("saveas", new String[] {"pdf", "pptx"});
        formats.put("odp", format);
    //"otp"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.oasis.opendocument.presentation-template"); format.put("type", "slide"); format.put("conv", true); format.put("saveas", new String[] {"pdf", "pptx", "odp"});
        formats.put("otp", format);
    //"pot"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.oasis.opendocument.presentation-template"); format.put("type", "slide"); format.put("conv", true); format.put("saveas", new String[] {"pdf", "pptx", "odp"});
        formats.put("pot", format);
    //"potm"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.ms-powerpoint.template.macroEnabled.12"); format.put("type", "slide"); format.put("conv", true); format.put("saveas", new String[] {"pdf", "pptx", "odp"});
        formats.put("potm", format);
    //"potx"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.presentationml.template"); format.put("type", "slide"); format.put("conv", true); format.put("saveas", new String[] {"pdf", "pptx", "odp"});
        formats.put("potx", format);
    //"pps"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.presentationml.presentation"); format.put("type", "slide"); format.put("conv", true); format.put("saveas", new String[] {"pdf", "pptx", "odp"});
        formats.put("pps", format);
    //"ppsm"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.ms-powerpoint.slideshow.macroEnabled.12"); format.put("type", "slide"); format.put("conv", true); format.put("saveas", new String[] {"pdf", "pptx", "odp"});
        formats.put("ppsm", format);
    //"ppsx"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.presentationml.slideshow"); format.put("type", "slide"); format.put("conv", true); format.put("saveas", new String[] {"pdf", "pptx", "odp"});
        formats.put("ppsx", format);
    //"ppt"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.ms-powerpoint"); format.put("type", "slide"); format.put("conv", true); format.put("saveas", new String[] {"pdf", "pptx", "odp"});
        formats.put("ppt", format);
    //"pptm"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.ms-powerpoint.presentation.macroEnabled.12"); format.put("type", "slide"); format.put("conv", true); format.put("saveas", new String[] {"pdf", "pptx", "odp"});
        formats.put("pptm", format);
    //"pptx"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.presentationml.presentation"); format.put("type", "slide"); format.put("edit", true); format.put("def", true); format.put("saveas", new String[] {"pdf", "odp"});
        formats.put("pptx", format);

//spreadsheet
    //"csv"
        format = new HashMap<String, Object>();
        format.put("mime", "text/csv"); format.put("type", "cell"); format.put("conv", true); format.put("saveas", new String[] {"ods", "pdf", "xlsx"});
        formats.put("csv", format);
    //"fods"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); format.put("type", "cell"); format.put("conv", true); format.put("editable", true); format.put("saveas", new String[] {"csv", "pdf", "xlsx"});
        formats.put("fods", format);
    //"ods"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.oasis.opendocument.spreadsheet"); format.put("type", "cell"); format.put("conv", true); format.put("editable", true); format.put("saveas", new String[] {"csv", "pdf", "xlsx"});
        formats.put("ods", format);
    //"ots"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.oasis.opendocument.spreadsheet-template"); format.put("type", "cell"); format.put("conv", true); format.put("saveas", new String[] {"csv", "ods", "pdf", "xlsx"});
        formats.put("ots", format);
    //"xls"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.ms-excel"); format.put("type", "cell"); format.put("conv", true); format.put("saveas", new String[] {"csv", "ods", "pdf", "xlsx"});
        formats.put("xls", format);
    //"xlsm"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.ms-excel.sheet.macroEnabled.12"); format.put("type", "cell"); format.put("conv", true); format.put("saveas", new String[] {"csv", "ods", "pdf", "xlsx"});
        formats.put("xlsm", format);
    //"xlsx"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); format.put("type", "cell"); format.put("edit", true); format.put("def", true); format.put("saveas", new String[] {"csv", "ods", "pdf"});
        formats.put("xlsx", format);
    //"xlt"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); format.put("type", "cell"); format.put("conv", true); format.put("saveas", new String[] {"csv", "ods", "pdf", "xlsx"});
        formats.put("xlt", format);
    //"xltm"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.ms-excel.template.macroEnabled.12"); format.put("type", "cell"); format.put("conv", true); format.put("saveas", new String[] {"csv", "ods", "pdf", "xlsx"});
        formats.put("xltm", format);
    //"xltx"
        format = new HashMap<String, Object>();
        format.put("mime", "application/vnd.openxmlformats-officedocument.spreadsheetml.template"); format.put("type", "cell"); format.put("conv", true); format.put("saveas", new String[] {"csv", "ods", "pdf", "xlsx"});
        formats.put("xltx", format);
    }

    private static final String[] extsNewDocs = new String[] {".docx", ".xlsx", ".pptx", ".docxf"};
    
    public static String getType(String fileName) {
        String ext = getExt(fileName);

        if (formats.containsKey(ext)) {
            return (String)((Map)formats.get(ext)).get("type");
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

        if (formats.containsKey(ext)) {
            if ((String)((Map)formats.get(ext)).get("mime") != null){
                return (String)((Map)formats.get(ext)).get("mime");
            }
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
