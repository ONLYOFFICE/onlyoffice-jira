package onlyoffice.sdk.service.settings;

import com.atlassian.sal.api.message.I18nResolver;
import com.onlyoffice.client.DocumentServerClient;
import com.onlyoffice.manager.url.UrlManager;
import com.onlyoffice.model.common.CommonResponse;
import com.onlyoffice.model.settings.Settings;
import com.onlyoffice.model.settings.validation.ValidationResult;
import com.onlyoffice.model.settings.validation.status.Status;
import com.onlyoffice.service.settings.DefaultSettingsValidationServiceV2;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class SettingsValidationServiceImpl extends DefaultSettingsValidationServiceV2
        implements SettingsValidationService {
    private final Logger log = LogManager.getLogger(this.getClass());

    private final I18nResolver i18n;

    public SettingsValidationServiceImpl(final DocumentServerClient documentServerClient, final UrlManager urlManager,
                                         final I18nResolver i18n) {
        super(documentServerClient, urlManager);
        this.i18n = i18n;
    }

    @Override
    public Map<String, ValidationResult> validateSettings(final Settings settings) {
        Map<String, ValidationResult> result = new HashMap<>();

        try {
            result.put(
                    "documentServer",
                    checkDocumentServer()
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            result.put(
                    "documentServer",
                    ValidationResult.builder()
                            .status(Status.FAILED)
                            .error(CommonResponse.Error.CONNECTION)
                            .build()
            );
        }

        try {
            result.put(
                    "commandService",
                    checkCommandService()
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            result.put(
                    "commandService",
                    ValidationResult.builder()
                            .status(Status.FAILED)
                            .error(CommonResponse.Error.CONNECTION)
                            .build()
            );
        }

        try {
            result.put(
                    "convertService",
                    checkConvertService(null)
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            result.put(
                    "convertService",
                    ValidationResult.builder()
                            .status(Status.FAILED)
                            .error(CommonResponse.Error.CONNECTION)
                            .build()
            );
        }

        if (result.get("documentServer").getStatus().equals(Status.FAILED)) {
            result.get("documentServer")
                    .setMessage(
                            i18n.getText(
                                    "onlyoffice.server.common.error." + result.get("documentServer")
                                            .getError()
                                            .toString()
                                            .toLowerCase()
                            )
                    );
        }

        if (result.get("commandService").getStatus().equals(Status.FAILED)) {
            result.get("commandService")
                    .setMessage(
                            i18n.getText(
                                    "onlyoffice.service.command.error." + result.get("commandService")
                                            .getError()
                                            .toString()
                                            .toLowerCase()
                            )
                    );
        }

        if (result.get("convertService").getStatus().equals(Status.FAILED)) {
            result.get("convertService")
                    .setMessage(
                            i18n.getText(
                                    "onlyoffice.service.convert.error." + result.get("convertService")
                                            .getError()
                                            .toString()
                                            .toLowerCase()
                            )
                    );
        }

        return result;
    }
}
