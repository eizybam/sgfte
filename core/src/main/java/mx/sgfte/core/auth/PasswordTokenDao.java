package mx.sgfte.core.auth;

import mx.sgfte.core.shared.db.Db;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class PasswordTokenDao {
    void insert(long appUserId, String token, LocalDateTime expiresAt) {
        String sql = "INSERT INTO password_token (app_user_id, token, expires_at) VALUES (?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, appUserId);
            ps.setString(2, token);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error INSERTING password token", e);
        }
    }

    Optional<PasswordToken> findByToken(String token) throws SQLException {
        String sql = "SELECT id, app_user_id, token, expires_at, used_at "
                + "FROM password_token where token = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Timestamp used = rs.getTimestamp("used_at");
                return Optional.of(new PasswordToken(
                        rs.getLong("id"),
                        rs.getLong("app_user_id"),
                        rs.getString("token"),
                        rs.getTimestamp("expires_at").toLocalDateTime(),
                        used == null ? null : used.toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error looking up password token", e);
        }
    }

    void markUsed(long id) {
        String sql = "UPDATE password_token SET used_at = SYSTIMESTAMP where id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error marking password token as used", e);
        }
    }
}
