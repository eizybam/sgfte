package mx.sgfte.core.users;

import java.util.List;

/** Thrown when a cardholder fails validation. Carries all messages for the view. */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(String.join(", ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
