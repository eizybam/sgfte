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
                + "(account_id, movement_type, amount, related_account_id, description, card_id) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
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
            // Sólo un consumo trae tarjeta. Si alguien la pusiera en otro tipo
            // de movimiento, la base lo rechaza (chk_mov_card_only_withdrawal),
            // y si la tarjeta no fuera de esta cuenta, tampoco pasa: la llave
            // foránea va sobre el par (card_id, account_id).
            if (m.getCardId() == null) {
                ps.setNull(6, java.sql.Types.NUMERIC);
            } else {
                ps.setLong(6, m.getCardId());
            }
            ps.executeUpdate();
        }
    }
}
