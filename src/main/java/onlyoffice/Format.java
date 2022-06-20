package onlyoffice;


public enum Format {
    DJYU("djvu","application/vnd.openxmlformats-officedocument.wordprocessingml.document",DocumentType.WORD,
        false, false, false, false, false, new String[] {}),
    DOC("doc","application/msword",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    DOCM("docm","application/vnd.ms-word.document.macroEnabled.12",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    DOCX("docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",DocumentType.WORD,
        false, true, true, false, false, new String[] {"odt", "pdf", "rtf", "txt", "docxf"}),
    DOCXF("docxf","application/vnd.openxmlformats-officedocument.wordprocessingml.document.docxf",DocumentType.WORD,
        false, true, true, true, false, new String[] {"odt", "pdf", "rtf", "txt"}),
    DOT("dot","application/vnd.openxmlformats-officedocument.wordprocessingml.document",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    DOTX("dotx","application/vnd.openxmlformats-officedocument.wordprocessingml.template",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    EPUB("epub","application/epub+zip",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    FB2("fb2","application/vnd.openxmlformats-officedocument.wordprocessingml.document",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    FODT("fodt","application/vnd.openxmlformats-officedocument.wordprocessingml.document",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    HTM("htm","application/vnd.openxmlformats-officedocument.wordprocessingml.document",DocumentType.WORD,
        true, false, false, false, false, new String[] {}),
    HTML("html","text/html",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    MHT("mht","text/plain",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    ODT("odt","application/vnd.oasis.opendocument.text",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "pdf", "rtf", "txt"}),
    OTT("ott","application/vnd.oasis.opendocument.text-template",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    OXPS("oxps","application/oxps",DocumentType.WORD,
        true, false, false, false, false, new String[] {}),
    PDF("pdf","application/pdf",DocumentType.WORD,
        true, false, false, false, false, new String[] {}),
    RFT("rtf","text/rtf",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "txt"}),
    TXT("txt","text/plain",DocumentType.WORD,
        true, true, false, false, false, new String[] {"docx", "odt", "pdf", "rtf"}),
    XPS("xps","application/vnd.ms-xpsdocument",DocumentType.WORD,
        false, false, false, false, false, new String[] {}),
    XML("xml","application/xml",DocumentType.WORD,
        true, false, false, false, false, new String[] {"docx", "odt", "pdf", "rtf", "txt"}),
    OFORM("oform","application/vnd.openxmlformats-officedocument.wordprocessingml.document.oform",DocumentType.WORD,
        true, false, true, false, true, new String[] {}),

    CSV("csv","text/csv",DocumentType.CELL,
        true, false, false, false, false, new String[] {"ods", "pdf", "xlsx"}),
    FODS("fods","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",DocumentType.CELL,
        true, false, false, false, false, new String[] {"csv", "pdf", "xlsx"}),
    ODS("ods","application/vnd.oasis.opendocument.spreadsheet",DocumentType.CELL,
        true, false, false, false, false, new String[] {"csv", "pdf", "xlsx"}),
    OTS("ots","application/vnd.oasis.opendocument.spreadsheet-template",DocumentType.CELL,
        true, false, false, false, false, new String[] {"csv", "ods", "pdf", "xlsx"}),
    XLS("xls","application/vnd.ms-excel",DocumentType.CELL,
        true, false, false, false, false, new String[] {"csv", "ods", "pdf", "xlsx"}),
    XLSM("xlsm","application/vnd.ms-excel.sheet.macroEnabled.12",DocumentType.CELL,
        true, false, false, false, false, new String[] {"csv", "ods", "pdf", "xlsx"}),
    XLSX("xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",DocumentType.CELL,
        false, true, true, false, false, new String[] {"csv", "ods", "pdf"}),
    XLT("xlt","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",DocumentType.CELL,
        true, false, false, false, false, new String[] {"csv", "ods", "pdf", "xlsx"}),
    XLTM("xltm","application/vnd.ms-excel.template.macroEnabled.12",DocumentType.CELL,
        true, false, false, false, false, new String[] {"csv", "ods", "pdf", "xlsx"}),
    XLTX("xltx","application/vnd.openxmlformats-officedocument.spreadsheetml.template",DocumentType.CELL,
        true, false, false, false, false, new String[] {"csv", "ods", "pdf", "xlsx"}),

    FODP("fodp","application/vnd.openxmlformats-officedocument.presentationml.presentation",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx", "odp"}),
    ODP("odp","application/vnd.oasis.opendocument.presentation",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx"}),
    OTP("otp","application/vnd.oasis.opendocument.presentation-template",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx", "odp"}),
    POT("pot","application/vnd.oasis.opendocument.presentation-template",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx", "odp"}),
    POTM("potm","application/vnd.ms-powerpoint.template.macroEnabled.12",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx", "odp"}),
    POTX("potx","application/vnd.openxmlformats-officedocument.presentationml.template",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx", "odp"}),
    PPS("pps","application/vnd.openxmlformats-officedocument.presentationml.presentation",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx", "odp"}),
    PPSM("ppsm","application/vnd.ms-powerpoint.slideshow.macroEnabled.12",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx", "odp"}),
    PPSX("ppsx","application/vnd.openxmlformats-officedocument.presentationml.slideshow",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx", "odp"}),
    PPT("ppt","application/vnd.ms-powerpoint",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx", "odp"}),
    PPTM("pptm","application/vnd.ms-powerpoint.presentation.macroEnabled.12",DocumentType.SLIDE,
        true, false, false, false, false, new String[] {"pdf", "pptx", "odp"}),
    PPTX("pptx","application/vnd.openxmlformats-officedocument.presentationml.presentation",DocumentType.SLIDE,
        false, true, true, false, false, new String[] {"pdf", "odp"});

    Format(    String extension, String mime, DocumentType type, 
                Boolean conv, Boolean edit, Boolean def, Boolean createForm, Boolean fillForms,
                String[] saveas) {
        this.extension = extension;
        this.mime = mime;
        this.type = type;
        this.conv = conv;
        this.edit = edit;
        this.def = def;
        this.createForm = createForm;
        this.fillForms = fillForms;
        this.saveas = saveas;
    }
    private String extension;
    private String mime;
    private DocumentType type;
    private Boolean conv;
    private Boolean edit;
    private Boolean def;
    private Boolean createForm;
    private Boolean fillForms;
    private String[] saveas;

    public String getType() {
        return type.getType();
    }
    public String getMime() {
        return mime;
    }

    public Boolean isConv() {
        return conv;
    }
    public Boolean isEdit() {
        return edit;
    }
    public Boolean isCreateForm() {
        return createForm;
    }
    public Boolean isFillForms() {
        return fillForms;
    }

    public String[] getSaveas() {
        if (saveas.length > 0) return saveas;
        return null;
    }
}