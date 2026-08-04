package mx.sgfte.core.notifications;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * The only thing this module needs from the database: who to write to.
 *
 * The address is the cardholder's, taken from cardholder.email — the account
 * does not have one of its own, and the person who owns the account is the one
 * who cares that money arrived.
 */
class NotificationDao {

    /**
     * The account and its owner, or empty if the account is gone.
     *
     * Returns empty rather than throwing: this runs after the money already
     * moved, and a missing row must not turn a completed transfer into an error.
     */
    Optional<AccountParty> findParty(long accountId) {
        String sql = "SELECT a.id, a.account_number, a.balance, cat.name AS purpose, "
                   + "       ch.first_name, ch.last_name, ch.email "
                   + "  FROM account a "
                   + "  JOIN cardholder ch ON ch.id = a.cardholder_id "
                   + "  JOIN category  cat ON cat.id = a.category_id "
                   + " WHERE a.id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new AccountParty(
                        rs.getLong("id"),
                        rs.getString("account_number"),
                        rs.getString("purpose"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getBigDecimal("balance")));
            }
        } catch (SQLException e) {
            System.err.println("[NOTIFY] no se pudo leer la cuenta " + accountId + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
