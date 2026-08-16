package mx.sgfte.core.auth;

import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.notifications.NotificationService;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Issues and redeems the single-use links behind two flows that are really
 * the same mechanism: "create your password" (sent once, right after
 * CardholderService.register() creates the login) and "forgot your
 * password" (self-service, requested any time after). Both end at the same
 * set-password page — the token is the only thing that says which login it's
 * for, so nothing else needs to differ.
 */
public class PasswordTokenService {

    /*
      Activation gets a longer window than reset: it's mailed once, at
      registration, and the person might not open it same-day. A reset link
      is issued on demand right when someone is standing at the login page,
      so an hour is plenty — and sitting in an inbox for a shorter window
      matters more, since redeeming it hijacks a login that already works.
     */
    static final Duration ACTIVATION_TTL = Duration.ofHours(24);
    static final Duration RESET_TTL = Duration.ofHours(1);

    private static final String BASE_URL = env("SGFTE_APP_BASE_URL", "http://localhost:8080");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordTokenDao tokenDao;
    private final UserDao userDao;
    private final NotificationService notificationService;

    public PasswordTokenService() {
        this(new PasswordTokenDao(), new UserDao(), new NotificationService());
    }

    public PasswordTokenService(PasswordTokenDao tokenDao, UserDao userDao,
                                NotificationService notificationService) {
        this.tokenDao = tokenDao;
        this.userDao = userDao;
        this.notificationService = notificationService;
    }

    /**
     * Called once, right after CardholderService.register() creates the
     * PENDING login — mails the "create your password" link.
     */
    public void issueActivationToken(long appUserId, Long cardholderId, String email, String fullName) {
        String token = newToken();
        tokenDao.insert(appUserId, token, LocalDateTime.now().plus(ACTIVATION_TTL));

        String link = BASE_URL + "/set-password?token=" + token;
        String subject = "Activa tu cuenta SGFTE";
        String body = "Hola " + fullName + ",\n\n"
                + "Se creó tu acceso al portal de SGFTE. Para entrar, primero crea tu contraseña:\n\n"
                + "  " + link + "\n\n"
                + "Este enlace vale por 24 horas y sólo se puede usar una vez.\n\n"
                + "Si tú no esperabas este correo, ignóralo.\n\n"
                + "— Sistema de Gestión de Fondos y Tarjetas Empresariales";

        deliver(cardholderId, AuditEvent.ACCOUNT_ACTIVATION_SENT, email, subject, body);
    }

    /**
     * "¿Olvidaste tu contraseña?" — silent on purpose when the email doesn't
     * match anything, or matches an INACTIVE (deactivated) login: the caller
     * always shows the same generic "if that address exists, we sent a
     * link" message either way, so there's nothing here for it to leak.
     *
     * PENDING accounts ARE allowed through, not just ACTIVE ones — this
     * doubles as the recovery path for someone whose original activation
     * link already expired, without needing a second "resend activation"
     * feature.
     */
    public void requestPasswordReset(String email) {
        Optional<AppUser> found = userDao.findLoginByEmail(email);
        if (found.isEmpty() || "INACTIVE".equals(found.get().getStatus())) {
            return;
        }
        AppUser user = found.get();

        String token = newToken();
        tokenDao.insert(user.getId(), token, LocalDateTime.now().plus(RESET_TTL));

        String link = BASE_URL + "/set-password?token=" + token;
        String subject = "Restablece tu contraseña de SGFTE";
        String body = "Hola " + user.getFullName() + ",\n\n"
                + "Pediste restablecer tu contraseña. Este enlace vale por 1 hora y sólo se puede usar una vez:\n\n"
                + "  " + link + "\n\n"
                + "Si tú no lo pediste, ignora este correo — tu contraseña actual sigue funcionando.\n\n"
                + "— Sistema de Gestión de Fondos y Tarjetas Empresariales";

        deliver(user.getCardholderId(), AuditEvent.PASSWORD_RESET_REQUESTED, email, subject, body);
    }

    /** Loads the token behind the set-password form, if it's still good. */
    public Optional<PasswordToken> validate(String rawToken) throws SQLException {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        return tokenDao.findByToken(rawToken).filter(PasswordToken::isUsable);
    }

    /**
     * Redeems the token: sets the password, moves the login to ACTIVE
     * (covers both first activation and a later reset — a reset for an
     * already-ACTIVE user just leaves it ACTIVE), and burns the token so it
     * can't be replayed.
     *
     * @return false if the token is missing/expired/used/already redeemed
     *         between validate() and here (double submit, two tabs), or if
     *         the login has since been deactivated.
     */
    public boolean setPassword(String rawToken, String newPassword) throws SQLException {
        Optional<PasswordToken> found = validate(rawToken);
        if (found.isEmpty()) return false;

        PasswordToken pt = found.get();
        Optional<AppUser> userOpt = userDao.findById(pt.appUserId());
        if (userOpt.isEmpty() || "INACTIVE".equals(userOpt.get().getStatus())) return false;
        AppUser user = userOpt.get();

        userDao.setPasswordAndActivate(user.getId(), PasswordHasher.hash(newPassword));
        tokenDao.markUsed(pt.id());

        deliver(user.getCardholderId(), AuditEvent.PASSWORD_CHANGED, user.getEmail(),
                "Tu contraseña cambió",
                "Hola " + user.getFullName() + ",\n\n"
                        + "Tu contraseña de SGFTE se actualizó correctamente.\n\n"
                        + "Si tú no hiciste este cambio, contacta a tu administrador de inmediato.\n\n"
                        + "— Sistema de Gestión de Fondos y Tarjetas Empresariales");
        return true;
    }

    /**
     * notify() needs a cardholderId — it writes to the cardholder's own
     * Notificaciones page. Admins don't have one (cardholderId is null for
     * them), so they fall back to the plain send(): still emailed, still
     * audited, just not logged anywhere they'd see in-app.
     */
    private void deliver(Long cardholderId, AuditEvent event, String to, String subject, String body) {
        if (cardholderId != null) {
            notificationService.notify(cardholderId, event, to, subject, body);
        } else {
            notificationService.send(to, subject, body);
        }
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}