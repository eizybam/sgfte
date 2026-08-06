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

    /**
     * Who is acting: their email, or "anónimo" before login.
     *
     * The email and not the name because it is unique and it is the identity
     * they actually log in with — two "Juan Pérez" are indistinguishable in a
     * trail, two addresses are not.
     *
     * Never String.valueOf(principal): that wrote the object's toString —
     * "mx.sgfte.core.auth.SessionUser@1a2b3c" — into the trail and the ledger.
     */
    public static String actorOf(HttpServletRequest req) {
        if (req == null || req.getSession(false) == null) return null;
        Object user = req.getSession().getAttribute("user");
        if (user instanceof mx.sgfte.core.auth.SessionUser session) return session.getEmail();
        return "anónimo";
    }

    public List<AuditLog> recent(int limit) {
        return dao.findRecent(limit);
    }
}
