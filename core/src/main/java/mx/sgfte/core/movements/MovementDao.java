package mx.sgfte.core.movements;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Write side of the ledger: inserts immutable movement records.
 * Reused by DispersionService (M1) and TransferService (M3).
 */
public class MovementDao {

    public void insert(Connection conn, Movement movement) throws SQLException {
        String sql = "INSERT INTO account_movement (account_id, movement_type, amount, related_account_id, description) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, movement.getAccountId());
            ps.setString(2, movement.getMovementType());
            ps.setBigDecimal(3, movement.getAmount());
            if (movement.getRelatedAccountId() != null) {
                ps.setLong(4, movement.getRelatedAccountId());
            } else {
                ps.setNull(4, Types.NUMERIC);
            }
            ps.setString(5, movement.getDescription());
            ps.executeUpdate();
        }
    }
}
