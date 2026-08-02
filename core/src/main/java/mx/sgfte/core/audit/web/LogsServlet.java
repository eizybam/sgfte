package mx.sgfte.core.audit.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.audit.AuditLogService;

import java.io.IOException;

/** Admin view of the most recent audit events. Protected by AuthFilter (/admin/*). */
@WebServlet("/admin/logs")
public class LogsServlet extends HttpServlet {

    private final AuditLogService auditLogService = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("logs", auditLogService.recent(100));
        req.getRequestDispatcher("/WEB-INF/jsp/admin/logs.jsp").forward(req, resp);
    }
}
