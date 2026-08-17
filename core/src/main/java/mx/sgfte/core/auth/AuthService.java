package mx.sgfte.core.auth;

import mx.sgfte.core.users.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthService {
    // A real-shaped hash used only to spend similar CPU time when the email
    // doesn't exist, so "no such user" and "wrong password" take about as long.
    private static final String DUMMY_HASH =
            "$2a$12$P6s7lL4fFj3Qcy0Szxi1g.mKC1LD6DlVRn6xKyiNTD7yK4am6T2ky";

    /** Same minimum that SetPasswordServlet enforces on first creation. */
    public static final int MIN_LENGTH = 8;

    private final UserDao userDao;

    public AuthService() {
        this(new UserDao());
    }

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public Optional<AppUser> authenticate(String email, String rawPassword){
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()){
            return Optional.empty();
        }

        Optional<AppUser> found = userDao.findLoginByEmail(email);
        if (found.isEmpty()){
            PasswordHasher.matches(rawPassword, DUMMY_HASH); // burn comparable time
            return Optional.empty();
        }

        AppUser user = found.get();
        if (!"ACTIVE".equals(user.getStatus())) {
            return Optional.empty();
        }

        if (!PasswordHasher.matches(rawPassword, user.getPasswordHash())){
            return Optional.empty();
        }
        return Optional.of(user);
    }

    /**
     * Changes the password of the currently logged-in user (RF-01).
     *
     * Asks for the current password on purpose: without it, a session left open
     * on a shared machine is enough to take over the account forever. The
     * session proves WHO logged in; the current password proves it is still
     * that person who is typing.
     *
     * Collects all problems and reports them together: nobody wants to fix one
     * and discover the next on submit.
     */
    public void changePassword(long userId, String current, String next, String confirm) {
        List<String> errors = new ArrayList<>();

        AppUser user = userDao.findById(userId).orElseThrow(
                () -> new ValidationException(List.of("La sesión ya no es válida. Vuelve a entrar.")));

        if (current == null || current.isBlank()) {
            errors.add("Escribe tu contraseña actual.");
        } else if (!PasswordHasher.matches(current, user.getPasswordHash())) {
            errors.add("La contraseña actual no es correcta.");
        }
        if (next == null || next.length() < MIN_LENGTH) {
            errors.add("La nueva contraseña debe tener al menos " + MIN_LENGTH + " caracteres.");
        }
        if (next != null && !next.equals(confirm)) {
            errors.add("La confirmación no coincide con la nueva contraseña.");
        }
        if (next != null && next.equals(current)) {
            errors.add("La nueva contraseña debe ser distinta de la actual.");
        }

        if (!errors.isEmpty()) throw new ValidationException(errors);

        userDao.updatePassword(userId, PasswordHasher.hash(next));
    }
}
