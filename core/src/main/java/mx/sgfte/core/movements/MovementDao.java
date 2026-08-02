package mx.sgfte.core.movements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Ledger writes. ALWAYS takes the Service's Connection so the INSERT is in the
 * SAME transaction as the money movement.
 * (Module 3 adds the read side here: findByAccount for the history view.)
 */
public class MovementDao {

    public void insert(Connection conn, Movement m) throws SQLException {
        String sql = "INSERT INTO account_movement "
                + "(account_id, movement_type, amount, related_account_id, description) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, m.getAccountId());
            ps.setString(2, m.getMovementType());
            ps.setBigDecimal(3, m.getAmount());
            if (m.getRelatedAccountId() == null) {
                ps.setNull(4, java.sql.Types.NUMERIC);
            } else {
                ps.setLong(4, m.getRelatedAccountId());
            }
            ps.setString(5, m.getDescription());
            ps.executeUpdate();
        }
    }
}
