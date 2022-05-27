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

    var clear = function () { this.remove(); }

    function checkEditButton(el) {
        if (jQuery(el.currentTarget).find(".attachment-oo-edit").length != 0) return;

        var dropZone = jQuery("div[duitype='dndattachment/dropzones/AttachmentsDropZone']");

        var attachmentId = getAttachmentId(el);
        var attachmentTitle = getAttachmentTitle(el);
        var ext = attachmentTitle.toLowerCase().split(".").pop();
        if (editExt.indexOf(ext) != -1 && dropZone.length != 0) {
            addEditButton(el, attachmentId, true, false);
        } else if (fillFormExt.indexOf(ext) != -1 && dropZone.length != 0){
            addEditButton(el, attachmentId, true, true);
        } else if (viewExt.indexOf(ext) != -1 || editExt.indexOf(ext) != -1 || fillFormExt.indexOf(ext) != -1) {
            addEditButton(el, attachmentId, false, false);
        }
    }

    function addEditButton(el, attachmentId, edit, form) {
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
        var context = jQuery("#attachmentmodule");
        if (context.length != 0) {
            init(context);
        }

        if (context.length != 0 && $(".onlioffice-create-div").length == 0){
            addCreatButton('#attachmentmodule');
            
            var script = document.createElement("script");
            script.type="text/javascript";
            script.setAttribute('language', "javascript");
            script.innerHTML = sendScript();

            $('body').append(script);
        }
    });

    function sendScript() {
        var script =''+
        'function showForm() {' +
        '       $("#onlioffice-dialog-form-create").show();' +
        //'       $("#background")[0].style = "z-index: 3280; opacity: 1;transition: opacity .2s;transition-delay: .1s;visibility: visible;";' +
        '}'
        return script;
    }

    function addCreatButton(parentElement) {
        var div = document.createElement("div");
        var a = document.createElement("a");
        var spanIcon = document.createElement("span");
        var spanText = document.createElement("span");

        div.classList.add("onlioffice-create-div");
        spanIcon.classList.add("onlioffice-create-icon");   
        spanText.classList.add("onlioffice-create-label");

        a.title = AJS.I18n.getText("onlyoffice.create.file");
        a.href = "#";
        a.id = "onlyoffice-form-file-create";
        a.setAttribute('onclick', 'showForm()');

        spanText.innerText = AJS.I18n.getText("onlyoffice.create.file");

        a.append(spanIcon);
        a.append(spanText);
        div.append(a);
        $(parentElement)[0].append(div);

        var divBlanket = document.createElement("div");
        divBlanket.id = "background";
        divBlanket.classList.add("onlioffice-form-blanket");
        $('body').append(divBlanket);
    }
});
