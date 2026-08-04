package mx.sgfte.core.concentrator;

import mx.sgfte.core.accounts.AccountDao;
import mx.sgfte.core.movements.MovementDao;
import mx.sgfte.core.users.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concentrator funding (RF-05) and dispersion input validation (RF-06).
 *
 * ConcentratorService is fully testable: its DAO is injected. DispersionService
 * is only testable up to the point where it calls Db.getConnection() — see the
 * note in TransferServiceValidationTest for why, and MATRIZ_QA.md for which
 * cases are therefore covered manually instead.
 */
class ConcentratorServiceTest {

    private static class FakeConcentratorDao extends ConcentratorDao {
        BigDecimal funded;
        String actor;
        BigDecimal balance = new BigDecimal("1000000.00");

        @Override
        public void fund(BigDecimal amount, String actor) {
            this.funded = amount;
            this.actor = actor;
        }

        @Override
        public ConcentratorAccount findSingleton() {
            ConcentratorAccount a = new ConcentratorAccount();
            a.setId(1L);
            a.setName("Cuenta Concentradora");
            a.setBalance(balance);
            return a;
        }
    }

    private final FakeConcentratorDao dao = new FakeConcentratorDao();
    private final ConcentratorService service = new ConcentratorService(dao);

    @Test
    void readsTheSingletonBalance() {
        assertEquals(new BigDecimal("1000000.00"), service.getConcentrator().getBalance());
    }

    @Test
    void fundingPassesTheAmountThrough() {
        service.fund(new BigDecimal("2500.00"));
        assertEquals(new BigDecimal("2500.00"), dao.funded);
    }

    /** RN-08 en su forma más simple: no se fondea con montos no positivos. */
    @Test
    void fundingAmountMustBePositive() {
        assertThrows(ValidationException.class, () -> service.fund(BigDecimal.ZERO));
        assertThrows(ValidationException.class, () -> service.fund(new BigDecimal("-1.00")));
        assertThrows(ValidationException.class, () -> service.fund(null));
        assertNull(dao.funded, "ningún intento inválido debe llegar al DAO");
    }

    // ---- Dispersión: solo la validación previa a la transacción -------------

    private final DispersionService dispersion =
            new DispersionService(new ConcentratorDao(), new AccountDao(), new MovementDao());

    @Test
    void dispersionRequiresADestinationAccount() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> dispersion.disperse(null, new BigDecimal("100.00"), null));
        assertTrue(e.getErrors().stream().anyMatch(m -> m.contains("cuenta destino")));
    }

    @Test
    void dispersionAmountMustBePositive() {
        assertThrows(ValidationException.class,
                () -> dispersion.disperse(1L, BigDecimal.ZERO, null));
        assertThrows(ValidationException.class,
                () -> dispersion.disperse(1L, new BigDecimal("-10.00"), null));
        assertThrows(ValidationException.class,
                () -> dispersion.disperse(1L, null, null));
    }

    @Test
    void dispersionReportsEveryProblemAtOnce() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> dispersion.disperse(null, BigDecimal.ZERO, null));
        assertEquals(2, e.getErrors().size());
    }
}
