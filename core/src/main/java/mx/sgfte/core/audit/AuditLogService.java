package mx.sgfte.core.audit;

/**
 * Audit microservice entry point. Any module calls record(...) after an
 * important action. Logging failures should never break the main operation,
 * so we swallow errors here (best-effort).
 */
public class AuditLogService {

    private final AuditLogDao dao;

    public AuditLogService() { this(new AuditLogDao()); }
    public AuditLogService(AuditLogDao dao) { this.dao = dao; }

    public void record(String eventType, String detail, String actor) {
        try {
            dao.insert(new AuditLog(eventType, detail, actor));
        } catch (RuntimeException e) {
            System.err.println("[AUDIT] could not write log: " + e.getMessage());
        }
    }

    public java.util.List<AuditLog> recent(int limit) {
        return dao.findRecent(limit);
    }
}
