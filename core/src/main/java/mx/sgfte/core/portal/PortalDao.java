package mx.sgfte.core.portal;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Read-only queries for the employee area, ALWAYS scoped by cardholder_id.
 *
 * Security note — read this before adding a method here:
 *
 *   Every query takes cardholderId as a parameter and puts it in the WHERE
 *   clause. Ownership is therefore enforced by the query itself, not by an
 *   "if" the caller might forget. Asking for an account that belongs to someone
 *   else simply returns no rows, exactly as if it did not exist.
 *
 *   The cardholderId always comes from the session (SessionUser), never from a
 *   request parameter. Without this, an employee could change ?id= in the URL
 *   and read a colleague's balance and movements — the classic IDOR bug.
 *
 * Do not add a findById(accountId) without the cardholder filter "just for
 * convenience". That is how the hole gets reopened.
 */
public class PortalDao {

    private static final String ACCOUNT_COLUMNS =
            "a.id, a.account_number, a.category_id, a.balance, cat.name AS purpose, "
          + "(SELECT COUNT(*) FROM card k WHERE k.account_id = a.id AND k.status = 'ACTIVE') AS active_cards ";

    /** Every active account owned by this cardholder, richest purpose first by name. */
    public List<PortalAccount> findAccounts(long cardholderId) {
        String sql = "SELECT " + ACCOUNT_COLUMNS
                + "FROM account a "
                + "JOIN category cat ON cat.id = a.category_id "
                + "WHERE a.cardholder_id = ? AND a.status = 'ACTIVE' "
                + "ORDER BY cat.name";
        List<PortalAccount> accounts = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    accounts.add(mapAccount(rs));
                }
            }
            return accounts;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading the cardholder's accounts", e);
        }
    }

    /**
     * One account, but only if this cardholder owns it.
     * Returns empty both when the account does not exist and when it belongs to
     * somebody else — the caller cannot tell the difference, which is the point.
     */
    public Optional<PortalAccount> findAccount(long cardholderId, long accountId) {
        String sql = "SELECT " + ACCOUNT_COLUMNS
                + "FROM account a "
                + "JOIN category cat ON cat.id = a.category_id "
                + "WHERE a.id = ? AND a.cardholder_id = ? AND a.status = 'ACTIVE'";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setLong(2, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapAccount(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading the account", e);
        }
    }

    /** Cheap ownership check for write paths (transfers), where we don't need the row. */
    public boolean owns(long cardholderId, long accountId) {
        String sql = "SELECT 1 FROM account WHERE id = ? AND cardholder_id = ? AND status = 'ACTIVE'";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.setLong(2, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking account ownership", e);
        }
    }

    /**
     * Colleagues' accounts that share the purpose of the given source account
     * (business rule RN-07), excluding the employee's own accounts.
     *
     * The purpose is resolved through a subquery that is itself scoped by
     * cardholder_id: if the source account is not owned by this cardholder the
     * subquery yields NULL, the comparison matches nothing, and the dropdown
     * comes back empty. No separate guard needed.
     */
    public List<PeerOption> findPeersForTransfer(long cardholderId, long sourceAccountId) {
        String sql = "SELECT a.id, a.account_number, c.first_name, c.last_name "
                + "FROM account a "
                + "JOIN cardholder c ON c.id = a.cardholder_id "
                + "WHERE a.status = 'ACTIVE' "
                + "  AND a.cardholder_id <> ? "
                + "  AND a.category_id = (SELECT s.category_id FROM account s "
                + "                       WHERE s.id = ? AND s.cardholder_id = ? AND s.status = 'ACTIVE') "
                + "ORDER BY c.last_name, c.first_name";
        List<PeerOption> peers = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            ps.setLong(2, sourceAccountId);
            ps.setLong(3, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String label = rs.getString("first_name") + " " + rs.getString("last_name")
                            + " — " + rs.getString("account_number");
                    peers.add(new PeerOption(rs.getLong("id"), label));
                }
            }
            return peers;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading peer accounts for transfer", e);
        }
    }

    private PortalAccount mapAccount(ResultSet rs) throws SQLException {
        return new PortalAccount(
                rs.getLong("id"),
                rs.getString("account_number"),
                rs.getLong("category_id"),
                rs.getString("purpose"),
                rs.getBigDecimal("balance"),
                rs.getInt("active_cards"));
    }
}
