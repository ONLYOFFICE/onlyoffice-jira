/**
 *
 * (c) Copyright Ascensio System SIA 2023
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
    function CreateOnlyofficeEditorButton(el) {
        var dropZone = jQuery("div[duitype='dndattachment/dropzones/AttachmentsDropZone']");

        var attachmentId = getAttachmentId(el);
        var attachmentTitle = getAttachmentTitle(el);
        var ext = attachmentTitle.toLowerCase().split(".").pop();

        var nameButton = "onlyoffice-editor";
        var hrefButton = "/plugins/servlet/onlyoffice/doceditor?attachmentId=" + attachmentId;
        var iconButton = "icon-onlyoffice-edit";
        var titleButton;


        if (AJS.Onlyoffice.FormatManager.isEditable(ext) && dropZone.length != 0) {
            titleButton = AJS.I18n.getText("onlyoffice.connector.editlink");
        } else if (AJS.Onlyoffice.FormatManager.isViewable(ext)) {
            titleButton = AJS.I18n.getText("onlyoffice.connector.viewlink");
        }

        if (titleButton) {
            CreateOnlyofficeButton(el.currentTarget, nameButton, titleButton, hrefButton, true, iconButton);
        }
    }

    function CreateOnlyofficeConversionButton(el) {
        var dropZone = jQuery("div[duitype='dndattachment/dropzones/AttachmentsDropZone']");

        var attachmentId = getAttachmentId(el);
        var attachmentTitle = getAttachmentTitle(el);
        var ext = attachmentTitle.toLowerCase().split(".").pop();

        var nameButton = "onlyoffice-conversion";
        var hrefButton = "/OnlyOfficeConversion!default.jspa?id=" + JIRA.Issue.getIssueId() + "&attachmentId=" + attachmentId;
        var iconButton = "icon-onlyoffice-conversion";
        var titleButton = AJS.I18n.getText("onlyoffice.connector.dialog.conversion.header.title");

        if (AJS.Onlyoffice.FormatManager.isConvertible(ext) && dropZone.length != 0) {
            CreateOnlyofficeButton(el.currentTarget, nameButton, titleButton, hrefButton, false, iconButton);
        }

    }

     function CreateOnlyofficeButton (targetElement, name, title, href, blank, classIcon) {
        if (jQuery(targetElement).find(".attachment-onlyoffice-button." + name).length != 0) return;

        var target = jQuery(targetElement).find(".blender");
        var elementWrapper;

        if (target.length != 0) {
            elementWrapper = document.createElement("div");
        } else {
            target = jQuery(targetElement).find(".attachment-title");
            elementWrapper = document.createElement("dd");
        }

        var tabIndex = jQuery(targetElement).find("a").length - 2;

        if (tabIndex > 0) {
            elementWrapper.style.marginRight = 20 * tabIndex + "px";
            target.addClass("blender-onlyoffice" + tabIndex);
        }

        var link = document.createElement("a");
        link.title = title;
        link.href = href;
        if (blank) { link.setAttribute("target", "_blank"); }
        var icon = document.createElement("span");
        icon.classList.add("icon-default", "aui-icon", "aui-icon-small", classIcon);

        link.appendChild(icon);

        elementWrapper.classList.add("attachment-onlyoffice-button", name);
        elementWrapper.appendChild(link);

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
        context.find("li.attachment-content.js-file-attachment").hover(CreateOnlyofficeConversionButton);
        context.find("li.attachment-content.js-file-attachment").hover(CreateOnlyofficeEditorButton);
    }

    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(e, context, reason) {
        var context = jQuery("#attachmentmodule");
        if (context.length != 0) {
            init(context);
        }
    });
});