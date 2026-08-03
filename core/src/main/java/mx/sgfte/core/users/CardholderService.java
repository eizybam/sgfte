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
        // El código se asigna aquí y sólo aquí: una vez guardado no se recalcula.
        if (isBlank(ch.getEmployeeCode())) {
            String fullName = (ch.getFirstName() + " " + ch.getLastName()).trim();
            ch.setEmployeeCode(EmployeeCode.of(fullName, dao.nextEmployeeSequence()));
        }
        return dao.insert(ch);
    }

    /**
     * Registers from the single "Nombre completo" field the modal uses.
     *
     * The table stores the name in two columns, so the first word becomes the
     * given name and the rest the surnames — "Diego Jarillo Estrada" splits into
     * "Diego" / "Jarillo Estrada". A one-word name is rejected rather than
     * guessed at, because a blank last_name would violate the schema.
     */
    public long registerFromFullName(String fullName, String email, String department) {
        String[] parts = splitName(fullName);
        if (parts == null) {
            throw new ValidationException(List.of("Escribe el nombre y al menos un apellido"));
        }
        Cardholder ch = new Cardholder(parts[0], parts[1], trim(email), null);
        ch.setDepartment(trim(department));
        return register(ch);
    }

    /** {given name, surnames} or null when there is only one word. */
    static String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return null;
        String[] words = fullName.trim().split("\\s+");
        if (words.length < 2) return null;

        String surnames = String.join(" ", java.util.Arrays.copyOfRange(words, 1, words.length));
        return new String[]{words[0], surnames};
    }

    private String trim(String s) { return s == null ? null : s.trim(); }

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
