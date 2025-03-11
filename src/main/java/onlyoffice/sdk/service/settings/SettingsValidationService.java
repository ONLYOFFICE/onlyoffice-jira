package onlyoffice.sdk.service.settings;

import com.onlyoffice.model.settings.Settings;
import com.onlyoffice.model.settings.validation.ValidationResult;

import java.util.Map;

public interface SettingsValidationService extends com.onlyoffice.service.settings.SettingsValidationService {
    Map<String, ValidationResult> validateSettings(Settings settings);
}
