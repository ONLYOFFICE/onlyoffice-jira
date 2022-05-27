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

    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(e, context, reason) {

        if (context.length != 0 && $("#onlioffice-dialog-form-create").length == 0){
            var issueId = JIRA.Issue.getIssueId();
            var dialog = createDialog(issueId);
            var form = $("#onlyoffice-form-create")[0];
            form.addEventListener("submit", function(event) {
                event.preventDefault();
                sendAjaxCreate();
                dialog.hide();
                form.reset();
            }, false );
        }
    });

    function createDialog(issueId) {
        var dialog = new AJS.Dialog({
            id: "onlioffice-dialog-form-create",
            width:800,
            height:500, 
            closeOnOutsideClick: false
        });

        dialog.addHeader(AJS.I18n.getText("onlyoffice.create.file"));
        dialog.addButton(AJS.I18n.getText("onlyoffice.create"), function () {
            $('#submit-onlyoffice-form-create')[0].click();
        });
        dialog.addCancel(AJS.I18n.getText("onlyoffice.cancel"), function (dialog) {
            dialog.hide();
        });
        $("#onlioffice-dialog-form-create .dialog-page-menu").remove();

        form =  '<form class="aui" id="onlyoffice-form-create">'+
                    '<input type="hidden" id="onlyoffice-form-issueId" name="onlyoffice-form-issueId" value="'+ issueId +'"></input>'+
                    '<div class="field-group">'+
                        '<label for="fileExt">'+ AJS.I18n.getText("onlyoffice.create.filetype") +'</label>'+
                        '<select class="select" id="onlyoffice-form-fileExt" name="onlyoffice-form-fileExt">'+
                            '<option value="docx">'+ AJS.I18n.getText("onlyoffice.context.create.type.docx") +'</option>'+
                            '<option value="xlsx">'+ AJS.I18n.getText("onlyoffice.context.create.type.xlsx") +'</option>'+
                            '<option value="pptx">'+ AJS.I18n.getText("onlyoffice.context.create.type.pptx") +'</option>'+
                        '</select>'+
                    '</div>'+
                    '<div class="field-group">'+
                        '<label for="text-input">' + AJS.I18n.getText("onlyoffice.create.filename") + '<span class="aui-icon icon-required">required</span></label>'+
                        '<input class="text" type="text" id="onlyoffice-form-filename" name="onlyoffice-form-filename" title="Text input" required>'+
                    '</div>'+
                    '<input type="submit" id="submit-onlyoffice-form-create" class="hidden" />'+
                '</form>'
        $('#onlioffice-dialog-form-create .dialog-page-body')[0].innerHTML += form;
        return dialog;
    }

    function sendAjaxCreate() {
        var data = {
                issueId: $("#onlyoffice-form-issueId")[0].value,
                fileExt: $("#onlyoffice-form-fileExt")[0].value,
                filename: $("#onlyoffice-form-filename")[0].value
            };
        $.ajax({
            type: "POST",
            url: "/plugins/servlet/onlyoffice/create?issueId=" + data["issueId"] + "&fileExt=" + data["fileExt"] + "&filename=" + data["filename"],
            contentType: "application/json; charset=utf-8",
            data: JSON.stringify(data)
        }).always(function (result, textStatus, jqXHR) {
           JIRA.trigger(JIRA.Events.REFRESH_ISSUE_PAGE, [JIRA.Issue.getIssueId()]);
       });
    }
 });


 