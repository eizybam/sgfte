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
        String sql = "INSERT INTO audit_log "
                   + "(event_type, severity, module, detail, actor, ip_address) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, log.getEventType());
            ps.setString(2, log.getSeverity() == null ? "INFO" : log.getSeverity());
            ps.setString(3, log.getModule());
            ps.setString(4, log.getDetail());
            ps.setString(5, log.getActor());
            ps.setString(6, log.getIpAddress());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error writing audit log", e);
        }
    }

    /**
     * One page of the log, newest first, with the screen's three filters.
     *
     * The search covers the actor, the IP and the event code — the code rather
     * than the Spanish label, because the label lives in the enum and never
     * reaches the database.
     */
    public List<AuditLog> find(String search, String severity, String module,
                               int offset, int limit) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, event_type, severity, module, detail, actor, ip_address, created_at "
              + "FROM audit_log ");

        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, severity, module);

        sql.append("ORDER BY created_at DESC, id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        params.add(offset);
        params.add(limit);

        List<AuditLog> list = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(read(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Error reading audit log", e);
        }
    }

    public int count(String search, String severity, String module) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM audit_log ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, severity, module);

        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting audit log", e);
        }
    }

    /** Modules actually present in the log — feeds the toolbar pill. */
    public List<String> distinctModules() {
        String sql = "SELECT DISTINCT module FROM audit_log "
                   + "WHERE module IS NOT NULL ORDER BY module";
        List<String> modules = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) modules.add(rs.getString(1));
            return modules;
        } catch (SQLException e) {
            throw new RuntimeException("Error loading audit modules", e);
        }
    }

    public List<AuditLog> findRecent(int limit) {
        return find(null, null, null, 0, limit);
    }

    private void appendFilters(StringBuilder sql, List<Object> params,
                               String search, String severity, String module) {
        sql.append("WHERE 1 = 1 ");

        if (search != null && !search.isBlank()) {
            sql.append("AND (UPPER(actor) LIKE ? OR UPPER(ip_address) LIKE ? ")
               .append("  OR UPPER(event_type) LIKE ? OR UPPER(detail) LIKE ?) ");
            String like = "%" + search.trim().toUpperCase() + "%";
            for (int i = 0; i < 4; i++) params.add(like);
        }
        if (severity != null && !severity.isBlank()) {
            sql.append("AND severity = ? ");
            params.add(severity);
        }
        if (module != null && !module.isBlank()) {
            sql.append("AND module = ? ");
            params.add(module);
        }
    }

    private AuditLog read(ResultSet rs) throws SQLException {
        AuditLog l = new AuditLog();
        l.setId(rs.getLong("id"));
        l.setEventType(rs.getString("event_type"));
        l.setSeverity(rs.getString("severity"));
        l.setModule(rs.getString("module"));
        l.setDetail(rs.getString("detail"));
        l.setActor(rs.getString("actor"));
        l.setIpAddress(rs.getString("ip_address"));
        l.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return l;
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
    }
}
