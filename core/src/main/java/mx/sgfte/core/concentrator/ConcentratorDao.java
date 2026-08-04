package mx.sgfte.core.concentrator;

import mx.sgfte.core.shared.db.Db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data access for the Concentrator (singleton row: singleton = 'Y').
 * Methods that take a Connection join the Service's transaction (they do NOT
 * open their own). Methods without it are standalone read/updates.
 */
public class ConcentratorDao {

    /** Reads the Concentrator (read-only, own connection). */
    public ConcentratorAccount findSingleton() {
        String sql = "SELECT id, name, balance FROM concentrator_account WHERE singleton = 'Y'";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                ConcentratorAccount ca = new ConcentratorAccount();
                ca.setId(rs.getLong("id"));
                ca.setName(rs.getString("name"));
                ca.setBalance(rs.getBigDecimal("balance"));
                return ca;
            }
            throw new IllegalStateException("Concentrator account not found (check the schema seed)");
        } catch (SQLException e) {
            throw new RuntimeException("Error reading the Concentrator", e);
        }
    }

    /**
     * Adds money to the Concentrator (company funding).
     *
     * Now a transaction rather than a lone UPDATE: the balance change and its
     * ledger entry have to land together, or funding would once again be able to
     * happen without leaving a trace.
     */
    public void fund(BigDecimal amount, String actor) {
        String sql = "UPDATE concentrator_account SET balance = balance + ? WHERE singleton = 'Y'";
        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setBigDecimal(1, amount);
                if (ps.executeUpdate() != 1) {
                    throw new IllegalStateException("Could not fund the Concentrator");
                }
                record(c, "FUNDING", amount, actor);
                c.commit();
            } catch (RuntimeException | SQLException e) {
                c.rollback();
                throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error funding the Concentrator", e);
        }
    }

    /**
     * Debits the Concentrator inside the Service's transaction.
     * The "balance >= ?" guard prevents going negative: with no funds it affects
     * 0 rows and we return false so the Service can roll back.
     */
    public boolean debit(Connection conn, BigDecimal amount) throws SQLException {
        String sql = "UPDATE concentrator_account SET balance = balance - ? "
                + "WHERE singleton = 'Y' AND balance >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setBigDecimal(2, amount);
            if (ps.executeUpdate() != 1) return false;   // no había saldo
        }
        record(conn, "DISPERSION", amount, null);
        return true;
    }

    /** Credits the Concentrator inside a transaction (used by reintegration, M4). */
    public void credit(Connection conn, BigDecimal amount) throws SQLException {
        String sql = "UPDATE concentrator_account SET balance = balance + ? WHERE singleton = 'Y'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.executeUpdate();
        }
        record(conn, "REINTEGRATION", amount, null);
    }

    /**
     * Writes the ledger entry for a balance change.
     *
     * It lives here, next to the three methods that touch the balance, so that
     * no future caller can move Concentrator money without leaving a record —
     * putting it in the services would have left that up to whoever writes the
     * next one. Always on the caller's connection, so the entry commits with the
     * money or not at all.
     *
     * balance_after is read back inside the same transaction rather than
     * computed, so it is the real post-condition and not an assumption.
     */
    private void record(Connection conn, String type, BigDecimal amount, String actor)
            throws SQLException {

        BigDecimal balanceAfter;
        try (PreparedStatement ps = conn.prepareStatement(
                     "SELECT balance FROM concentrator_account WHERE singleton = 'Y'");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalStateException("Concentrator account not found");
            balanceAfter = rs.getBigDecimal(1);
        }

        String sql = "INSERT INTO concentrator_movement "
                   + "(movement_type, amount, balance_after, actor) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            ps.setBigDecimal(2, amount);
            ps.setBigDecimal(3, balanceAfter);
            ps.setString(4, actor);
            ps.executeUpdate();
        }
    }

    /**
     * The Concentrator balance as of a moment in time: the balance_after of the
     * last movement before it.
     *
     * Returns null when the ledger has nothing that old — with no baseline it is
     * better for the screen to show no comparison than to invent a 0 and report
     * an infinite change.
     */
    public BigDecimal balanceAsOf(java.time.LocalDateTime moment) {
        String sql = "SELECT balance_after FROM concentrator_movement "
                   + "WHERE created_at < ? ORDER BY created_at DESC, id DESC "
                   + "FETCH FIRST 1 ROWS ONLY";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(moment));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading the Concentrator history", e);
        }
    }
}
