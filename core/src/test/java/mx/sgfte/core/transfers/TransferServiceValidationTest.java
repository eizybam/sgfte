package mx.sgfte.core.transfers;

import mx.sgfte.core.accounts.AccountDao;
import mx.sgfte.core.movements.MovementDao;
import mx.sgfte.core.users.ValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Input validation for P2P transfers (HU-05 / RN-07).
 *
 * SCOPE — read this before adding cases here.
 *
 * TransferService.transfer() validates its arguments FIRST and only then calls
 * Db.getConnection(). Everything checked before that call can be tested here
 * with no database. Everything after it cannot: the connection is obtained from
 * a static factory, so there is no seam to inject a fake one.
 *
 * That means these rules are NOT covered by this class and must be verified as
 * integration cases against Oracle (see docs/QA_M3.md, cases T1, T2, T3, T6, T7):
 *   · same-purpose only        (needs account rows)
 *   · sufficient balance       (needs account rows)
 *   · atomicity / rollback     (needs a real transaction)
 *
 * Making those automatable means giving the service an injectable connection
 * source. That is a change to money-moving code and was deliberately not done
 * this close to delivery — it is recorded as a recommendation in MATRIZ_QA.md.
 */
class TransferServiceValidationTest {

    /** DAOs are irrelevant here: validation fails before any of them is used. */
    private final TransferService service =
            new TransferService(new TransferDao(), new AccountDao(), new MovementDao());

    private static final BigDecimal OK_AMOUNT = new BigDecimal("100.00");

    @Test
    void sourceIsRequired() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.transfer(null, 2L, OK_AMOUNT, null));
        assertTrue(e.getErrors().stream().anyMatch(m -> m.contains("origen")));
    }

    @Test
    void destinationIsRequired() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.transfer(1L, null, OK_AMOUNT, null));
        assertTrue(e.getErrors().stream().anyMatch(m -> m.contains("destino")));
    }

    /** T4: origen = destino. */
    @Test
    void cannotTransferToTheSameAccount() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.transfer(5L, 5L, OK_AMOUNT, null));
        assertTrue(e.getErrors().stream().anyMatch(m -> m.contains("misma cuenta")));
    }

    /** T5: monto <= 0. Cero incluido: mover $0 no es una operación válida. */
    @Test
    void amountMustBePositive() {
        assertThrows(ValidationException.class,
                () -> service.transfer(1L, 2L, BigDecimal.ZERO, null));
        assertThrows(ValidationException.class,
                () -> service.transfer(1L, 2L, new BigDecimal("-5.00"), null));
    }

    @Test
    void amountIsRequired() {
        assertThrows(ValidationException.class, () -> service.transfer(1L, 2L, null, null));
    }

    /** Todos los errores se reportan juntos, no de uno en uno. */
    @Test
    void everyProblemIsReportedInOneGo() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.transfer(null, null, BigDecimal.ZERO, null));
        assertEquals(3, e.getErrors().size(),
                "esperaba: falta origen, falta destino, monto no positivo");
    }
}
