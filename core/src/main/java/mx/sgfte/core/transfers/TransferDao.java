package mx.sgfte.core.transfers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Small helper for the transfer transaction. */
public class TransferDao {

    /**
     * Returns the account's category_id (its "purpose") if the account is ACTIVE,
     * or null if it doesn't exist / is inactive. Used to enforce the same-purpose rule.
     * Runs inside the Service's transaction (takes the Connection).
     */
    public Long categoryIdIfActive(Connection conn, long accountId) throws SQLException {
        String sql = "SELECT category_id FROM account WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("category_id") : null;
            }
        }
    }
}
