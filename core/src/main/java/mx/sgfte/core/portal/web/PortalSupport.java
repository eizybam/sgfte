package mx.sgfte.core.portal.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.auth.SessionUser;

import java.math.BigDecimal;

/**
 * Small helpers shared by the /app servlets.
 *
 * The important one is {@link #cardholderId(HttpServletRequest)}: it is the ONLY
 * sanctioned way for a portal servlet to learn whose data it is showing. It reads
 * the session, never a request parameter, so no screen can be tricked into
 * scoping its query to somebody else.
 */
final class PortalSupport {

    private PortalSupport() {}

    /**
     * The cardholder behind the current session.
     * AppAuthFilter guarantees there is one by the time any /app servlet runs,
     * so a missing value here is a programming error, not a user error.
     */
    static long cardholderId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object principal = session == null ? null : session.getAttribute("user");
        if (principal instanceof SessionUser user && user.getCardholderId() != null) {
            return user.getCardholderId();
        }
        throw new IllegalStateException(
                "No cardholder in session — AppAuthFilter should have blocked this request");
    }

    /** Parses an id from a parameter; null when absent or not a number. */
    static Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a money amount. Returns null on garbage input and lets the service
     * produce the user-facing message, so validation wording stays in one place.
     */
    static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
