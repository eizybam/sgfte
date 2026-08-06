package mx.sgfte.core.notifications;

import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The cardholder-facing notification feed: every event a cardholder was
 * emailed about, scoped to them.
 *
 * Every query takes cardholderId and puts it in the WHERE clause, same rule as
 * PortalDao: ownership is enforced by the query, not by an "if" the caller
 * might forget, and cardholderId must come from the session, never a request
 * parameter.
 */
public class NotificationLogDao {

    void insert(long cardholderId, AuditEvent event, String detail) {
        String sql = "INSERT INTO notification (cardholder_id, event_type, category, detail) "
                   + "VALUES (?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, cardholderId);
            ps.setString(2, event.name());
            ps.setString(3, NotificationCategory.of(event).name());
            ps.setString(4, detail);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NOTIFY] no se pudo registrar la notificación de "
                    + cardholderId + ": " + e.getMessage());
        }
    }

    public List<NotificationRow> find(long cardholderId, String category, int offset, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, event_type, category, detail, created_at FROM notification "
              + "WHERE cardholder_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(cardholderId);
        appendCategory(sql, params, category);
        sql.append("ORDER BY created_at DESC, id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        List<NotificationRow> rows = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rows.add(read(rs));
            }
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading notifications", e);
        }
    }

    public int count(long cardholderId, String category) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM notification WHERE cardholder_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(cardholderId);
        appendCategory(sql, params, category);

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting notifications", e);
        }
    }

    private void appendCategory(StringBuilder sql, List<Object> params, String category) {
        if (category != null && !category.isBlank()) {
            sql.append("AND category = ? ");
            params.add(category);
        }
    }

    private NotificationRow read(ResultSet rs) throws SQLException {
        return new NotificationRow(
                rs.getLong("id"),
                rs.getString("event_type"),
                NotificationCategory.valueOf(rs.getString("category")),
                rs.getString("detail"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
    }
}
