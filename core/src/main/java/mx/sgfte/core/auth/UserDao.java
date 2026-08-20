package mx.sgfte.core.auth;
import mx.sgfte.core.shared.db.Db;
import oracle.jdbc.proxy.annotation.Pre;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.Optional;


public class UserDao {

    /**
     * Lookup User by Email, ignoring case.
     *
     * "MAIL@empresa.com" y "mail@empresa.com" son el mismo buzón y el mismo
     * usuario. Comparando carácter a carácter no lo eran, y eso permitía dos
     * altas para la misma persona.
     */
    public Optional<AppUser> findByEmail(String email) {
        String sql = "SELECT id, email, password_hash, full_name, role, cardholder_id, status "
                + "FROM app_user WHERE UPPER(email) = UPPER(?)";

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
    /*
      El UPPER de los dos lados arregla además un fallo que se vivía como "no me
      deja entrar": quien escribía su correo con la primera letra en mayúscula
      —el teclado del móvil lo hace solo— no encontraba su propio usuario, y la
      pantalla contestaba "Credenciales inválidas" con la contraseña correcta.
     */
    public Optional<AppUser> findLoginByEmail(String email) {
        String sql = "SELECT u.id, u.email, u.password_hash, u.full_name, "
                + "       u.role, u.cardholder_id, u.status "
                + "  FROM app_user u "
                + "  LEFT JOIN cardholder ch ON ch.id = u.cardholder_id "
                + " WHERE UPPER(u.email) = UPPER(?) "
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

    public int updateIdentity(Connection conn, long cardholderId, String email, String fullName)
            throws SQLException {
        String sql = "UPDATE app_user SET email = ?, full_name = ? WHERE cardholder_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, fullName);
            ps.setLong(3, cardholderId);
            return ps.executeUpdate();
        }
    }


    /**
     * Changes only the password. Unlike setPasswordAndActivate, does NOT touch
     * the status: a suspended login stays suspended after the change.
     */
    public void updatePassword(long id, String passwordHash) {
        String sql = "UPDATE app_user SET password_hash = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating password", e);
        }
    }


    /* ---------------------------------------------------------------
       Foto de perfil (V14). Sólo la usa la pantalla de Ajustes.
       --------------------------------------------------------------- */

    /**
     * La foto y su tipo, tal como salen de la base.
     *
     * Record pelado a propósito: esto NO llega a ningún JSP —el servlet
     * escribe los bytes directamente en la respuesta—, así que no necesita los
     * getters JavaBean que EL 5.0 exigiría para leerlo con ${...}.
     */
    public record ProfilePhoto(byte[] bytes, String contentType) {}

    /** Guarda o reemplaza la foto de perfil. Una por usuario: no hay historial. */
    public void updatePhoto(long userId, byte[] bytes, String contentType) {
        String sql = "UPDATE app_user SET photo = ?, photo_type = ? WHERE id = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            /*
              setBinaryStream y no setBytes: es el camino explícito para un LOB,
              donde setBytes deja que el driver decida cómo bindear el valor.
              Vale la pena aunque hoy la imagen quepa de sobra — el día que se
              suba el tope de 2 MB, esto no hay que volver a tocarlo.
             */
            ps.setBinaryStream(1, new ByteArrayInputStream(bytes), bytes.length);
            ps.setString(2, contentType);
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving profile photo", e);
        }
    }

    /** La foto de ESE usuario, o vacío si no tiene. */
    public Optional<ProfilePhoto> findPhoto(long userId) {
        String sql = "SELECT photo, photo_type FROM app_user WHERE id = ? AND photo IS NOT NULL";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                Blob blob = rs.getBlob(1);
                return Optional.of(new ProfilePhoto(
                        blob.getBytes(1, (int) blob.length()), rs.getString(2)));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reading profile photo", e);
        }
    }

    /**
     * ¿Tiene foto? Sí o no, sin traérsela.
     *
     * La vista sólo necesita elegir entre pintar el <img> o las iniciales.
     * Resolverlo con findPhoto cargaría hasta 2 MB de BLOB en memoria en cada
     * carga de Ajustes para acabar tirándolos.
     */
    public boolean hasPhoto(long userId) {
        String sql = "SELECT 1 FROM app_user WHERE id = ? AND photo IS NOT NULL";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking profile photo", e);
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
