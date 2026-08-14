package mx.sgfte.core.deletion;

import mx.sgfte.core.cards.CardDao;
import mx.sgfte.core.concentrator.ConcentratorDao;
import mx.sgfte.core.movements.Movement;
import mx.sgfte.core.movements.MovementDao;
import mx.sgfte.core.shared.db.Db;
import mx.sgfte.core.users.ValidationException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Deletion with automatic reintegration (business rule 4).
 *
 * Deleting an ACCOUNT: its remaining balance returns to the Concentrator, its
 * cards are invalidated, and the account is deactivated — all in ONE transaction.
 * Deleting a CARDHOLDER: same thing for EVERY one of their accounts, then the
 * cardholder is deactivated — also in ONE transaction (all or nothing).
 *
 * We deactivate (status = INACTIVE), we don't hard-DELETE rows, so the ledger
 * and history stay intact (traceability).
 */
public class DeletionService {

    private final DeletionDao deletionDao;
    private final ConcentratorDao concentratorDao;
    private final CardDao cardDao;
    private final MovementDao movementDao;

    public DeletionService() {
        this(new DeletionDao(), new ConcentratorDao(), new CardDao(), new MovementDao());
    }

    public DeletionService(DeletionDao deletionDao, ConcentratorDao concentratorDao,
                           CardDao cardDao, MovementDao movementDao) {
        this.deletionDao = deletionDao;
        this.concentratorDao = concentratorDao;
        this.cardDao = cardDao;
        this.movementDao = movementDao;
    }

    /** Deletes one account, reintegrating its balance to the Concentrator. */
    public void deleteAccount(long accountId) {
        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal balance = deletionDao.activeBalanceForUpdate(conn, accountId);
                if (balance == null) {
                    throw new ValidationException(List.of("La cuenta no existe o ya está inactiva"));
                }
                reintegrateAndClose(conn, accountId, balance);
                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in deleteAccount transaction", e);
        }
    }

    /** Deletes a cardholder: reintegrates ALL their accounts, then deactivates them. */
    public void deleteCardholder(long cardholderId) {
        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!deletionDao.cardholderIsActive(conn, cardholderId)) {
                    throw new ValidationException(List.of("El tarjetahabiente no existe o ya está inactivo"));
                }
                for (Long accountId : deletionDao.activeAccountIds(conn, cardholderId)) {
                    BigDecimal balance = deletionDao.activeBalanceForUpdate(conn, accountId);
                    if (balance != null) {
                        reintegrateAndClose(conn, accountId, balance);
                    }
                }
                deletionDao.deactivateCardholder(conn, cardholderId);
                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in deleteCardholder transaction", e);
        }
    }

    public void activateCardholder(long cardholderId) {
        try (Connection conn = Db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (deletionDao.cardholderIsActive(conn, cardholderId)) {
                    throw new ValidationException(List.of("El tarjetahabiente ya se encuentra activo"));
                }
                deletionDao.activateCardholder(conn, cardholderId);
                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error in activateCardholder transaction", e);
        }
    }

    /**
     * The shared steps for one account: send balance back to the Concentrator (if any),
     * log a REINTEGRATION movement, invalidate its cards, deactivate the account.
     * Runs inside the caller's transaction.
     */
    private void reintegrateAndClose(Connection conn, long accountId, BigDecimal balance) throws SQLException {
        if (balance.signum() > 0) {
            concentratorDao.credit(conn, balance);                 // money back to Concentrator
            movementDao.insert(conn, new Movement(accountId, "REINTEGRATION", balance, null,
                    "Reintegración por eliminación"));             // ledger row (amount > 0)
        }
        cardDao.invalidateAllForAccount(conn, accountId);          // cards -> INACTIVE
        deletionDao.deactivateAccount(conn, accountId);            // account -> balance 0, INACTIVE
    }
}
