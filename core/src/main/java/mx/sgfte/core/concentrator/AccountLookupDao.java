package mx.sgfte.core.concentrator;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lookup DAO that provides active accounts for UI dropdowns.
 * Used by TransferServlet and HistorialServlet.
 */
public class AccountLookupDao {

    /**
     * Returns all active accounts formatted for HTML select dropdowns.
     * Each entry has id, a human-readable label, and balance.
     */
    public List<AccountOption> findActiveForSelect() {
        String sql = "SELECT a.id, a.account_number, a.balance, c.first_name || ' ' || c.last_name AS owner_name, cat.name AS category_name "
                   + "FROM account a "
                   + "JOIN cardholder c ON a.cardholder_id = c.id "
                   + "JOIN category cat ON a.category_id = cat.id "
                   + "WHERE a.status = 'ACTIVE' "
                   + "ORDER BY a.id";
        List<AccountOption> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                String label = rs.getString("account_number") + " – " + rs.getString("owner_name") + " (" + rs.getString("category_name") + ")";
                list.add(new AccountOption(id, label, rs.getBigDecimal("balance")));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading accounts for select", e);
        }
    }
}
