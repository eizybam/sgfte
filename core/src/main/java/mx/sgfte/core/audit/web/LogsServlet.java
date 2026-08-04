package mx.sgfte.core.audit.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.audit.AuditLog;
import mx.sgfte.core.audit.AuditLogDao;

import java.io.IOException;
import java.util.List;

/**
 * GET /admin/logs — Figma frame "Vista de Logs" (253:43).
 *
 * Filters and page live in the query string, same as the other tables, so a
 * filtered view can be linked and reloaded.
 *
 * Read-only by design: the log is immutable at the database level too.
 */
@WebServlet("/admin/logs")
public class LogsServlet extends HttpServlet {

    /** Six 80px rows is what the frame's table holds. */
    private static final int PAGE_SIZE = 6;

    private final AuditLogDao auditLogDao = new AuditLogDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String search = trimToNull(req.getParameter("q"));
        String severity = normalizeSeverity(req.getParameter("sev"));
        String module = trimToNull(req.getParameter("mod"));

        int total = auditLogDao.count(search, severity, module);
        int pageCount = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int page = clamp(parsePage(req.getParameter("page")), pageCount);

        List<AuditLog> rows =
                auditLogDao.find(search, severity, module, (page - 1) * PAGE_SIZE, PAGE_SIZE);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageCount", pageCount);
        req.setAttribute("modules", auditLogDao.distinctModules());
        req.setAttribute("q", search);
        req.setAttribute("sev", severity == null ? "" : severity);
        req.setAttribute("mod", module == null ? "" : module);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/logs.jsp").forward(req, resp);
    }

    /** Only the three real levels filter; anything else means "todos". */
    private String normalizeSeverity(String raw) {
        if ("INFO".equals(raw) || "ALERTA".equals(raw) || "CRIT".equals(raw)) return raw;
        return null;
    }

    private int clamp(int page, int pageCount) {
        return Math.min(Math.max(page, 1), pageCount);
    }

    private int parsePage(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try { return Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { return 1; }
    }

    private String trimToNull(String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }
}
