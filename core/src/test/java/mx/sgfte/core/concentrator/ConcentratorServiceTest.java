package mx.sgfte.core.concentrator;

import mx.sgfte.core.accounts.AccountDao;
import mx.sgfte.core.movements.MovementDao;
import mx.sgfte.core.users.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dispersion input validation (RF-06).
 *
 * El fondeo (RF-05) ya no se prueba aquí: con V12 dejó de ser "validar que el
 * monto sea positivo" y pasó a exigir un depósito bancario con referencia.
 * Sus reglas viven en FundingServiceValidationTest.
 *
 * ConcentratorService is fully testable: its DAO is injected. DispersionService
 * is only testable up to the point where it calls Db.getConnection() — see the
 * note in TransferServiceValidationTest for why, and MATRIZ_QA.md for which
 * cases are therefore covered manually instead.
 */
class ConcentratorServiceTest {

    private static class FakeConcentratorDao extends ConcentratorDao {
        BigDecimal balance = new BigDecimal("1000000.00");

        /*
          Aquí el stub interceptaba fund(). Ese método ya no existe en el DAO:
          con V12 fondear dejó de ser "validar que el monto sea positivo" y pasó
          a exigir un depósito bancario con referencia. Sus reglas se prueban en
          FundingServiceValidationTest.
         */

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


    /** RN-08 en su forma más simple: no se fondea con montos no positivos. */

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
