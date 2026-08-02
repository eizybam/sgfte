package mx.sgfte.core.concentrator.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.concentrator.AccountLookupDao;
import mx.sgfte.core.concentrator.ConcentratorService;
import mx.sgfte.core.concentrator.DispersionService;
import mx.sgfte.core.concentrator.InsufficientFundsException;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * GET  /admin/dispersion -> show account dropdown + amount form.
 * POST /admin/dispersion -> move money from Concentrator to the chosen account.
 * Protected by AuthFilter (/admin/*).
 */
@WebServlet("/admin/dispersion")
public class DispersionServlet extends HttpServlet {

    private final DispersionService dispersionService = new DispersionService();
    private final ConcentratorService concentratorService = new ConcentratorService();
    private final AccountLookupDao accountLookupDao = new AccountLookupDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Long accountId = parseId(req.getParameter("accountId"));
        BigDecimal amount = parseAmount(req.getParameter("amount"));
        String description = req.getParameter("description");
        try {
            dispersionService.disperse(accountId, amount, description);
            req.setAttribute("success", "Dispersión aplicada. El saldo se sumó a la cuenta.");
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
        } catch (InsufficientFundsException e) {
            req.setAttribute("errors", java.util.List.of(e.getMessage()));
        }
        render(req, resp);
    }

    private void render(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("accounts", accountLookupDao.findActiveForSelect());
        req.setAttribute("concentrator", concentratorService.getConcentrator());
        req.getRequestDispatcher("/WEB-INF/jsp/admin/dispersion.jsp").forward(req, resp);
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
