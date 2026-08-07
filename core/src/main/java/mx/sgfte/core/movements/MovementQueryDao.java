package mx.sgfte.core.movements;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read side of the ledger: movement history for one account. */
public class MovementQueryDao {

    public List<Movement> findByAccount(long accountId) {
        String sql = "SELECT id, account_id, movement_type, amount, related_account_id, description, created_at "
                   + "FROM account_movement WHERE account_id = ? ORDER BY created_at DESC, id DESC";
        List<Movement> list = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Movement m = new Movement();
                    m.setId(rs.getLong("id"));
                    m.setAccountId(rs.getLong("account_id"));
                    m.setMovementType(rs.getString("movement_type"));
                    m.setAmount(rs.getBigDecimal("amount"));
                    long rel = rs.getLong("related_account_id");
                    m.setRelatedAccountId(rs.wasNull() ? null : rel);
                    m.setDescription(rs.getString("description"));
                    m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(m);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading movement history", e);
        }
    }
}
