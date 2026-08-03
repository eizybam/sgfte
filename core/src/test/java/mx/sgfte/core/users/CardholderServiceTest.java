package mx.sgfte.core.users;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation and registration rules for cardholders (RF-02).
 * Runs without a database or Tomcat: the DAO is replaced by a fake.
 */
class CardholderServiceTest {

    /** Fake DAO: answers whatever the test needs and records the insert. */
    private static class FakeCardholderDao extends CardholderDao {
        boolean emailTaken = false;
        Cardholder inserted;

        @Override
        public boolean emailExists(String email) { return emailTaken; }

        @Override
        public long insert(Cardholder ch) {
            this.inserted = ch;
            return 42L;
        }
    }

    private final FakeCardholderDao dao = new FakeCardholderDao();
    private final CardholderService service = new CardholderService(dao);

    @Test
    void validCardholderHasNoErrors() {
        Cardholder ch = new Cardholder("Ana", "López", "ana@empresa.com", "5551234567");
        assertTrue(service.validate(ch).isEmpty());
    }

    @Test
    void blankNameAndBadEmailAreReported() {
        Cardholder ch = new Cardholder("", "López", "not-an-email", null);
        List<String> errors = service.validate(ch);
        assertEquals(2, errors.size());
    }

    @Test
    void phoneIsOptional() {
        Cardholder ch = new Cardholder("Ana", "López", "ana@empresa.com", null);
        assertTrue(service.validate(ch).isEmpty());
    }

    @Test
    void registerReturnsTheNewId() {
        Cardholder ch = new Cardholder("Ana", "López", "ana@empresa.com", "5551234567");
        assertEquals(42L, service.register(ch));
        assertSame(ch, dao.inserted);
    }

    /** RF-02: "No se permiten usuarios duplicados por correo corporativo." */
    @Test
    void duplicateEmailIsRejectedAndNothingIsInserted() {
        dao.emailTaken = true;
        Cardholder ch = new Cardholder("Ana", "López", "ana@empresa.com", "5551234567");

        ValidationException e = assertThrows(ValidationException.class, () -> service.register(ch));

        assertTrue(e.getErrors().get(0).contains("correo"));
        assertNull(dao.inserted, "no debe insertarse nada si el correo ya existe");
    }

    /** A failed validation must stop before the DAO is ever reached. */
    @Test
    void invalidCardholderNeverReachesTheDao() {
        Cardholder ch = new Cardholder("", "", "bad", null);
        assertThrows(ValidationException.class, () -> service.register(ch));
        assertNull(dao.inserted);
    }
}
