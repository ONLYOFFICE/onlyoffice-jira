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

jQuery(function() {
    var editExt = ["docx", "xlsx", "pptx", "docxf"];
    var fillFormExt = ["oform"];
    var viewExt = ["doc", "docm", "dot", "dotx", "dotm", "odt", "fodt", "ott", "rtf", "txt", "html", "htm", "mht", "pdf",
                   "djvu", "fb2", "epub", "xps", "xls", "xlsm", "xlt", "xltx", "xltm", "ods", "fods", "ots", "csv", "pps",
                   "ppsx", "ppsm", "ppt",  "pptm", "pot", "potx", "potm", "odp", "fodp", "otp"];
    var convExt = [ "doc", "docm", "docx", "docxf", "dot", "dotm", "dotx", "epub", "fb2", "fodt", "html",
                    "mht", "odt", "ott", "rtf", "txt", "xml",
                    "fodp", "odp", "otp", "pot", "potm", "potx", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx",
                    "csv", "fods", "ods", "ots", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx"];

    var clear = function () { this.remove(); }

    function checkEditButton(el) {
        if (jQuery(el.currentTarget).find(".attachment-oo-edit").length != 0) return;

        var dropZone = jQuery("div[duitype='dndattachment/dropzones/AttachmentsDropZone']");

        var attachmentId = getAttachmentId(el);
        var attachmentTitle = getAttachmentTitle(el);
        var ext = attachmentTitle.toLowerCase().split(".").pop();
        if (editExt.indexOf(ext) != -1 && dropZone.length != 0) {
            crateButton(el, attachmentId, true, false, "edit");
        } else if (fillFormExt.indexOf(ext) != -1 && dropZone.length != 0){
            crateButton(el, attachmentId, true, true, "edit");
        } else if (viewExt.indexOf(ext) != -1 || editExt.indexOf(ext) != -1 || fillFormExt.indexOf(ext) != -1) {
            crateButton(el, attachmentId, false, false, "edit");
        }

        if (convExt.indexOf(ext) != -1){
            crateButton(el, attachmentId, NaN, NaN, "downloadAs");
        }
    }

    function crateButton(el, attachmentId, edit, form, type) {

        if (type === "edit") var link = addEditLink(attachmentId, edit, form);
        if (type === "downloadAs") var link = addDownloadAsLink(attachmentId);

        var target = jQuery(el.currentTarget).find(".blender");
        var elementWrapper;

        if (target.length != 0) {
            elementWrapper = document.createElement("div");
        } else {
            target = jQuery(el.currentTarget).find(".attachment-title");
            elementWrapper = document.createElement("dd");
        }

        if (type === "edit") var nearButton = jQuery(el.currentTarget).find(".aui-iconfont-delete");
        if (type === "downloadAs") var nearButton = jQuery(el.currentTarget).find(".attachment-oo-edit");
        if (nearButton.length != 0) {
            elementWrapper.classList.add("margin-delete");
            target.addClass("blender-" + type);
        }

        elementWrapper.classList.add("attachment-oo-" + type);
        elementWrapper.appendChild(link);
        if (type === "edit") elementWrapper.onclick = clear;
        if (type === "downloadAs") elementWrapper.onclick = function () {
            if ($("#onlioffice-dialog-conversion").length == 0){
                var attachmentTitle = getAttachmentTitle(el);
                var ext = attachmentTitle.toLowerCase().split(".").pop();
                var dialog = createDialogDownloadAs(el, attachmentTitle, attachmentId, ext);
                getOptions(ext);
                dialog.show();
            }
        };
        jQuery(target).after(elementWrapper);
    }

    function addEditLink(attachmentId, edit, form) {
        var link = document.createElement("a");
        if (edit) {
            if (form) {
                link.title = AJS.I18n.getText("onlyoffice.connector.fillFormlink");
            } else {
                link.title = AJS.I18n.getText("onlyoffice.connector.editlink");
            }
        } else {
            link.title = AJS.I18n.getText("onlyoffice.connector.viewlink");
        }
        link.setAttribute("target", "_blank");
        link.href = "/plugins/servlet/onlyoffice/doceditor?attachmentId=" + attachmentId;
        var icon = document.createElement("span");
        icon.classList.add("icon-default", "aui-icon", "aui-icon-small");
        link.appendChild(icon);
        return link;
    }

    function addDownloadAsLink(attachmentId) {
        var link = document.createElement("div");
        link.title = AJS.I18n.getText("onlyoffice.conversion.download.as");
        link.setAttribute("target", "_blank");

        var icon = document.createElement("span");
        icon.classList.add("icon-default", "aui-icon", "aui-icon-small");
        link.appendChild(icon);
        return link;
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
        var context = jQuery("#attachmentmodule");
        if (context.length != 0) {
            init(context);
        }
    });

    function createDialogDownloadAs(el, attachmentTitle, attachmentId, ext) {

        var dialog = new AJS.Dialog({
            id: "onlioffice-dialog-conversion",
            width:500,
            height:260,
            closeOnOutsideClick: false
        });

        dialog.addHeader(AJS.I18n.getText("onlyoffice.conversion.download.as"));
        dialog.addButton(AJS.I18n.getText("onlyoffice.conversion.download"), function () {
            if (isFormError()){
                sendAjaxConversion(dialog);
                $('#onlioffice-dialog-conversion')[0].innerHTML += '<aui-spinner size="large" filled></aui-spinner>'
                $('.aui-blanket')[0].style.zIndex = 2700;
            }
        });
        dialog.addCancel(AJS.I18n.getText("onlyoffice.cancel"), function (dialog) {
            dialog.remove();
        });
        $("#onlioffice-dialog-conversion .dialog-page-menu").remove();
        var form =  '<form class="aui" id="onlyoffice-form-conversion">'+
                        '<input type="hidden" id="onlyoffice-form-conversion-attachmentId" value="'+ attachmentId +'"></input>'+
                        '<div class="field-group">'+
                            '<label for="onlyoffice-form-conversion-filename">' + AJS.I18n.getText("onlyoffice.conversion.filename") + '<span class="aui-icon icon-required">required</span></label>'+
                            '<input value="'+ attachmentTitle.split(".").shift() +'" class="text" type="text" id="onlyoffice-form-conversion-filename" required>'+
                            '<div class="error" id="onlyoffice-form-conversion-filename-error">'+ AJS.I18n.getText("onlyoffice.form.input.filename") +'</div>'+
                        '</div>'+
                        '<div class="field-group">'+
                            '<label for="onlyoffice-form-conversion-origin-format">' + AJS.I18n.getText("onlyoffice.conversion.origin.format") + '</label>'+
                            '<input value="'+ ext +'" class="text" type="text" id="onlyoffice-form-conversion-origin-format" disabled>'+
                        '</div>'+
                        '<div class="field-group">'+
                            '<label for="onlyoffice-form-conversion-fileExt">'+ AJS.I18n.getText("onlyoffice.conversion.file.format") + '<span class="aui-icon icon-required">required</span></label>'+
                            '<select class="select" id="onlyoffice-form-conversion-fileExt" name="onlyoffice-form-fileExt">'+
                                '<option value>'+ AJS.I18n.getText("onlyoffice.conversion.select.format") +'</option>'+
                            '</select>'+
                            '<div class="error" id="onlyoffice-form-conversion-fileExt-error">'+ AJS.I18n.getText("onlyoffice.form.input.format") +'</div>'+
                        '</div>'+
                        '<input type="submit" id="submit-onlyoffice-form-conversion" class="hidden" />'+
                    '</form>'
        $('#onlioffice-dialog-conversion .dialog-page-body')[0].innerHTML += form;
        return dialog;
    }

    function getOptions(ext) {
        $.ajax({
            type: "GET",
            url: "/plugins/servlet/onlyoffice/conversion?ext=" + ext,
            contentType: "application/json; charset=utf-8",
        }).always(function (result) {
            var req = JSON.parse(result);
            $('#onlyoffice-form-conversion-fileExt')[0].innerHTML += req.options;
        });
    }

    function sendAjaxConversion(dialog) {
        var data = {
            attachmentId: $("#onlyoffice-form-conversion-attachmentId")[0].value,
            filename: $("#onlyoffice-form-conversion-filename")[0].value,
            originFormat: $("#onlyoffice-form-conversion-origin-format")[0].value,
            fileExt: $("#onlyoffice-form-conversion-fileExt")[0].value
        };

        $.ajax({
            type: "POST",
            url: "/plugins/servlet/onlyoffice/conversion?fileUrl=" + data["fileUrl"] + "&attachmentId=" + data["attachmentId"]+ "&filename=" + data["filename"] + "&originFormat=" + data["originFormat"] + "&fileExt=" + data["fileExt"],
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(data)
        }).always(function (result) {
            if (result.includes("code") || result.includes("java")) {
                sendAjaxConversion(dialog);
                showFlag('error', AJS.I18n.getText("onlyoffice.disabled"));
                dialog.remove();
                return;
            }
            if (!result.includes("http")) {
                sendAjaxConversion(dialog);
                return;
            }
            dialog.remove();
            sendDownloadFile(result);
            showFlag('success', AJS.I18n.getText("onlyoffice.download.file"));
        });
    }

    function showFlag(type, message) {
        var flag = AJS.flag({
            type: type,
            body: message,
        });
        setTimeout(flag.close, 2000);
    }

    function sendDownloadFile(url) {
        var link = document.createElement("a");
            link.download = url.substring((url.lastIndexOf("/") + 1), url.length);
            link.href = url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            delete link;
    }

    function isFormError() {
        function isShow(el, bool){
            if (bool) $(el)[0].style = "display: block";
            else $(el)[0].style = "display: none";
        }
        var isSend = true;
        var dialogHeight = 260;

        if ($('#onlyoffice-form-conversion-filename')[0].value == "") {
            isShow('#onlyoffice-form-conversion-filename-error', true);
            isSend = false; dialogHeight += 20;
        } else isShow('#onlyoffice-form-conversion-filename-error', false);

        if ($('#onlyoffice-form-conversion-fileExt')[0].value == "") {
            isShow('#onlyoffice-form-conversion-fileExt-error', true);
            isSend = false; dialogHeight += 20;
        } else isShow('#onlyoffice-form-conversion-fileExt-error', false);

        $('#onlioffice-dialog-conversion')[0].style.height = dialogHeight + "px"
        return isSend;
    }
});