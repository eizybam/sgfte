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

    /** Adds money to the Concentrator (company funding). Single statement. */
    public void fund(BigDecimal amount) {
        String sql = "UPDATE concentrator_account SET balance = balance + ? WHERE singleton = 'Y'";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Could not fund the Concentrator");
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
            return ps.executeUpdate() == 1;   // true = there was enough balance
        }
    }

    /** Credits the Concentrator inside a transaction (used by reintegration, M4). */
    public void credit(Connection conn, BigDecimal amount) throws SQLException {
        String sql = "UPDATE concentrator_account SET balance = balance + ? WHERE singleton = 'Y'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.executeUpdate();
        }
    }
}
