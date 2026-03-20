# ONLYOFFICE app for Jira

This app enables users to view, edit, and collaborate on office documents directly within [Jira Software](https://www.atlassian.com/software/jira) using [ONLYOFFICE Docs](https://www.onlyoffice.com/docs).

<p align="center">
  <a href="https://www.onlyoffice.com/office-for-jira">
    <img width="800" src="https://static-site.onlyoffice.com/public/images/templates/office-for-jira/hero/hero@2x.png" alt="ONLYOFFICE Docs for Jira">
  </a>
</p>

## Features ✨

- Create, view, and edit text **documents**, **spreadsheets**, **presentations**, and **PDFs** directly in Jira.
- Collaborate in real time using:
  - Two co-editing modes (**Fast** and **Strict**)
  - **Track Changes**, version history, and comments
  - Built-in **chat 💬** for discussions within documents
- **JWT (JSON Web Token)** support to secure your traffic — ensuring that only authorized users have access to your documents.

### Supported formats 📁

**For viewing:**

* **WORD**: DOC, DOCM, DOCX, DOT, DOTM, DOTX, EPUB, FB2, FODT, HTM, HTML, HWP, HWPX, MD, MHT, MHTML, ODT, OTT, PAGES, RTF, STW, SXW, TXT, WPS, WPT, XML
* **CELL**: CSV, ET, ETT, FODS, NUMBERS, ODS, OTS, SXC, XLS, XLSM, XLSX, XLT, XLTM, XLTX
* **SLIDE**: DPS, DPT, FODP, KEY, ODG, ODP, OTP, POT, POTM, POTX, PPS, PPSM, PPSX, PPT, PPTM, PPTX, SXI
* **PDF**: DJVU, DOCXF, OFORM, OXPS, PDF, XPS
* **DIAGRAM**: VSDM, VSDX, VSSM, VSSX, VSTM, VSTX

**For editing:**

* **WORD**: DOCM, DOCX, DOTM, DOTX
* **CELL**: XLSB, XLSM, XLSX, XLTM, XLTX
* **SLIDE**: POTM, POTX, PPSM, PPSX, PPTM, PPTX
* **PDF**: PDF

**For editing with possible loss of information:**

* **WORD**: EPUB, FB2, HTML, ODT, OTT, RTF, TXT
* **CELL**: CSV, ODS, OTS
* **SLIDE**: ODP, OTP

## Installing ONLYOFFICE Docs

This integration requires a running instance of **ONLYOFFICE Docs (Document Server)** accessible to both your **Jira instance** and **client browsers**. The server must also be able to send **POST callbacks** to Jira for document status updates.

You can deploy either version below:

### 🖥️ Self-hosted version

- **Community Edition (Free):** [Docker guide](https://github.com/onlyoffice/Docker-DocumentServer) or [manual installation](https://helpcenter.onlyoffice.com/docs/installation/docs-community-install-ubuntu.aspx)
- **Enterprise Edition:** [Installation guide](https://helpcenter.onlyoffice.com/docs/installation/enterprise)

Community Edition vs Enterprise Edition comparison can be found [here](#onlyoffice-docs-editions).

### ☁️ Cloud option

If you prefer not to host and maintain your own server, use **ONLYOFFICE Docs Cloud**, which requires neither installation nor configuration.

👉 [Get started here](https://www.onlyoffice.com/docs-registration)

## Installing ONLYOFFICE app for Jira

Upload the compiled ***target/onlyoffice-jira-app.jar*** to Jira on the `Manage apps` page.

The latest compiled package files are available [here](https://github.com/ONLYOFFICE/onlyoffice-jira/releases) and on [Atlassian Marketplace](https://marketplace.atlassian.com/apps/1226616/onlyoffice-connector-for-jira).

You can also install the app from the Jira administration panel:

1. Navigate to the `Manage apps` page.
2. Click **Find new apps** on the left-side panel.
3. Locate **ONLYOFFICE Connector for Jira** using search.
4. Click **Install** to download and install the app.

## Configuring ONLYOFFICE app for Jira ⚙️

Find the uploaded app on the `Manage apps` page. Click `Configure` and enter the name of the server with ONLYOFFICE Docs installed:
```
http://documentserver/
```

Configuration settings include JWT, enabled by default to protect the editors from unauthorized access. If setting a custom **Secret key**, ensure it matches the one in the ONLYOFFICE Docs [config file](https://api.onlyoffice.com/docs/docs-api/additional-api/signature/) for proper validation.

Sometimes your network configuration might not allow the requests between Jira and ONLYOFFICE Docs using the public addresses. The **Advanced server settings** section allows you to set the ONLYOFFICE Docs address for internal requests from Jira and the returning Jira address for internal requests from ONLYOFFICE Docs.

## Compiling ONLYOFFICE app for Jira

You will need:

* 1.8.X of the Oracle Java SE Development Kit 8

* Atlassian Plugin SDK

* Compile package:
  ```bash
  atlas-package
  ```

## Using ONLYOFFICE app for Jira

Once installed, you can **view**, **edit**, and **co-author** office documents attached to Jira tasks — without leaving your dashboard.

To edit a document:

- Open a Jira task with a document attachment.
- Click the **ONLYOFFICE Docs** icon next to the file name.
- The editor will open in a new browser tab.

When editing ends:

- The updated file is saved as a **new attachment** with the same name and a postfix.
- If multiple users co-edit, the document is updated after **the last user closes** the editor.

## How it works

The ONLYOFFICE app follows the API documented [here](https://api.onlyoffice.com/docs/docs-api/get-started/basic-concepts/):

* User navigates to the Jira attachments and selects the `Edit in ONLYOFFICE` action.
* Jira makes a request to OnlyOfficeEditorServlet (URL of the form: `/plugins/servlet/onlyoffice/doceditor?attachmentId=$attachment.id`).
* Jira sends the document to ONLYOFFICE Document storage service and receives a temporary link.
* Jira prepares a JSON object with the following properties:
  * **url**: the temporary link that ONLYOFFICE Document Server uses to download the document,
  * **callbackUrl**: the URL that ONLYOFFICE Document Server informs about status of the document editing,
  * **docserviceApiUrl**: the URL that the client needs to reply to ONLYOFFICE Document Server (provided by the files.docservice.url.api property),
  * **key**: the UUID to instruct ONLYOFFICE Document Server whether to download the document again or not,
  * **title**: the document Title (name).
* Jira takes this object and constructs a page from a freemarker template, filling in all of those values so that the client browser can load up the editor.
* The client browser makes a request for the javascript library from ONLYOFFICE Document Server and sends ONLYOFFICE Document Server the docEditor configuration with the above properties.
* Then ONLYOFFICE Document Server downloads the document from document storage and the user begins editing.
* When all users and client browsers are done with editing, they close the editing window.
* After 10 seconds of inactivity, ONLYOFFICE Document Server sends a POST to the `callback` URL letting Jira know that the clients have finished editing the document and closed it.
* The document with all the changes is saved as a new attachment with the postfix added to the file name.

## ONLYOFFICE Docs editions

ONLYOFFICE offers different versions of its online document editors that can be deployed on your own servers.

* Community Edition 🆓 (`onlyoffice-documentserver` package)
* Enterprise Edition 🏢 (`onlyoffice-documentserver-ee` package)

The table below will help you to make the right choice.

| Pricing and licensing | Community Edition | Enterprise Edition |
| ------------- | ------------- | ------------- |
| | [Get it now](https://www.onlyoffice.com/download-community?utm_source=github&utm_medium=cpc&utm_campaign=GitHubJira#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download?utm_source=github&utm_medium=cpc&utm_campaign=GitHubJira#docs-enterprise)  |
| Cost  | FREE  | [Go to the pricing page](https://www.onlyoffice.com/docs-enterprise-prices?utm_source=github&utm_medium=cpc&utm_campaign=GitHubJira)  |
| Simultaneous connections | up to 20 maximum  | As in chosen pricing plan |
| Number of users | up to 20 recommended | As in chosen pricing plan |
| License | GNU AGPL v.3 | Proprietary |
| **Support** | **Community Edition** | **Enterprise Edition** |
| Documentation | [Help Center](https://helpcenter.onlyoffice.com/docs/installation/community) | [Help Center](https://helpcenter.onlyoffice.com/docs/installation/enterprise) |
| Standard support | [GitHub](https://github.com/ONLYOFFICE/DocumentServer/issues) or paid | 1 or 3 years support included |
| Premium support | [Contact us](mailto:sales@onlyoffice.com) | [Contact us](mailto:sales@onlyoffice.com) |
| **Services** | **Community Edition** | **Enterprise Edition** |
| Conversion Service                | + | + |
| Document Builder Service          | + | + |
| **Interface** | **Community Edition** | **Enterprise Edition** |
| Tabbed interface                  | + | + |
| Dark theme                        | + | + |
| 125%, 150%, 175%, 200% scaling    | + | + |
| White Label                       | - | - |
| Integrated test example (node.js) | + | + |
| Mobile web editors                | - | +* |
| **Plugins & Macros** | **Community Edition** | **Enterprise Edition** |
| Plugins                           | + | + |
| Macros                            | + | + |
| **Collaborative capabilities** | **Community Edition** | **Enterprise Edition** |
| Two co-editing modes              | + | + |
| Comments                          | + | + |
| Built-in chat                     | + | + |
| Review and tracking changes       | + | + |
| Display modes of tracking changes | + | + |
| Version history                   | + | + |
| **Document Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Adding Content control          | + | + |
| Editing Content control         | + | + |
| Layout tools                    | + | + |
| Table of contents               | + | + |
| Navigation panel                | + | + |
| Mail Merge                      | + | + |
| Comparing Documents             | + | + |
| **Spreadsheet Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Functions, formulas, equations  | + | + |
| Table templates                 | + | + |
| Pivot tables                    | + | + |
| Data validation                 | + | + |
| Conditional formatting          | + | + |
| Sparklines                      | + | + |
| Sheet Views                     | + | + |
| **Presentation Editor features** | **Community Edition** | **Enterprise Edition** |
| Font and paragraph formatting   | + | + |
| Object insertion                | + | + |
| Transitions                     | + | + |
| Animations                      | + | + |
| Presenter mode                  | + | + |
| Notes                           | + | + |
| **Form creator features** | **Community Edition** | **Enterprise Edition** |
| Adding form fields              | + | + |
| Form preview                    | + | + |
| Saving as PDF                   | + | + |
| **PDF Editor features**      | **Community Edition** | **Enterprise Edition** |
| Text editing and co-editing                                | + | + |
| Work with pages (adding, deleting, rotating)               | + | + |
| Inserting objects (shapes, images, hyperlinks, etc.)       | + | + |
| Text annotations (highlight, underline, cross out, stamps) | + | + |
| Comments                        | + | + |
| Freehand drawings               | + | + |
| Form filling                    | + | + |
| | [Get it now](https://www.onlyoffice.com/download-community?utm_source=github&utm_medium=cpc&utm_campaign=GitHubJira#docs-community)  | [Start Free Trial](https://www.onlyoffice.com/download?utm_source=github&utm_medium=cpc&utm_campaign=GitHubJira#docs-enterprise)  |

\* If supported by DMS.

## Need help? User Feedback and Support 💡

* **🐞 Found a bug?** Please report it by creating an [issue](https://github.com/ONLYOFFICE/onlyoffice-jira/issues).
* **❓ Have a question?** Ask our community and developers on the [ONLYOFFICE Forum](https://community.onlyoffice.com).
* **👨‍💻 Need help for developers?** Check our [API documentation](https://api.onlyoffice.com).
* **💡 Want to suggest a feature?** Share your ideas on our [feedback platform](https://feedback.onlyoffice.com/forums/966080-your-voice-matters).