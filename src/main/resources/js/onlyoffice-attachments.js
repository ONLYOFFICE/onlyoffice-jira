jQuery(document).ready(function() {

    function checkEditButton(el) {
        if (el.currentTarget.hasAttribute("oo-edit")) return;
        addEditButton(el);
    }

    function addEditButton(el) {
        el.currentTarget.setAttribute("oo-edit", "");

        var delButton = jQuery(el.currentTarget).find(".attachment-delete");
        var attachmentId = delButton.children("a")[0].id.substr(4);

        var divWrapper = document.createElement("div");
        divWrapper.classList.add("attachment-oo-edit");
        var link = document.createElement("a");
        link.title = AJS.I18n.getText("onlyoffice.connector.editlink");
        link.setAttribute("target", "_blank");
        link.href = "/plugins/servlet/onlyoffice/doceditor?attachmentId=" + attachmentId;
        var icon = document.createElement("span");
        icon.classList.add("icon-default", "aui-icon", "aui-icon-small");
        link.appendChild(icon);
        divWrapper.appendChild(link);

        jQuery(divWrapper).insertBefore(delButton);
    }

    function init() {
        jQuery(".attachment-content.js-file-attachment").hover(checkEditButton);
    }

    init();
});