package mx.sgfte.core.concentrator;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Read-only lookups of active accounts for the dispersion screen. */
public class AccountLookupDao {

    public List<AccountOption> findActiveForSelect() {
        String sql = "SELECT a.id, a.balance, c.first_name, c.last_name, cat.name AS purpose "
                + "FROM account a "
                + "JOIN cardholder c ON c.id = a.cardholder_id "
                + "JOIN category  cat ON cat.id = a.category_id "
                + "WHERE a.status = 'ACTIVE' "
                + "ORDER BY c.last_name, cat.name";
        List<AccountOption> options = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String label = rs.getString("last_name") + ", " + rs.getString("first_name")
                        + " — " + rs.getString("purpose");
                options.add(new AccountOption(rs.getLong("id"), label, rs.getBigDecimal("balance")));
            }
            return options;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading accounts for dispersion", e);
        }
    }
}
