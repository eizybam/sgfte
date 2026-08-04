package mx.sgfte.core.concentrator.web;

import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.audit.AuditEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.concentrator.DispersionService;
import mx.sgfte.core.concentrator.InsufficientFundsException;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * POST /admin/dispersion — moves money from the Concentrator to an account.
 *
 * The form used to be its own page. In the Figma prototype it is the modal
 * "Dispersión de fondos" (2177:376) that opens on top of the Vista Global, so
 * there is no page left to render: the GET just sends you to /admin/home, where
 * the modal lives.
 *
 * The POST follows post/redirect/get. The outcome travels in the session as a
 * one-shot flash that AdminHomeServlet reads and clears, which means a refresh
 * after a dispersion cannot repeat it — worth caring about when the side effect
 * is moving money.
 *
 * Protected by AuthFilter (/admin/*).
 */
@WebServlet("/admin/dispersion")
public class DispersionServlet extends HttpServlet {

    /** Set on the session so /admin/home knows to reopen the modal. */
    public static final String FLASH_ERRORS  = "dispersionErrors";
    public static final String FLASH_ACCOUNT = "dispersionAccountId";
    public static final String FLASH_AMOUNT  = "dispersionAmount";
    public static final String FLASH_SUCCESS = "success";

    private final DispersionService dispersionService = new DispersionService();
    private final AuditLogService audit = new AuditLogService();

    /** The form is a modal now; there is nothing to show on its own. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/home");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String rawAccountId = req.getParameter("accountId");
        String rawAmount = req.getParameter("amount");

        Long accountId = parseId(rawAccountId);
        BigDecimal amount = parseAmount(rawAmount);
        String description = req.getParameter("description");

        HttpSession session = req.getSession();
        try {
            dispersionService.disperse(accountId, amount, description);
            session.setAttribute(FLASH_SUCCESS, "Dispersión aplicada. El saldo se sumó a la cuenta.");
            audit.record(AuditEvent.DISPERSION,
                    "Cuenta " + accountId + " · $" + amount, req);
        } catch (ValidationException e) {
            keepForRetry(session, e.getErrors(), rawAccountId, rawAmount);
        } catch (InsufficientFundsException e) {
            // Saldo insuficiente es ALERTA, no un error cualquiera: dice que
            // alguien intentó mover dinero que no había.
            audit.record(AuditEvent.DISPERSION_REJECTED, e.getMessage(), req);
            keepForRetry(session, List.of(e.getMessage()), rawAccountId, rawAmount);
        }

        resp.sendRedirect(req.getContextPath() + backTo(req));
    }

    /**
     * Where to land after dispersing. Three screens open this same modal, and
     * bouncing them all back to the dashboard would lose the admin's place.
     *
     * Never a URL: only an account id or the literal "concentradora", so the
     * parameter cannot be turned into an open redirect.
     */
    private String backTo(HttpServletRequest req) {
        if ("concentradora".equals(req.getParameter("returnTo"))) return "/admin/concentradora";
        Long detailId = parseId(req.getParameter("returnToAccount"));
        return detailId == null ? "/admin/home" : "/admin/cuenta?id=" + detailId;
    }

    /**
     * Puts the errors and what the admin typed back on the session, so the modal
     * reopens already filled in instead of making them start over.
     */
    private void keepForRetry(HttpSession session, List<String> errors,
                              String rawAccountId, String rawAmount) {
        session.setAttribute(FLASH_ERRORS, errors);
        session.setAttribute(FLASH_ACCOUNT, rawAccountId);
        session.setAttribute(FLASH_AMOUNT, rawAmount);
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return new BigDecimal(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
