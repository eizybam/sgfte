package mx.sgfte.core.portal;

import mx.sgfte.core.cards.Card;
import mx.sgfte.core.cards.CardDao;
import mx.sgfte.core.movements.Movement;
import mx.sgfte.core.movements.MovementQueryDao;
import mx.sgfte.core.transfers.TransferDao;
import mx.sgfte.core.transfers.TransferService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The rule this whole class exists to enforce: an employee may only touch their
 * OWN accounts (RNF-05).
 *
 * These are the automated counterpart of section 3 of
 * docs/QA_PORTAL_TARJETAHABIENTE.md — the cross-employee isolation cases. They
 * matter because the failure mode is silent: nothing crashes when an employee
 * reads a colleague's balance, it just quietly works.
 *
 * Cardholder 1 is "us"; cardholder 2 is the colleague. Account 10 belongs to us,
 * account 20 does not.
 */
class PortalServiceTest {

    private static final long ME = 1L;
    private static final long MY_ACCOUNT = 10L;
    private static final long SOMEONE_ELSES_ACCOUNT = 20L;

    /** Fake DAO that only ever admits ownership of MY_ACCOUNT by ME. */
    private static class FakePortalDao extends PortalDao {
        @Override
        public boolean owns(long cardholderId, long accountId) {
            return cardholderId == ME && accountId == MY_ACCOUNT;
        }

        @Override
        public Optional<PortalAccount> findAccount(long cardholderId, long accountId) {
            return owns(cardholderId, accountId)
                    ? Optional.of(account(MY_ACCOUNT, "Gasolina", "1000.00"))
                    : Optional.empty();
        }

        @Override
        public List<PortalAccount> findAccounts(long cardholderId) {
            if (cardholderId != ME) return List.of();
            return List.of(account(MY_ACCOUNT, "Gasolina", "1000.00"),
                           account(11L, "Alimentos", "250.50"));
        }
    }

    private static PortalAccount account(long id, String purpose, String balance) {
        return new PortalAccount(id, "GAS-00001", 1L, purpose, new BigDecimal(balance), 2);
    }

    /** Records whether the real transfer was allowed to run. */
    private static class SpyTransferService extends TransferService {
        boolean called = false;

        SpyTransferService() {
            super(new TransferDao(), new mx.sgfte.core.accounts.AccountDao(),
                  new mx.sgfte.core.movements.MovementDao());
        }

        @Override
        public void transfer(Long sourceId, Long destId, BigDecimal amount, String description) {
            called = true;   // deliberately does NOT touch the database
        }
    }

    private static class FakeCardDao extends CardDao {
        @Override
        public List<Card> findByAccount(long accountId) {
            return List.of(new Card(accountId, "PHYSICAL", "**** **** **** 4821"));
        }
    }

    private static class FakeMovementQueryDao extends MovementQueryDao {
        @Override
        public List<Movement> findByAccount(long accountId) {
            List<Movement> list = new ArrayList<>();
            list.add(new Movement(accountId, "DEPOSIT", new BigDecimal("500.00"), null, "alta"));
            return list;
        }
    }

    private final SpyTransferService transferService = new SpyTransferService();
    private final PortalService service = new PortalService(
            new FakePortalDao(), new FakeCardDao(), new FakeMovementQueryDao(), transferService);

    // ---- Lectura ---------------------------------------------------------

    @Test
    void listsOnlyMyAccounts() {
        assertEquals(2, service.myAccounts(ME).size());
        assertTrue(service.myAccounts(99L).isEmpty(), "otro empleado no ve mis cuentas");
    }

    @Test
    void totalIsTheSumOfEveryBalance() {
        assertEquals(new BigDecimal("1250.50"), service.totalBalance(service.myAccounts(ME)));
    }

    @Test
    void totalOfNoAccountsIsZeroNotAnError() {
        assertEquals(BigDecimal.ZERO, service.totalBalance(List.of()));
    }

    @Test
    void canOpenMyOwnAccount() {
        assertEquals("Gasolina", service.myAccount(ME, MY_ACCOUNT).getPurpose());
    }

    /** CP: leer la cuenta de otro. Debe ser indistinguible de "no existe". */
    @Test
    void cannotOpenSomeoneElsesAccount() {
        assertThrows(AccountNotOwnedException.class,
                () -> service.myAccount(ME, SOMEONE_ELSES_ACCOUNT));
    }

    @Test
    void cannotListSomeoneElsesCards() {
        assertThrows(AccountNotOwnedException.class,
                () -> service.cardsOf(ME, SOMEONE_ELSES_ACCOUNT));
    }

    @Test
    void cannotReadSomeoneElsesMovements() {
        assertThrows(AccountNotOwnedException.class,
                () -> service.movementsOf(ME, SOMEONE_ELSES_ACCOUNT));
    }

    @Test
    void canReadMyOwnCardsAndMovements() {
        assertEquals(1, service.cardsOf(ME, MY_ACCOUNT).size());
        assertEquals(1, service.movementsOf(ME, MY_ACCOUNT).size());
    }

    // ---- Transferencia ---------------------------------------------------

    @Test
    void transferFromMyOwnAccountIsDelegated() {
        service.transfer(ME, MY_ACCOUNT, 99L, new BigDecimal("100.00"), "gasolina");
        assertTrue(transferService.called, "debe delegar en TransferService");
    }

    /**
     * The case that matters most: posting somebody else's accountId as the
     * source. It must be refused BEFORE TransferService is reached, or an
     * employee could drain a colleague's balance.
     */
    @Test
    void cannotTransferFromSomeoneElsesAccount() {
        assertThrows(AccountNotOwnedException.class,
                () -> service.transfer(ME, SOMEONE_ELSES_ACCOUNT, 99L,
                        new BigDecimal("500.00"), "robo"));

        assertFalse(transferService.called, "el dinero nunca debió llegar a moverse");
    }

    @Test
    void missingSourceIsRefusedWithoutTouchingTheTransfer() {
        assertThrows(AccountNotOwnedException.class,
                () -> service.transfer(ME, null, 99L, new BigDecimal("10.00"), null));
        assertFalse(transferService.called);
    }

    /**
     * The DESTINATION is deliberately not ownership-checked: sending money to a
     * colleague is the entire point of HU-07. The same-purpose rule still applies,
     * but that lives in TransferService, not here.
     */
    @Test
    void destinationBelongingToSomeoneElseIsAllowed() {
        service.transfer(ME, MY_ACCOUNT, SOMEONE_ELSES_ACCOUNT, new BigDecimal("50.00"), null);
        assertTrue(transferService.called);
    }
}
