jQuery(function() {
    var editExt = ["docx", "xlsx", "pptx"];
    var viewExt = ["doc", "docm", "dot", "dotx", "dotm", "odt", "fodt", "ott", "rtf", "txt", "html", "htm", "mht", "pdf",
                   "djvu", "fb2", "epub", "xps", "xls", "xlsm", "xlt", "xltx", "xltm", "ods", "fods", "ots", "csv", "pps",
                   "ppsx", "ppsm", "ppt",  "pptm", "pot", "potx", "potm", "odp", "fodp", "otp"];

    var clear = function () { this.remove(); }

    function checkEditButton(el) {
        if (jQuery(el.currentTarget).find(".attachment-oo-edit").length != 0) return;

        var dropZone = jQuery("div[duitype='dndattachment/dropzones/AttachmentsDropZone']");

        var attachmentId = getAttachmentId(el);
        var attachmentTitle = getAttachmentTitle(el);
        var ext = attachmentTitle.toLowerCase().split(".").pop();
        if (editExt.indexOf(ext) != -1 && dropZone.length != 0) {
            addEditButton(el, attachmentId, true);
        } else if (viewExt.indexOf(ext) != -1 || editExt.indexOf(ext) != -1) {
            addEditButton(el, attachmentId, false);
        }
    }

    function addEditButton(el, attachmentId, edit) {
        var link = document.createElement("a");
        if (edit) {
            link.title = AJS.I18n.getText("onlyoffice.connector.editlink");
        } else {
            link.title = AJS.I18n.getText("onlyoffice.connector.viewlink");
        }
        link.setAttribute("target", "_blank");
        link.href = "/plugins/servlet/onlyoffice/doceditor?attachmentId=" + attachmentId;
        var icon = document.createElement("span");
        icon.classList.add("icon-default", "aui-icon", "aui-icon-small");
        link.appendChild(icon);

        var target = jQuery(el.currentTarget).find(".blender");
        var elementWrapper;

        if (target.length != 0) {
            elementWrapper = document.createElement("div");
        } else {
            target = jQuery(el.currentTarget).find(".attachment-title");
            elementWrapper = document.createElement("dd");
        }

        var delButton = jQuery(el.currentTarget).find(".aui-iconfont-delete");
        if (delButton.length != 0) {
            elementWrapper.classList.add("margin-delete");
            target.addClass("blender-edit");
        }

        elementWrapper.classList.add("attachment-oo-edit");
        elementWrapper.appendChild(link);
        elementWrapper.onclick = clear;
        jQuery(target).after(elementWrapper);
    }

    function getAttachmentId(el) {
        var attachmentId = el.currentTarget.getAttribute("data-attachment-id");
        if (!attachmentId) {
            var title = jQuery(el.currentTarget).find(".attachment-title");
            var href = title[0].getAttribute("href");
            attachmentId = href.replace(/.*secure\/attachment\/([^\/]*).*/,"$1");
        }
        return attachmentId;
    }

    function getAttachmentTitle(el) {
        var title = "";
        var linkTitle = jQuery(el.currentTarget).find(".attachment-title");
        if (linkTitle.length != 0) {
            title = linkTitle.text()
        }
        return title;
    }

    function init(context) {
        context.find("li.attachment-content.js-file-attachment").hover(checkEditButton);
    }

    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(e, context, reason) {
        var context = jQuery(context);
        if (context.find("#attachmentmodule").length != 0) {
            init(context);
        }
    });
});