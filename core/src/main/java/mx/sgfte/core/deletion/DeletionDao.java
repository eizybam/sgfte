package mx.sgfte.core.deletion;

import mx.sgfte.core.shared.db.Db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for deletion/reintegration. Transactional helpers take the
 * Service's Connection; list helpers use their own connection (read-only).
 */
public class DeletionDao {

    // ---- transactional helpers (inside the Service tx) ----

    /**
     * Reads and LOCKS an active account's balance for the duration of the tx
     * (FOR UPDATE stops two deletions racing on the same account).
     * Returns null if the account doesn't exist or is already inactive.
     */
    public BigDecimal activeBalanceForUpdate(Connection conn, long accountId) throws SQLException {
        String sql = "SELECT balance FROM account WHERE id = ? AND status = 'ACTIVE' FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("balance") : null;
            }
        }
    }

    /** Empties and deactivates an account (balance 0 + INACTIVE). */
    public void deactivateAccount(Connection conn, long accountId) throws SQLException {
        String sql = "UPDATE account SET balance = 0, status = 'INACTIVE' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            ps.executeUpdate();
        }
    }

    /** IDs of the cardholder's ACTIVE accounts. */
    public List<Long> activeAccountIds(Connection conn, long cardholderId) throws SQLException {
        String sql = "SELECT id FROM account WHERE cardholder_id = ? AND status = 'ACTIVE'";
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getLong("id"));
            }
        }
        return ids;
    }

    /** True if the cardholder exists and is ACTIVE. */
    public boolean cardholderIsActive(Connection conn, long cardholderId) throws SQLException {
        String sql = "SELECT 1 FROM cardholder WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public void deactivateCardholder(Connection conn, long cardholderId) throws SQLException {
        String sql = "UPDATE cardholder SET status = 'INACTIVE' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            ps.executeUpdate();
        }
    }

    public void activateCardholder(Connection conn, long cardholderId) throws SQLException {
        String sql = "UPDATE cardholder SET status = 'ACTIVE' WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            ps.executeUpdate();
        }
    }

    // ---- read-only lists (own connection) ----

    public List<CardholderRow> findActiveCardholders() {
        String sql = "SELECT id, first_name, last_name FROM cardholder WHERE status = 'ACTIVE' ORDER BY last_name, first_name";
        List<CardholderRow> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new CardholderRow(rs.getLong("id"),
                        rs.getString("last_name") + ", " + rs.getString("first_name")));
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading cardholders", e);
        }
    }
}
