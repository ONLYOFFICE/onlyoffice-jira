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
            var issueId = '';
            if ($('.issue-link').length != 0) {
                issueId = $('.issue-link')[0].rel;
            }
            if ($('#ghx-detail-issue').length != 0) {
                issueId = $('#ghx-detail-issue')[0].getAttribute('data-issueid');
            }
            formCreate(issueId);
            
        }

        if (context.length != 0 && $('fieldset.group').length == 1) {
            AddCrateButton('fieldset.group');
        }
    });

    function sendScript() {
        var script = '';
        //var data = {
        //    fileExt: "docx",
        //    filename: "secret"
        //};
        //$.ajax({
        //    type: 'GET',
        //    url: 'http://192.168.1.69:8080/plugins/servlet/onlyoffice/create',
        //    contentType: "application/json; charset=utf-8",
        //    data: JSON.stringify(data)
        //}).always(function (result, textStatus, jqXHR) {
        //    console.log(result)
        //});

        //function getFile(bytes, type, filename) {
        //var blob = new Blob(bytes.split(''), {type: type});

        //const file1 = new File(blob, filename, {
            //type: blob.type,
        //});

        //const file2 = new File(bytes.split(''), filename, {
            //type: type,
        //});

        //console.log(file1);
        //console.log(file2);

        script += 'function getFile(bytes, filename, type) {';
        script += 'var blob = new Blob(bytes.split(""), {type: type});';
        script += 'const options = {autoProcessQueue: false, url: "/file/post"};';
        script += 'const myDropzone = new Dropzone( $(".issue-drop-zone__file").get(1), options);';
        script += 'blob.name = filename; myDropzone.addFile(blob);'
        script += 'console.log(blob);console.log(bytes);console.log(type);}';

        script += 'function sendAJAX() {var data = {fileExt: "docx",filename: "secret"};';
        script += "var a = $.ajax({type: 'GET',url: 'http://192.168.1.69:8080/plugins/servlet/onlyoffice/create',";
        script += 'contentType: "application/json; charset=utf-8",data: JSON.stringify(data)';
        script += '}).always(function (result, textStatus, jqXHR) { console.log(a.getAllResponseHeaders()); getFile(a.getResponseHeader("---file---"), a.getResponseHeader("---filename---"), a.getResponseHeader("---contenttype---")) }); }';

        return script;
    }

    function showForm(isShow) {
        var script = '';
        if (isShow) {
            script += "$('#onlioffice-form-create')[0].setAttribute('open', true);"
            script += "$('#background')[0].style = 'z-index: 2980; opacity: 1;transition: opacity .2s;transition-delay: .1s;visibility: visible;';";
        }else{
            script += "$('#onlioffice-form-create')[0].removeAttribute('open');"
            script += "$('#background')[0].style = ''";
        }
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

        a.title = "Create file with ONLYOFFICE";
        a.href = "#";
        a.setAttribute('onclick', showForm(true));

        spanText.innerText = "Create file with ONLYOFFICE";

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
        header_h2.title = "Create file with ONLYOFFICE";
        header_h2.innerText = "Create file with ONLYOFFICE";
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
        form.innerHTML = '<input type="hidden" name="issueId" value="'+ issueId +'"></input>'

        var form_content = document.createElement("div");
        form_content.classList.add("content");
        var field_group_type = document.createElement("div");
        field_group_type.classList.add("field-group");
        field_group_type.innerHTML = '<label for="fileExt">' + 'Тип файла' + '<span class="visually-hidden">Обязательно</span><span class="aui-icon icon-required" aria-hidden="true"></span></label>';
        field_group_type.innerHTML += '<select class="select" id="fileExt" name="fileExt"><option value="docx">Document</option><option value="xlsx">Spreadsheet</option><option value="pptx">Presentation</option></select>';
        var field_group_filename = document.createElement("div");
        field_group_filename.classList.add("field-group");
        field_group_filename.innerHTML = '<label for="filename">' + 'Имя' + '<span class="visually-hidden">Обязательно</span><span class="aui-icon icon-required" aria-hidden="true"></span></label>';
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
        footer_input.title = "Нажмите Alt + S, чтобы отправить эту форму";
        footer_input.type = "submit";
        footer_input.value = "Создать";
        footer_input.setAttribute('accesskey', "S");
        footer_input.setAttribute('form', "onlioffice-dialog-form");
        footer_input.setAttribute('resolved', true);
        var footer_button = document.createElement("button");
        footer_button.classList.add("aui-button", "aui-button-link", "cancel");
        footer_button.title = "Нажмите Alt + ` для отмены";
        footer_button.type = "button";
        footer_button.setAttribute('onclick', showForm(false));
        footer_button.innerText = "Отмена";
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


        var script = document.createElement("script");
        script.type="text/javascript";
        script.setAttribute('language', "javascript");
        script.innerHTML = sendScript();

        $('body').append(script);

        $('body').append(section);

        var divBlanket = document.createElement("div");
        divBlanket.id = "background";
        divBlanket.classList.add("onlioffice-form-blanket");
        $('body').append(divBlanket);
    }
});
