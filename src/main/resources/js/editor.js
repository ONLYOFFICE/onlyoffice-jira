(function(AJS) {
    var icons = document.querySelectorAll("link[rel~='icon'][href$='favicon.ico']");
    for (var i = 0; i < icons.length; i++) {
        icons[i].remove();
    }

    var docEditor;

    var onAppReady = function() {
        var error = AJS.Meta.get("onlyoffice-error");
        if (error) {
            docEditor.showMessage(error);
            return;
        }

        if (!AJS.Meta.get("onlyoffice-attachment-id")) {
            createNewAttachment();
            return;
        }

        if (AJS.Meta.get("onlyoffice-demo")) {
            docEditor.showMessage(AJS.I18n.getText("onlyoffice.editor.message.demo"));
            return
        }
    }

    var onMakeActionLink = function (event) {
        var actionData = JSON.stringify(event.data);
        var actionLink = new URL(location.href);

        actionLink.searchParams.set("actionData", actionData);

        docEditor.setActionLink(actionLink.toString());
    };

    const onOutdatedVersion = function () {
        location.reload(true);
    };

    const onRequestRefreshFile = function () {
        location.reload(true);
    };

    var onRequestSaveAs = function (event) {
        var url = event.data.url;
        var fileType = event.data.fileType ? event.data.fileType : event.data.title.split(".").pop();

        const queryParams = new URLSearchParams({
            type: "save-as"
        });

        fetch(AJS.contextPath() + AJS.Meta.get("onlyoffice-api-path") + "?" + queryParams.toString(), {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                url: url,
                fileType: fileType,
                attachmentId: AJS.Meta.get("onlyoffice-attachment-id")
            })
        })
        .then(function(response) {
            if (!response.ok) {
                if (response.status === 403 || response.status === 404) {
                    JIRA.Messages.showErrorMsg(
                        AJS.I18n.getText("attachment.service.error.create.no.permission"),
                        {closeable: true}
                    );
                    return;
                }

                JIRA.Messages.showErrorMsg(
                    AJS.I18n.getText("rest.error.unexpected"),
                    {closeable: true}
                );
                return;
            }

            return response.json();
        })
        .then(function(data) {
            if (!data) {
                return;
            }

           var successMessage = AJS.I18n.getText("onlyoffice.editor.save-as.message.success");

           successMessage = successMessage.replace(
               "$",
               '<a href="/plugins/servlet/onlyoffice/doceditor?attachmentId=' + data.attachmentId + '" target="_blank"><b>' + data.fileName + '</b></a>'
           );

           JIRA.Messages.showSuccessMsg(successMessage, {closeable: true});
        });
    };

    const onRequestUsers = function(event) {
        switch (event.data.c) {
            case "info":
                const queryParams = new URLSearchParams({
                    type: "users-info"
                });

                fetch(AJS.contextPath() + AJS.Meta.get("onlyoffice-api-path") + "?" + queryParams.toString(), {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        ids: event.data.id

                    })
                }).then(function(response) {
                    if (!response.ok) {
                         return;
                    }

                    return  response.json();
                })
                .then(function (data) {
                    if (!data) {
                        return;
                    }

                    docEditor.setUsers({
                        "c": event.data.c,
                        "users": data.users,
                    });
                })
                break;
        }
    }

    var connectEditor = function() {
        if (typeof DocsAPI === "undefined") {
            alert("ONLYOFFICE is not available. Please contact us at support@onlyoffice.com");
            return;
        }

        var docsVersion = DocsAPI.DocEditor.version().split(".");
        if (docsVersion[0] < 6 || docsVersion[0] == 6 && docsVersion[1] == 0) {
            alert(AJS.I18n.getText("onlyoffice.editor.error.version"));
            return;
        }

        var config = JSON.parse(AJS.Meta.get("onlyoffice-editor-config") || "{}");

        if (config.document && (config.document.fileType === "docxf" || config.document.fileType === "oform")
            && docsVersion[0] < 7) {
            alert(AJS.I18n.getText("onlyoffice.editor.forms.error.version"));
            return;
        }

        config.events = {
            "onAppReady": onAppReady,
            "onMakeActionLink": onMakeActionLink,
            "onOutdatedVersion": onOutdatedVersion,
            "onRequestRefreshFile": onRequestRefreshFile,
            "onRequestUsers": onRequestUsers
        };

        if (AJS.Meta.get("onlyoffice-can-save-as")) {
            config.events["onRequestSaveAs"] = onRequestSaveAs;
        }

        docEditor = new DocsAPI.DocEditor("iframeEditor", config);
    };

    var createNewAttachment = function () {
        const queryParams = new URLSearchParams({
            type: "create-new"
        });

        fetch(AJS.contextPath() + AJS.Meta.get("onlyoffice-api-path") + "?" + queryParams.toString(), {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                issueId: AJS.Meta.get("onlyoffice-issue-id"),
                documentType: AJS.Meta.get("onlyoffice-document-type"),
                fileName: AJS.Meta.get("onlyoffice-file-name")
            })
        })
        .then(function(response) {
            if (!response.ok) {
                if (response.status === 403 || response.status === 404) {
                     docEditor.showMessage(AJS.I18n.getText("attachment.service.error.create.no.permission"));
                    return;
                }

                docEditor.showMessage(AJS.I18n.getText("rest.error.unexpected"));
                return;
            }

            return response.json();
        })
        .then(function(data) {
            if (!data) {
                return;
            }

            const url = new URL(window.location);
            url.search = "";
            url.searchParams.set('attachmentId', data.attachmentId);

            window.location.href = url.toString();
        })
    }

    if (window.addEventListener) {
        window.addEventListener("load", connectEditor);
    } else if (window.attachEvent) {
        window.attachEvent("load", connectEditor);
    }
})(AJS);