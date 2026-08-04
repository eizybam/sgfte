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

    /**
     * Recent movements across EVERY account this cardholder owns, newest first.
     *
     * Joined through account so ownership is part of the query and not a check
     * the caller has to remember: there is no way to call this and get somebody
     * else's movements.
     *
     * Ordered by created_at AND id — movements written in one transaction share
     * a timestamp, and without the tie-break the rows shuffle between loads.
     */
    public List<PortalActivity> findRecentActivity(long cardholderId, int limit) {
        String sql = "SELECT m.movement_type, m.description, cat.name AS purpose, "
                   + "       m.amount, m.created_at "
                   + "  FROM account_movement m "
                   + "  JOIN account  a   ON a.id = m.account_id "
                   + "  JOIN category cat ON cat.id = a.category_id "
                   + " WHERE a.cardholder_id = ? "
                   + " ORDER BY m.created_at DESC, m.id DESC "
                   + " FETCH FIRST ? ROWS ONLY";
        List<PortalActivity> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp at = rs.getTimestamp("created_at");
                    rows.add(new PortalActivity(
                            rs.getString("movement_type"),
                            rs.getString("description"),
                            rs.getString("purpose"),
                            rs.getBigDecimal("amount"),
                            at == null ? null : at.toLocalDateTime()));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading the cardholder's activity", e);
        }
    }

    /**
     * Net movement across this cardholder's accounts since the 1st of the month.
     *
     * The balance at the start of the month is not stored anywhere, so the card
     * works backwards: today's total minus what moved this month. Inflows count
     * positive and outflows negative, which is what makes the subtraction give
     * the opening figure.
     */
    public java.math.BigDecimal netThisMonth(long cardholderId) {
        String sql = "SELECT NVL(SUM(CASE WHEN m.movement_type IN ('DEPOSIT', 'TRANSFER_IN') "
                   + "                    THEN m.amount ELSE -m.amount END), 0) "
                   + "  FROM account_movement m "
                   + "  JOIN account a ON a.id = m.account_id "
                   + " WHERE a.cardholder_id = ? "
                   + "   AND m.created_at >= TRUNC(SYSDATE, 'MM')";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : java.math.BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading the month's movement", e);
        }
    }

    /**
     * Recent movements of ONE account, if this cardholder owns it.
     *
     * The ownership check is part of the WHERE and not a separate query: an
     * account id in the URL cannot be swapped for somebody else's and still
     * return rows.
     */
    public List<PortalActivity> findAccountActivity(long cardholderId, long accountId, int limit) {
        String sql = "SELECT m.movement_type, m.description, cat.name AS purpose, "
                   + "       m.amount, m.created_at "
                   + "  FROM account_movement m "
                   + "  JOIN account  a   ON a.id = m.account_id "
                   + "  JOIN category cat ON cat.id = a.category_id "
                   + " WHERE a.cardholder_id = ? AND a.id = ? "
                   + " ORDER BY m.created_at DESC, m.id DESC "
                   + " FETCH FIRST ? ROWS ONLY";
        List<PortalActivity> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            ps.setLong(2, accountId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp at = rs.getTimestamp("created_at");
                    rows.add(new PortalActivity(
                            rs.getString("movement_type"),
                            rs.getString("description"),
                            rs.getString("purpose"),
                            rs.getBigDecimal("amount"),
                            at == null ? null : at.toLocalDateTime()));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading the account activity", e);
        }
    }

    /**
     * Every account this cardholder could transfer to, grouped by purpose.
     *
     * One query for the whole modal instead of one per account. The transfer
     * form is a pop-up now, so it cannot reload the page when the source
     * changes: it needs all the eligible destinations up front and filters them
     * on the spot. With no connection pool, N queries would be N round trips on
     * every dashboard load.
     *
     * Same two guards as findPeersForTransfer: never your own accounts, and only
     * purposes you actually hold — the P2P rule is same-category.
     */
    public java.util.Map<Long, List<PeerOption>> findPeersByCategory(long cardholderId) {
        String sql = "SELECT a.category_id, a.id, a.account_number, c.first_name, c.last_name "
                   + "  FROM account a "
                   + "  JOIN cardholder c ON c.id = a.cardholder_id "
                   + " WHERE a.status = 'ACTIVE' "
                   + "   AND a.cardholder_id <> ? "
                   + "   AND a.category_id IN (SELECT s.category_id FROM account s "
                   + "                          WHERE s.cardholder_id = ? AND s.status = 'ACTIVE') "
                   + " ORDER BY c.last_name, c.first_name";
        java.util.Map<Long, List<PeerOption>> byCategory = new java.util.LinkedHashMap<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            ps.setLong(2, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String label = rs.getString("last_name") + ", " + rs.getString("first_name")
                                 + " · " + rs.getString("account_number");
                    byCategory.computeIfAbsent(rs.getLong("category_id"), k -> new ArrayList<>())
                              .add(new PeerOption(rs.getLong("id"), label));
                }
            }
            return byCategory;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading the transfer destinations", e);
        }
    }
}
