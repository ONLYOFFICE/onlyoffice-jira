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

jQuery(function () {
    var dialog = new AJS.FormPopup({
        id: "onlyoffice-conversion-dialog",
        trigger: ".onlyoffice-conversion"
    });

    dialog.onContentReady(function() {
        var popup = this.$popup;
        var content = popup.find("#form-data");
        var submitButton = popup.find(".button");
        var menuItem = popup.find(".dialog-menu-item");
        var formErrorMessage = popup.find(".aui-message-context");
        var form = popup.find("form");
        var params = {
            actionType: AJS.Onlyoffice.Conversion.actionType,
            fileName: AJS.Onlyoffice.Conversion.fileName,
            currentFileType: AJS.Onlyoffice.Conversion.currentFileType,
            targetFileType: AJS.Onlyoffice.Conversion.targetFileType,
            targetFileTypeList: AJS.Onlyoffice.Conversion.targetFileTypeList
        };

        content.html(JIRA.Templates.Onlyoffice.conversion(params));

        menuItem.click(function (e) {
            menuItem.removeClass("selected");
            jQuery(e.target).addClass("selected");

            formErrorMessage.html("");

            var actionId = e.target.id.replace("item-", "");

            var titleButton = actionId == "conversion" ? AJS.I18n.getText("onlyoffice.connector.dialog.conversion.button.conversion") : AJS.I18n.getText("onlyoffice.connector.dialog.conversion.button.download-as");
            submitButton.val(titleButton);

            params.actionType = actionId;
            content.html(JIRA.Templates.Onlyoffice.conversion(params));
        });

        submitButton.click(function (e) {
            e.preventDefault();

            var actionUrl = "/" + form.attr("action");

            var data = {
                "atl_token": form.find("#atl-token").attr("value"),
                "id": form.find("#action-id").attr("value"),
                "attachmentId": form.find("#attachment-id").attr("value"),
                "actionType": form.find("#action-type").attr("value"),
                "fileName": form.find("#file-name").attr("value"),
                "targetFileType": form.find("#target-file-type").attr("value")

            };

            dialog.showFooterLoadingIndicator();
            form.find("input,select").not(".disabled").attr("disabled","disabled");

            conversionRequest(
                actionUrl,
                data,
                function(response) {
                    if (data.actionType == "conversion") {
                        var successMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.success").replace(
                            "$",
                            "<a href='" + response.fileUrl + "' target='_blank'><b>" + response.fileName + "</b></a>"
                        );
                        JIRA.Messages.showReloadSuccessMsg(successMessage);
                        JIRA.IssueNavigator.reload();
                    } else {
                        dialog.hide();
                        jQuery("#onlyoffice-download-as-iframe").remove();
                        jQuery("body").append("<iframe id='onlyoffice-download-as-iframe' style='display:none;'></iframe>");
                        jQuery("#onlyoffice-download-as-iframe").attr("src", response.fileUrl);
                    }
                },
                function(errorMessage) {
                    formErrorMessage.html("<div class='aui-message aui-message-error'>" + errorMessage + "</div>");
                    dialog.hideFooterLoadingIndicator();
                    form.find("input,select").not(".disabled").attr("disabled", null);
                }
            )
        });
    });

    function conversionRequest(url, data, onSuccess, onError) {
        jQuery.ajax({
            type: "POST",
            url: url,
            data: data,
            success: function (response) {
                if (response.error) {
                    var errorMessage;
                    var servicePrefix = false;

                    switch (response.error) {
                        case -1:
                             errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.unknown");
                             servicePrefix = true;
                             break;
                        case -2:
                            errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.timeout");
                            servicePrefix = true;
                            break;
                        case -3:
                            errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.conversion");
                            servicePrefix = true;
                            break;
                        case -4:
                            errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.download");
                            servicePrefix = true;
                            break;
                        case -5:
                            errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.password");
                            servicePrefix = true;
                            break;
                        case -6:
                            errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.database");
                            servicePrefix = true;
                            break;
                        case -7:
                            errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.input");
                            servicePrefix = true;
                            break;
                        case -8:
                            errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.token");
                            servicePrefix = true;
                            break;
                        case -10:
                            errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.not-reached");
                            break;
                        default:
                            errorMessage = AJS.I18n.getText("onlyoffice.connector.error.Unknown");
                    }

                    if (servicePrefix) {
                       errorMessage = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.service-prefix").replace("$", errorMessage);
                    }

                    onError(errorMessage);
                    return;
                }

                if (response.endConvert) {
                    onSuccess(response);
                } else {
                    setTimeout(function() {
                        conversionRequest(url, data, onSuccess, onError);
                    }, 1000);
                }
            },
            error: function (xhr) {
                if (xhr.status == 403) {
                    onError(AJS.I18n.getText("onlyoffice.connector.dialog.conversion.message.error.permission"));
                } else {
                    onError(AJS.I18n.getText("onlyoffice.connector.error.Unknown"));
                }
            }
        });
    }

});
