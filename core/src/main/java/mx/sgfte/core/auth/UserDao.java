package mx.sgfte.core.auth;
import mx.sgfte.core.shared.db.Db;
import oracle.jdbc.proxy.annotation.Pre;

import java.sql.*;
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

    /**
     * El usuario que puede iniciar sesión con ese correo.
     *
     * Un tarjetahabiente desactivado no puede entrar aunque su app_user siga
     * ACTIVE: el estado vive en cardholder, y aquí se consulta en lugar de
     * duplicarlo. Los admins no tienen cardholder_id, por eso el LEFT JOIN
     * y el "IS NULL".
     */
    public Optional<AppUser> findLoginByEmail(String email) {
        String sql = "SELECT u.id, u.email, u.password_hash, u.full_name, "
                + "       u.role, u.cardholder_id, u.status "
                + "  FROM app_user u "
                + "  LEFT JOIN cardholder ch ON ch.id = u.cardholder_id "
                + " WHERE u.email = ? "
                + "   AND (u.cardholder_id IS NULL OR ch.status = 'ACTIVE')";

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error looking up login by email", e);
        }
    }

    public Optional<AppUser> findById(Long id) {
        String sql = "SELECT id, email, password_hash, full_name, role, cardholder_id, status "
                + "FROM app_user WHERE id = ?";

        try (Connection c = Db.getConnection();
              PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error looking up user by id", e);
        }
    }

    public long insert(AppUser user) {
        String sql = "INSERT INTO app_user (email, password_hash, full_name, role, cardholder_id, status)"
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
              PreparedStatement ps = c.prepareStatement(sql, new String[]{"id"})) {
             ps.setString(1, user.getEmail());
             ps.setString(2, user.getPasswordHash());
             ps.setString(3, user.getFullName());
             ps.setString(4, user.getRole());
             if (user.getCardholderId() != null) {
                 ps.setLong(5, user.getCardholderId());
             } else {
                 ps.setNull(5, Types.NUMERIC);
             }
             ps.setString(6, user.getStatus());
             ps.executeUpdate();
             try (ResultSet keys = ps.getGeneratedKeys()) {
                 if (keys.next()) {
                     return keys.getLong(1);
                 }
             }
             throw new IllegalStateException("Insert succeded but no generated ID was returned");
         } catch (SQLException e) {
            throw new RuntimeException("Error inserting user", e);
        }
    }

    public void setPasswordAndActivate(long id, String passwordHash) {
        String sql = "UPDATE app_user SET password_hash = ?, status = 'ACTIVE' WHERE id = ?";
        try (Connection c = Db.getConnection();
              PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error setting password and activating user", e);
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
