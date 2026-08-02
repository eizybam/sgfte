package mx.sgfte.core.analytics;

import mx.sgfte.core.shared.db.Db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only aggregates for the admin dashboard and analytics endpoint. */
public class DashboardDao {

    public BigDecimal concentratorBalance() {
        return scalarDecimal("SELECT balance FROM concentrator_account WHERE singleton = 'Y'");
    }

    public BigDecimal totalInAccounts() {
        return scalarDecimal("SELECT NVL(SUM(balance), 0) FROM account WHERE status = 'ACTIVE'");
    }

    public int activeCardholders() { return scalarInt("SELECT COUNT(*) FROM cardholder WHERE status = 'ACTIVE'"); }
    public int activeAccounts()    { return scalarInt("SELECT COUNT(*) FROM account WHERE status = 'ACTIVE'"); }
    public int activeCards()       { return scalarInt("SELECT COUNT(*) FROM card WHERE status = 'ACTIVE'"); }

    /** movement_type -> count, for the chart. */
    public Map<String, Integer> movementCountsByType() {
        String sql = "SELECT movement_type, COUNT(*) AS n FROM account_movement GROUP BY movement_type ORDER BY movement_type";
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getString("movement_type"), rs.getInt("n"));
            return map;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading movement counts", e);
        }
    }

    private BigDecimal scalarDecimal(String sql) {
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new RuntimeException("Error in query: " + sql, e);
        }
    }

    private int scalarInt(String sql) {
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error in query: " + sql, e);
        }
    }
}
