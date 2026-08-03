package mx.sgfte.core.accounts;

import mx.sgfte.core.users.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Account creation rules (RF-03). No database: the DAO is faked.
 *
 * The interesting parts are the duplicate-purpose rule and the generated
 * account_number, which is what users actually see ("GAS-48HSY").
 */
class AccountServiceTest {

    private static class FakeAccountDao extends AccountDao {
        boolean purposeTaken = false;
        int failInsertsBeforeSuccess = 0;
        final List<String> attemptedNumbers = new ArrayList<>();

        @Override
        public boolean existForPurpose(Long cardholderId, long categoryId) { return purposeTaken; }

        @Override
        public long insert(Account account) {
            attemptedNumbers.add(account.getAccountNumber());
            if (failInsertsBeforeSuccess > 0) {
                failInsertsBeforeSuccess--;
                throw new DuplicateAccountNumberException(account.getAccountNumber(), null);
            }
            return 7L;
        }
    }

    private final FakeAccountDao dao = new FakeAccountDao();
    private final AccountService service = new AccountService(dao);

    @Test
    void validSelectionHasNoErrors() {
        assertTrue(service.validate(new Account(1L, 2L)).isEmpty());
    }

    @Test
    void missingSelectionsAreReported() {
        assertEquals(2, service.validate(new Account(null, null)).size());
    }

    @Test
    void createReturnsTheNewId() {
        assertEquals(7L, service.create(new Account(1L, 2L), "Gasolina"));
    }

    /** The public code takes the first 3 letters of the purpose: Gasolina -> GAS-XXXXX */
    @Test
    void accountNumberUsesThePurposePrefix() {
        service.create(new Account(1L, 2L), "Gasolina");
        String number = dao.attemptedNumbers.get(0);

        assertTrue(number.startsWith("GAS-"), "esperaba prefijo GAS-, fue " + number);
        assertEquals(9, number.length(), "formato esperado: 3 letras + guion + 5 caracteres");
    }

    /** Accents must not leak into the code: "Viáticos" -> VIA, never "VIÁ". */
    @Test
    void accentsAreStrippedFromThePrefix() {
        service.create(new Account(1L, 2L), "Viáticos");
        assertTrue(dao.attemptedNumbers.get(0).startsWith("VIA-"));
    }

    /** A short purpose still yields a 3-letter prefix. */
    @Test
    void shortPurposeIsPadded() {
        service.create(new Account(1L, 2L), "Ok");
        assertEquals(3, dao.attemptedNumbers.get(0).indexOf('-'));
    }

    /** On a UNIQUE collision the service regenerates instead of failing. */
    @Test
    void collidingAccountNumberIsRetriedWithANewCode() {
        dao.failInsertsBeforeSuccess = 2;

        assertEquals(7L, service.create(new Account(1L, 2L), "Gasolina"));
        assertEquals(3, dao.attemptedNumbers.size(), "esperaba 2 colisiones + 1 éxito");
        assertNotEquals(dao.attemptedNumbers.get(0), dao.attemptedNumbers.get(1),
                "cada reintento debe generar un código distinto");
    }

    /** RF-03: one account per purpose per cardholder. */
    @Test
    void duplicatePurposeIsRejected() {
        dao.purposeTaken = true;

        ValidationException e = assertThrows(ValidationException.class,
                () -> service.create(new Account(1L, 2L), "Gasolina"));

        assertTrue(e.getErrors().get(0).contains("propósito"));
        assertTrue(dao.attemptedNumbers.isEmpty(), "no debe intentar insertar");
    }

    @Test
    void invalidAccountNeverReachesTheDao() {
        assertThrows(ValidationException.class,
                () -> service.create(new Account(null, null), "Gasolina"));
        assertTrue(dao.attemptedNumbers.isEmpty());
    }
}
