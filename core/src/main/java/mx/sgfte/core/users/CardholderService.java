package mx.sgfte.core.users;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Business logic for cardholders. Validation lives here (pure Java, testable without Tomcat),
 * NOT in the servlet.
 */
public class CardholderService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final CardholderDao dao;

    public CardholderService() {
        this(new CardholderDao());
    }

    // Constructor for tests (inject a fake DAO).
    public CardholderService(CardholderDao dao) {
        this.dao = dao;
    }

    /** Validates and registers a cardholder. Returns the new id, or throws ValidationException. */
    public long register(Cardholder ch) {
        List<String> errors = validate(ch);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        if (dao.emailExists(ch.getEmail())) {
            throw new ValidationException(List.of("El correo ya está registrado"));
        }
        return dao.insert(ch);
    }

    /** Field-level validation, independent of the database. */
    public List<String> validate(Cardholder ch) {
        List<String> errors = new ArrayList<>();
        if (isBlank(ch.getFirstName())) {
            errors.add("El nombre es obligatorio");
        }
        if (isBlank(ch.getLastName())) {
            errors.add("El apellido es obligatorio");
        }
        if (isBlank(ch.getEmail()) || !EMAIL.matcher(ch.getEmail().trim()).matches()) {
            errors.add("El correo no es válido");
        }
        return errors;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
