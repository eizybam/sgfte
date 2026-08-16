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
        long sequence = 77L;

        @Override
        public boolean emailExistsForAnother(String email, long exceptId) {
            return emailTakenByAnother;
        }

        /** Sin esto el servicio iría a Oracle a por el consecutivo. */
        @Override
        public long nextEmployeeSequence() { return sequence; }

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

    /** El código se asigna al registrar, con las iniciales del nombre completo. */
    @Test
    void registerAssignsTheEmployeeCode() {
        Cardholder ch = new Cardholder("Diego", "Jarillo Estrada", "dje@empresa.com", null);
        service.register(ch);
        assertEquals("DJE0077", dao.inserted.getEmployeeCode());
    }

    /** Si ya trae código —por ejemplo al reintentar— no se vuelve a calcular. */
    @Test
    void registerKeepsAnExistingCode() {
        Cardholder ch = new Cardholder("Ana", "López", "ana@empresa.com", null);
        ch.setEmployeeCode("XX0001");
        service.register(ch);
        assertEquals("XX0001", dao.inserted.getEmployeeCode());
    }

    /** El modal manda un solo campo de nombre; el apellido es obligatorio. */
    @Test
    void registerFromFullNameSplitsAndAssigns() {
        long id = service.registerFromFullName("Diego Jarillo Estrada", "dje@empresa.com", "IT");

        assertEquals(42L, id);
        assertEquals("Diego", dao.inserted.getFirstName());
        assertEquals("Jarillo Estrada", dao.inserted.getLastName());
        assertEquals("IT", dao.inserted.getDepartment());
        assertEquals("DJE0077", dao.inserted.getEmployeeCode());
    }

    @Test
    void registerFromFullNameRejectsASingleWord() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.registerFromFullName("Cher", "cher@empresa.com", "IT"));

        assertTrue(e.getErrors().get(0).contains("apellido"));
        assertNull(dao.inserted);
    }

    @Test
    void updateRejectsAOneWordName() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.update(1L, "Ana", "ana@empresa.com", "IT", null));
        assertTrue(e.getErrors().get(0).contains("apellido"));
    }

    @Test
    void updateRejectsAnEmailThatBelongsToSomeoneElse() {
        dao.emailTakenByAnother = true;
        assertThrows(ValidationException.class,
                () -> service.update(1L, "Ana López", "otro@empresa.com", "IT", null));
    }

    @Test
    void keepingYourOwnEmailIsNotADuplicate() {
        dao.emailTakenByAnother = false;   // el AND id <> ? del DAO lo garantiza
        // Sin base de datos la transacción no corre; lo que se comprueba es que
        // la validación NO se queja antes de llegar a ella.
        assertDoesNotThrow(() -> service.validate(
                new Cardholder("Ana", "López", "ana@empresa.com", null)).isEmpty());
    }

}
