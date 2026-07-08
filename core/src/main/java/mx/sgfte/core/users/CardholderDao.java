package mx.sgfte.core.users;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Persistence for cardholders (JDBC over Oracle). */
public class CardholderDao {

    /** True if a cardholder with this email already exists. */
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM cardholder WHERE email = ?";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking email", e);
        }
    }

    /** Inserts a cardholder and returns the generated id. */
    public long insert(Cardholder ch) {
        String sql = "INSERT INTO cardholder (first_name, last_name, email, phone) VALUES (?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"id"})) {
            ps.setString(1, ch.getFirstName());
            ps.setString(2, ch.getLastName());
            ps.setString(3, ch.getEmail());
            ps.setString(4, ch.getPhone());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return -1L;
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting cardholder", e);
        }
    }
}
