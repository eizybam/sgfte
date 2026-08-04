package mx.sgfte.core.portal;

import mx.sgfte.core.cards.Card;
import mx.sgfte.core.cards.CardDao;
import mx.sgfte.core.movements.Movement;
import mx.sgfte.core.movements.MovementQueryDao;
import mx.sgfte.core.transfers.TransferService;

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

    public PortalService() {
        this(new PortalDao(), new CardDao(), new MovementQueryDao(), new TransferService());
    }

    // Constructor for tests (inject fakes).
    public PortalService(PortalDao portalDao, CardDao cardDao,
                         MovementQueryDao movementQueryDao, TransferService transferService) {
        this.portalDao = portalDao;
        this.cardDao = cardDao;
        this.movementQueryDao = movementQueryDao;
        this.transferService = transferService;
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
}
