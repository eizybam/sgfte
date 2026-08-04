package mx.sgfte.core.audit;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Audit microservice entry point. Any module calls record(...) after an
 * important action.
 *
 * Logging failures never break the main operation: a dispersion that moved
 * money must not be rolled back because the trail could not be written. The
 * error goes to stderr instead, which is the right trade for a best-effort log
 * — but it does mean a silent gap is possible, so the swallow is deliberate and
 * narrow rather than a blanket catch.
 */
public class AuditLogService {

    private final AuditLogDao dao;

    public AuditLogService() { this(new AuditLogDao()); }
    public AuditLogService(AuditLogDao dao) { this.dao = dao; }

    /**
     * Records an event. Severity and module come from the event itself, so two
     * call sites cannot disagree about how serious the same thing is.
     */
    public void record(AuditEvent event, String detail, String actor, String ip) {
        try {
            dao.insert(new AuditLog(event, detail, actor, ip));
        } catch (RuntimeException e) {
            System.err.println("[AUDIT] could not write log: " + e.getMessage());
        }
    }

    /**
     * Same, taking the actor and the IP straight off the request — which is
     * where both actually live, and the only place the caller's address exists.
     */
    public void record(AuditEvent event, String detail, HttpServletRequest req) {
        record(event, detail, actorOf(req), req == null ? null : req.getRemoteAddr());
    }

    /** The signed-in user's name, or "anónimo" before login. */
    public static String actorOf(HttpServletRequest req) {
        if (req == null) return null;
        Object user = req.getSession(false) == null ? null : req.getSession().getAttribute("user");
        return user == null ? "anónimo" : String.valueOf(user);
    }

    public List<AuditLog> recent(int limit) {
        return dao.findRecent(limit);
    }
}
