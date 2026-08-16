package mx.sgfte.core.auth;

import mx.sgfte.core.users.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PasswordChangeTest {

    private static final String CURRENT = "Sgfte2026$";

    /** Fake: always returns the same user and remembers the saved hash. */
    private static class FakeUserDao extends UserDao {
        String savedHash;

        @Override
        public Optional<AppUser> findById(Long id) {
            AppUser u = new AppUser();
            u.setId(1L);
            u.setEmail("admin@empresa.com");
            u.setPasswordHash(PasswordHasher.hash(CURRENT));
            return Optional.of(u);
        }

        @Override
        public void updatePassword(long id, String passwordHash) { this.savedHash = passwordHash; }
    }

    private final FakeUserDao dao = new FakeUserDao();
    private final AuthService service = new AuthService(dao);

    @Test
    void changesThePasswordWhenEverythingIsRight() {
        service.changePassword(1L, CURRENT, "nuevaSegura1", "nuevaSegura1");
        assertNotNull(dao.savedHash);
        assertTrue(PasswordHasher.matches("nuevaSegura1", dao.savedHash));
    }

    @Test
    void aWrongCurrentPasswordIsRejected() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.changePassword(1L, "loQueSea", "nuevaSegura1", "nuevaSegura1"));
        assertTrue(e.getErrors().stream().anyMatch(m -> m.contains("actual")));
        assertNull(dao.savedHash);          // nothing was saved
    }

    @Test
    void aShortPasswordAndAMismatchAreBothReported() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.changePassword(1L, CURRENT, "corta", "otra"));
        assertEquals(2, e.getErrors().size());
    }

    @Test
    void theNewPasswordCannotBeTheOldOne() {
        assertThrows(ValidationException.class,
                () -> service.changePassword(1L, CURRENT, CURRENT, CURRENT));
    }
}
