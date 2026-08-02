package mx.sgfte.core.audit;

import mx.sgfte.core.shared.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDao {

    public void insert(AuditLog log) {
        String sql = "INSERT INTO audit_log (event_type, detail, actor) VALUES (?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, log.getEventType());
            ps.setString(2, log.getDetail());
            ps.setString(3, log.getActor());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error writing audit log", e);
        }
    }

    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT id, event_type, detail, actor, created_at FROM audit_log "
                   + "ORDER BY id DESC FETCH FIRST ? ROWS ONLY";
        List<AuditLog> list = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog l = new AuditLog();
                    l.setId(rs.getLong("id"));
                    l.setEventType(rs.getString("event_type"));
                    l.setDetail(rs.getString("detail"));
                    l.setActor(rs.getString("actor"));
                    l.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(l);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error reading audit log", e);
        }
    }
}
