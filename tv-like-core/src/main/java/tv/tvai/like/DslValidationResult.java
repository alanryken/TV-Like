package tv.tvai.like;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DslValidationResult {

    private final List<String> errors = new ArrayList<String>();
    private final List<String> warnings = new ArrayList<String>();

    public void addError(String message) {
        if (message != null && !message.trim().isEmpty()) {
            errors.add(message.trim());
        }
    }

    public void addWarning(String message) {
        if (message != null && !message.trim().isEmpty()) {
            warnings.add(message.trim());
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
}
