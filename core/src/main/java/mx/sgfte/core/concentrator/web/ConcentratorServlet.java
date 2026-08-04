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

    /** Leídos por AdminHomeServlet para reabrir el modal si falló. */
    public static final String FLASH_ERRORS = "fundErrors";
    public static final String FLASH_AMOUNT = "fundAmount";

    private final ConcentratorService service = new ConcentratorService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BigDecimal amount = parseAmount(req.getParameter("amount"));
        String method = req.getParameter("method");

        jakarta.servlet.http.HttpSession session = req.getSession();
        try {
            // El actor del ledger es el correo, igual que en la bitácora.
            service.fund(amount, AuditLogService.actorOf(req));
            session.setAttribute("success", "Concentradora fondeada correctamente.");

            // El método de fondeo va en la bitácora, no en el ledger: es contexto
            // operativo, no un hecho financiero. El ledger guarda el importe y el
            // saldo resultante, que es lo que tiene que cuadrar.
            audit.record(AuditEvent.CONCENTRATOR_FUNDED,
                    (method == null || method.isBlank() ? "" : method + " · ") + "$" + amount, req);
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
            session.setAttribute(FLASH_AMOUNT, req.getParameter("amount"));
        }
        // Redirect y no forward: fondear mueve dinero, refrescar no debe repetirlo.
        resp.sendRedirect(req.getContextPath() + "/admin/home");
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
