package mx.sgfte.core.concentrator.web;

import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.audit.AuditEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.concentrator.ConcentratorService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * GET  /admin/concentradora -> show balance + funding form.
 * POST /admin/concentradora -> add funds to the Concentrator.
 * Protected by AuthFilter (/admin/*).
 */
@WebServlet("/admin/concentradora")
public class ConcentratorServlet extends HttpServlet {

    private final AuditLogService audit = new AuditLogService();

    private final ConcentratorService service = new ConcentratorService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        BigDecimal amount = parseAmount(req.getParameter("amount"));
        try {
            // Quién fondea queda en el ledger; antes esta operación no dejaba rastro.
            Object user = req.getSession().getAttribute("user");
            service.fund(amount, user == null ? null : String.valueOf(user));
            req.setAttribute("success", "Concentradora fondeada correctamente.");
            audit.record(AuditEvent.CONCENTRATOR_FUNDED, "$" + amount, req);
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
        }
        render(req, resp);
    }

    private void render(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("concentrator", service.getConcentrator());
        req.getRequestDispatcher("/WEB-INF/jsp/admin/concentradora.jsp").forward(req, resp);
    }

    /** Parses a money string into BigDecimal, or null if empty/invalid. */
    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
