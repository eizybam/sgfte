package mx.sgfte.core.accounts;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDao {

    /** True if the cardholder already has an account with this purpose */
    public boolean existForPurpose(Long cardholderId, long categoryId) {
        String sql = "SELECT 1 FROM account WHERE cardholder_id = ? AND category_id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            ps.setLong(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking account", e);
        }
    }

    public long insert(Account account){
        String sql = "INSERT INTO account (cardholder_id, category_id) VALUES (?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"id"})) {
            ps.setLong(1, account.getCardholderId());
            ps.setLong(2, account.getCategoryId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new IllegalStateException("Insert succeeded but no generated id was returned");
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting account", e);
        }
    }
}
