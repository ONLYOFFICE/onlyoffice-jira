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
    AJS.Onlyoffice = {};

    AJS.Onlyoffice.FormatManager = {
        supportedFormats: null,
        isEditable: function (ext) {
            for (i = 0; i < this.supportedFormats.length; i++) {
                if (this.supportedFormats[i].name == ext) {
                    return Boolean(this.supportedFormats[i].edit);
                }
            }

            return false;
        },
        isFillForm: function (ext) {
            for (i = 0; i < this.supportedFormats.length; i++) {
                if (this.supportedFormats[i].name == ext) {
                    return Boolean(this.supportedFormats[i].fillForm);
                }
            }

            return false;
        },
        isViewable: function (ext) {
            for (i = 0; i < this.supportedFormats.length; i++) {
                if (this.supportedFormats[i].name == ext) {
                    return true;
                }
            }

            return false;
        },
        isConvertible: function (ext) {
            for (i = 0; i < this.supportedFormats.length; i++) {
                if (this.supportedFormats[i].name == ext) {
                    return this.supportedFormats[i].convertTo != null && this.supportedFormats[i].convertTo.length != 0;
                }
            }

            return false;
        }
    };

    jQuery.get("/plugins/servlet/onlyoffice/formats/info", function(data) {
        AJS.Onlyoffice.FormatManager.supportedFormats = data;
    });

});