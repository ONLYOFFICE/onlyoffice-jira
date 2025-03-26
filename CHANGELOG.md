# Change Log

## 
## Added
- core of the plugin has been moved to com.onlyoffice.docs-integration-sdk (https://github.com/ONLYOFFICE/docs-integration-sdk-java)
- link to docs cloud
- chat hidden for anonymous
- protection tab on the toolbar and the protect button in the left menu hidden for anonymous
- don't request name for anonymous user in view mode

## Changed
- com.onlyoffice:documentserver-sdk-java
    - address of the convert service, /converter instead /ConvertService.ashx
    - apache httpclient 5
    - default token lifetime is 5 minutes
    - shardkey parameter
    - default template for create new blank file
    - demo server address changed
    - extended list supported formats
- create pdf instead docxf
- remove filling for oform
- fixed work with plugin for anonymous user

## 2.3.0
## Added
- compatible with JIRA 9.9.*
- opening for editing not OOXML

## 2.1.0
## Added
- compatible with JIRA 9.*

## 2.0.0
## Added
- download as
- documents conversion
- ability to create documents
- disable certificate verification
- added connection to a demo document server
- detecting mobile browser
- go back from editor to manager
- compatible with JIRA 8.22.4

## Changed
- document server v6.0 and earlier is no longer supported

## 1.1.0
## Added
- support docxf and oform formats
- "save as" in editor

## 1.0.2
## Added
- compatible with v8.20

## 1.0.1
## Added
- compatible with Data Center

## 1.0.0
## Added
- edit option for DOCX, XLSX, PPTX
- view option for DOC, DOCM, DOT, DOTX, DOTM, ODT, FODT, OTT, RTF, TXT, HTML, HTM, MHT, PDF, DJVU, FB2, EPUB, XPS, XLS,
XLSM, XLT, XLTX, XLTM, ODS, FODS, OTS, CSV, PPS, PPSX, PPSM, PPT, PPTM, POT, POTX, POTM, ODP, FODP, OTP
- collaboration editing
- configuration page
- Translations for DE, EN, ES, FR, IT, RU