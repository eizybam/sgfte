package mx.sgfte.core.auth;

import java.util.Optional;

public class AuthService {
    // A real-shaped hash used only to spend similar CPU time when the email
    // doesn't exist, so "no such user" and "wrong password" take about as long.
    private static final String DUMMY_HASH =
            "$2a$12$P6s7lL4fFj3Qcy0Szxi1g.mKC1LD6DlVRn6xKyiNTD7yK4am6T2ky";

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

        Optional<AppUser> found = userDao.findByEmail(email);
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
}
