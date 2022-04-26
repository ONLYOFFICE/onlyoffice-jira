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
            AddCrateButton('#attachmentmodule');
            var issueId = JIRA.Issue.getIssueId();
            formCreate(issueId);

        }
    });

    function sendScript() {
        var script =''+
        'function showForm(isShow) {' +
        '   if (isShow) {' +
        '       $("#onlioffice-form-create")[0].setAttribute("open", true);' +
        '       $("#background")[0].style = "z-index: 3280; opacity: 1;transition: opacity .2s;transition-delay: .1s;visibility: visible;";' +
        '   }else{' +
        '       $("#onlioffice-form-create")[0].removeAttribute("open");' +
        '       $("#background")[0].style = "";' +
        '}}' +
        'function sendAjaxCreate() {' +
        //'   var data = {' +
        //'        issueId: $("#issueId")[0].value,' +
        //'        fileExt: $("#fileExt")[0].value,' +
        //'        filename: $("#filename")[0].value' +
        //'    };' +
        '    console.log(data);' +
        '    $.ajax({' +
        '        type: "POST",' +
        '        url: "/plugins/servlet/onlyoffice/create?issueId=" + data["issueId"] + "&fileExt=" + data["fileExt"] + "&filename=" + data["filename"],' +
        '        contentType: "application/json; charset=utf-8",' +
        //'        data: JSON.stringify(data)' +
        '    }).always(function (result, textStatus, jqXHR) {' +
        '       JIRA.trigger(JIRA.Events.REFRESH_ISSUE_PAGE, [JIRA.Issue.getIssueId()]);' +
        '   });' +
        '}' +
        'var form = $("#onlioffice-dialog-form")[0];' +
        'form.addEventListener("submit", function(event) {' +
        '    event.preventDefault();' +
        '    sendAjaxCreate();' +
        '    showForm(false);' +
        '    form.reset();' +
        '}, false );';

        return script;
    }

    function AddCrateButton(parentElement) {
        var div = document.createElement("div");
        var a = document.createElement("a");
        var spanIcon = document.createElement("span");
        var spanText = document.createElement("span");

        div.classList.add("onlioffice-create-div");
        spanIcon.classList.add("onlioffice-create-icon");   
        spanText.classList.add("onlioffice-create-label");

        a.title = AJS.I18n.getText("onlyoffice.create.file");
        a.href = "#";
        a.setAttribute('onclick', 'showForm(true)');

        spanText.innerText = AJS.I18n.getText("onlyoffice.create.file");

        a.append(spanIcon);
        a.append(spanText);
        div.append(a);
        $(parentElement)[0].append(div)
    }

    function formCreate(issueId) {
        var section = document.createElement("section");
        section.classList.add("onlioffice-form-create", "aui-layer", "jira-dialog2", "jira-dialog-core", "aui-dialog2-large", "jira-dialog-open", "jira-dialog-content-ready");
        section.id = "onlioffice-form-create";


        var header = document.createElement("header");
        header.classList.add("aui-dialog2-header", "jira-dialog-core-heading")
        var header_h2 = document.createElement("h2");
        header_h2.title = AJS.I18n.getText("onlyoffice.create.file");
        header_h2.innerText = AJS.I18n.getText("onlyoffice.create.file");
        header.append(header_h2);


        var content = document.createElement("div");
        content.classList.add("aui-dialog2-content", "jira-dialog-core-content");
        content.style = "min-height: 100%;"
        var content_div = document.createElement("div");
        content_div.classList.add("qf-container");
        var content_div_form = document.createElement("div");
        content_div_form.classList.add("qf-unconfigurable-form");
        var form = document.createElement("form");
        form.classList.add("aui");
        form.id="onlioffice-dialog-form"
        form.method="post"
        form.action = "/plugins/servlet/onlyoffice/create";
        form.innerHTML = '<input type="hidden" id="issueId" name="issueId" value="'+ issueId +'"></input>'

        var form_content = document.createElement("div");
        form_content.classList.add("content");
        var field_group_type = document.createElement("div");
        field_group_type.classList.add("field-group");
        field_group_type.innerHTML = '<label for="fileExt">' + AJS.I18n.getText("onlyoffice.create.filetype") + '<span class="visually-hidden">Обязательно</span><span class="aui-icon icon-required" aria-hidden="true"></span></label>';
        field_group_type.innerHTML += '<select class="select" id="fileExt" name="fileExt"><option value="docx">'+ AJS.I18n.getText("onlyoffice.context.create.type.docx") +'</option><option value="xlsx">'+ AJS.I18n.getText("onlyoffice.context.create.type.xlsx") +'</option><option value="pptx">'+ AJS.I18n.getText("onlyoffice.context.create.type.pptx") +'</option></select>';
        var field_group_filename = document.createElement("div");
        field_group_filename.classList.add("field-group");
        field_group_filename.innerHTML = '<label for="filename">' + AJS.I18n.getText("onlyoffice.create.filename") + '<span class="visually-hidden">Обязательно</span><span class="aui-icon icon-required" aria-hidden="true"></span></label>';
        field_group_filename.innerHTML += '<input class="text" id="filename" name="filename" type="text" data-qe-no-aria-label="true" required >';

        form_content.append(field_group_type);
        form_content.append(field_group_filename);
        form.append(form_content);
        content.append(form);


        var footer = document.createElement("footer");
        footer.classList.add("aui-dialog2-footer");
        var footer_div_container = document.createElement("div");
        footer_div_container.classList.add("buttons-container", "form-footer");
        var footer_div_buttons = document.createElement("div");
        footer_div_buttons.classList.add("buttons");

        var footer_span = document.createElement("span");
        footer_span.classList.add("throbber");
        var footer_input = document.createElement("input");
        footer_input.classList.add("button", "aui-button", "aui-button-primary");
        footer_input.title = AJS.I18n.getText("onlyoffice.jira.helper.alt.s");
        footer_input.type = "submit";
        footer_input.value = AJS.I18n.getText("onlyoffice.create");
        footer_input.setAttribute('accesskey', "S");
        footer_input.setAttribute('form', "onlioffice-dialog-form");
        footer_input.setAttribute('resolved', true);
        var footer_button = document.createElement("button");
        footer_button.classList.add("aui-button", "aui-button-link", "cancel");
        footer_button.title = AJS.I18n.getText("onlyoffice.jira.helper.alt.tild");
        footer_button.type = "button";
        footer_button.setAttribute('onclick', 'showForm(false)');
        footer_button.innerText = AJS.I18n.getText("onlyoffice.cancel");
        footer_button.setAttribute('accesskey', "`");
        footer_button.setAttribute('resolved', true);

        footer_div_buttons.append(footer_span);
        footer_div_buttons.append(footer_input);
        footer_div_buttons.append(footer_button);
        footer_div_container.append(footer_div_buttons);
        footer.append(footer_div_container);

        section.append(header);
        section.append(content);
        section.append(footer);

        $('body').append(section);

        var script = document.createElement("script");
        script.type="text/javascript";
        script.setAttribute('language', "javascript");
        script.innerHTML = sendScript();

        $('body').append(script);

        var divBlanket = document.createElement("div");
        divBlanket.id = "background";
        divBlanket.classList.add("onlioffice-form-blanket");
        $('body').append(divBlanket);
    }
});
