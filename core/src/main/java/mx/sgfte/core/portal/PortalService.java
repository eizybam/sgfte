package mx.sgfte.core.portal;

import mx.sgfte.core.cards.Card;
import mx.sgfte.core.cards.CardDao;
import mx.sgfte.core.movements.Movement;
import mx.sgfte.core.movements.MovementQueryDao;
import mx.sgfte.core.purchases.PurchaseService;
import mx.sgfte.core.transfers.TransferService;
import mx.sgfte.core.users.ValidationException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business logic for the employee area (HU-07, HU-08).
 *
 * This service owns exactly one rule the rest of the system does not have:
 * an employee may only touch their OWN accounts. Everything else is delegated —
 * the money itself still moves through TransferService, so the transactional
 * guarantees and the same-purpose rule (RN-07) live in one place only and are
 * not duplicated here.
 *
 * Read the ownership check as: resolve the id against the session's cardholder
 * FIRST, then do the work. Never the other way round.
 */
public class PortalService {

    private final PortalDao portalDao;
    private final CardDao cardDao;
    private final MovementQueryDao movementQueryDao;
    private final TransferService transferService;
    private final PurchaseService purchaseService;

    public PortalService() {
        this(new PortalDao(), new CardDao(), new MovementQueryDao(),
                new TransferService(), new PurchaseService());
    }

    // Constructor for tests (inject fakes).
    public PortalService(PortalDao portalDao, CardDao cardDao,
                         MovementQueryDao movementQueryDao, TransferService transferService) {
        this(portalDao, cardDao, movementQueryDao, transferService, new PurchaseService());
    }

    public PortalService(PortalDao portalDao, CardDao cardDao, MovementQueryDao movementQueryDao,
                         TransferService transferService, PurchaseService purchaseService) {
        this.portalDao = portalDao;
        this.cardDao = cardDao;
        this.movementQueryDao = movementQueryDao;
        this.transferService = transferService;
        this.purchaseService = purchaseService;
    }

    /** The employee's accounts. Empty list is a normal state, not an error. */
    public List<PortalAccount> myAccounts(long cardholderId) {
        return portalDao.findAccounts(cardholderId);
    }

    /** Sum of every account balance — what the home screen shows as "total". */
    public BigDecimal totalBalance(List<PortalAccount> accounts) {
        return accounts.stream()
                .map(PortalAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * One of the employee's accounts.
     * @throws AccountNotOwnedException if the id is not theirs (or does not exist)
     */
    public PortalAccount myAccount(long cardholderId, long accountId) {
        return portalDao.findAccount(cardholderId, accountId)
                .orElseThrow(() -> new AccountNotOwnedException(cardholderId, accountId));
    }

    /** Cards of an account the employee owns. Ownership is re-checked here. */
    public List<Card> cardsOf(long cardholderId, long accountId) {
        requireOwnership(cardholderId, accountId);
        return cardDao.findByAccount(accountId);
    }

    /** Every card the employee holds, across all of their accounts — "Mis tarjetas". */
    public List<PortalCard> myCards(long cardholderId) {
        return portalDao.findCards(cardholderId);
    }

    /** Ledger of an account the employee owns, newest first. */
    public List<Movement> movementsOf(long cardholderId, long accountId) {
        requireOwnership(cardholderId, accountId);
        return movementQueryDao.findByAccount(accountId);
    }

    /** Legal destinations for a P2P transfer: same purpose, someone else's. */
    public List<PeerOption> peersFor(long cardholderId, long sourceAccountId) {
        return portalDao.findPeersForTransfer(cardholderId, sourceAccountId);
    }

    /**
     * P2P transfer started by the employee (HU-07).
     *
     * The ONLY thing added on top of TransferService is the ownership check on
     * the source account — without it, an employee could post someone else's
     * accountId as the source and drain their balance. Everything after that
     * (same purpose, sufficient funds, atomicity, ledger rows) is TransferService's
     * job and is not repeated here.
     *
     * The destination is deliberately NOT ownership-checked: sending money to a
     * colleague is the whole point of the feature. TransferService still refuses
     * if the purposes differ.
     */
    public void transfer(long cardholderId, Long sourceId, Long destId,
                         BigDecimal amount, String description) {
        if (sourceId == null || !portalDao.owns(cardholderId, sourceId)) {
            throw new AccountNotOwnedException(cardholderId, sourceId == null ? -1 : sourceId);
        }
        transferService.transfer(sourceId, destId, amount, description);
    }

    /**
     * "Hacer un gasto": simulates a real purchase against an account the
     * employee owns. Same ownership guard as transfer() — without it, an
     * employee could post someone else's accountId and drain their balance
     * through a fake purchase instead of a fake transfer.
     */
    /**
     * Un gasto ocurre CON UNA TARJETA.
     *
     * Hasta aquí sólo se comprobaba la cuenta, así que se podía "pagar" desde
     * una cuenta sin plásticos y una tarjeta bloqueada seguía pagando. La
     * tarjeta pasa a ser parte de la operación, y su propiedad se comprueba en
     * la misma consulta que su estado.
     */
    public void spend(long cardholderId, long accountId, long cardId,
                      BigDecimal amount, String merchant) {
        requireOwnership(cardholderId, accountId);
        if (!portalDao.cardUsable(cardId, accountId, cardholderId)) {
            throw new ValidationException(List.of(
                    "La tarjeta no está activa o no pertenece a esa cuenta."));
        }
        purchaseService.spend(accountId, amount, merchant);
    }

    /** Bloqueo desde el portal: el empleado se protege sin esperar a nadie. */
    public void blockMyCard(long cardholderId, long cardId) {
        if (!portalDao.blockOwnCard(cardId, cardholderId)) {
            throw new ValidationException(List.of("La tarjeta no está disponible para bloquear."));
        }
    }

    public void unblockMyCard(long cardholderId, long cardId) {
        if (!portalDao.unblockOwnCard(cardId, cardholderId)) {
            throw new ValidationException(List.of("La tarjeta no está bloqueada."));
        }
    }

    private void requireOwnership(long cardholderId, long accountId) {
        if (!portalDao.owns(cardholderId, accountId)) {
            throw new AccountNotOwnedException(cardholderId, accountId);
        }
    }

    /** The "ACTIVIDAD RECIENTE" panel: eight rows fit the frame. */
    public List<PortalActivity> recentActivity(long cardholderId) {
        return portalDao.findRecentActivity(cardholderId, 8);
    }

    /**
     * "+2.4% vs mes anterior": how the total compares with the 1st of the month.
     *
     * Returns null —rendered as a dash— when there is nothing to compare
     * against. With an opening balance of zero every change is infinite, and a
     * card that reports "+∞%" is worse than one that admits it cannot say.
     */
    public java.math.BigDecimal monthChangePercent(long cardholderId, java.math.BigDecimal total) {
        if (total == null) return null;
        java.math.BigDecimal opening = total.subtract(portalDao.netThisMonth(cardholderId));
        if (opening.signum() <= 0) return null;
        return total.subtract(opening)
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(opening, 1, java.math.RoundingMode.HALF_UP);
    }

    public List<PortalActivity> accountActivity(long cardholderId, long accountId) {
        return portalDao.findAccountActivity(cardholderId, accountId, 8);
    }

    /**
     * Eligible destinations keyed by MY account id, ready for the pop-up.
     *
     * The DAO groups by category because that is what the rule keys on; this
     * re-keys by account so the form can look up destinations directly from the
     * source the employee picked.
     */
    public java.util.Map<Long, List<PeerOption>> peersByAccount(long cardholderId) {
        java.util.Map<Long, List<PeerOption>> byCategory =
                portalDao.findPeersByCategory(cardholderId);

        java.util.Map<Long, List<PeerOption>> byAccount = new java.util.LinkedHashMap<>();
        for (PortalAccount account : myAccounts(cardholderId)) {
            byAccount.put(account.getId(),
                    byCategory.getOrDefault(account.getCategoryId(), List.of()));
        }
        return byAccount;
    }
}
