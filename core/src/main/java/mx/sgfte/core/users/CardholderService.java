package mx.sgfte.core.users;

import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.auth.*;
import mx.sgfte.core.categories.Category;
import mx.sgfte.core.notifications.NotificationService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import mx.sgfte.core.deletion.DeletionService;
import mx.sgfte.core.shared.db.Db;

/**
 * Business logic for cardholders. Validation lives here (pure Java, testable without Tomcat),
 * NOT in the servlet.
 */
public class CardholderService {
    private final DeletionService deletionService = new DeletionService();

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final NotificationService notificationService =
            new NotificationService();

    private final CardholderDao dao;
    private final UserDao userDao = new UserDao();
    private final PasswordTokenService passwordTokenService = new PasswordTokenService();

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
        long id =  dao.insert(ch);
        createLogin(id, ch);
        return id;
    }

    private void createLogin(long cardholderId, Cardholder ch) {
        try {
            String fullName = (ch.getFirstName() + " " + ch.getLastName()).trim();

            AppUser login = new AppUser();
            login.setEmail(ch.getEmail());
            login.setPasswordHash(PasswordHasher.hash(UUID.randomUUID().toString())); // random password; user must reset
            login.setFullName(fullName);
            login.setRole(Role.CARDHOLDER);
            login.setCardholderId(cardholderId);
            login.setStatus("PENDING");
            long appUserId = userDao.insert(login);

            passwordTokenService.issueActivationToken(appUserId, cardholderId, ch.getEmail(), fullName);

        } catch (RuntimeException e) {
            System.err.println("[CARDHOLDER] no se pudo crear el acceso de " + ch.getEmail() + ": " + e.getMessage());
        }
    }

    /**
     * Registers from the single "Nombre completo" field the modal uses.
     *
     * The table stores the name in two columns, so the first word becomes the
     * given name and the rest the surnames — "Diego Jarillo Estrada" splits into
     * "Diego" / "Jarillo Estrada". A one-word name is rejected rather than
     * guessed at, because a blank last_name would violate the schema.
     */
    public long registerFromFullName(String fullName, String email, Long departmentId) {
        String[] parts = splitName(fullName);
        if (parts == null) {
            throw new ValidationException(List.of("Escribe el nombre y al menos un apellido"));
        }
        Cardholder ch = new Cardholder(parts[0], parts[1], trim(email), null);
        // El área llega como id del catálogo (V10), no como texto: la FK es
        // quien garantiza que exista, así que aquí no hay nada que validar.
        ch.setDepartmentId(departmentId);
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

    /**
     * Activa o desactiva a un tarjetahabiente. Devuelve su estado NUEVO.
     *
     * Desactivar no es un simple UPDATE: delega en DeletionService, que en una
     * sola transacción reintegra el saldo de todas sus cuentas a la Concentradora,
     * invalida sus tarjetas y lo deja INACTIVE. La fila del empleado y su
     * histórico se conservan.
     *
     * Reactivar sí es un simple UPDATE: vuelve sin cuentas ni tarjetas, hay que
     * asignárselas de nuevo.
     */
    public boolean toggleStatus(Long cardholderId) {
        if (cardholderId == null) {
            throw new ValidationException(List.of("No se indicó qué empleado cambiar."));
        }

        CardholderDetail found = dao.findDetail(cardholderId)
                .orElseThrow(() -> new ValidationException(
                        List.of("El tarjetahabiente ya no existe.")));

        if (found.isActive()) {
            deletionService.deleteCardholder(cardholderId);
            return false;
        } else {
            deletionService.activateCardholder(cardholderId);
            return true;
        }

    }

    public void update(long id, String fullName, String email,
                       Long departmentId, String phone) {

        String[] parts = splitName(fullName);
        if (parts == null) {
            throw new ValidationException(List.of("Escribe el nombre y al menos un apellido"));
        }

        Cardholder ch = new Cardholder(parts[0], parts[1], trim(email), trim(phone));
        ch.setId(id);
        ch.setDepartmentId(departmentId);

        List<String> errors = validate(ch);          // el MISMO validador del alta
        if (!errors.isEmpty()) throw new ValidationException(errors);

        if (dao.emailExistsForAnother(ch.getEmail(), id)) {
            throw new ValidationException(List.of("El correo ya está registrado"));
        }

        String full = (ch.getFirstName() + " " + ch.getLastName()).trim();

        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                dao.update(conn, ch);
                userDao.updateIdentity(conn, id, ch.getEmail(), full);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                /*
                  ORA-00001 = choque con un UNIQUE. Llega aquí pese al chequeo de
                  arriba por dos motivos, y los dos son reales:
                    · el correo puede estar tomado por un ADMIN, que no tiene
                      ficha de cardholder y por tanto no sale en esa consulta;
                    · entre el chequeo y el UPDATE cabe otra alta.
                  La base es la que decide de verdad; aquí sólo se traduce.
                */
                if (e instanceof SQLException sql && sql.getErrorCode() == 1) {
                    throw new ValidationException(List.of("El correo ya está registrado"));
                }
                throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in update cardholder transaction", e);
        }
    }
}
