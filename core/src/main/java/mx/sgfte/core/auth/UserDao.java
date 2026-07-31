package mx.sgfte.core.auth;
import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;


public class UserDao {

    // Lookup User by Email
    public Optional<AppUser> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, full_name, role, cardholder_id, status "
                + "FROM app_user WHERE email = ?";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error looking up user by email", e);
        }
    }

    private AppUser mapRow(ResultSet resultSet) throws SQLException {
        AppUser user = new AppUser();
        user.setId(resultSet.getLong("id"));
        user.setEmail(resultSet.getString("email"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setFullName(resultSet.getString("full_name"));
        user.setRole(resultSet.getString("role"));
        long cardholderId = resultSet.getLong("cardholder_id");
        user.setCardholderId(resultSet.wasNull() ? null : cardholderId);
        user.setStatus(resultSet.getString("status"));
        return user;
    }
}
