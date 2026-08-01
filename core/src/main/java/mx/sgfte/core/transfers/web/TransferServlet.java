package mx.sgfte.core.transfers.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.concentrator.AccountLookupDao;
import mx.sgfte.core.transfers.TransferService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * P2P transfer screen. Placed under /admin/* for now so AuthFilter protects it;
 * in the final flow this moves to the employee area /app/* once Module 6 adds
 * the employee filter. Reuses M1's AccountLookupDao for the dropdowns.
 */
@WebServlet("/admin/transferencia")
public class TransferServlet extends HttpServlet {

    private final TransferService transferService = new TransferService();
    private final AccountLookupDao accountLookupDao = new AccountLookupDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Long sourceId = parseId(req.getParameter("sourceId"));
        Long destId = parseId(req.getParameter("destId"));
        BigDecimal amount = parseAmount(req.getParameter("amount"));
        try {
            transferService.transfer(sourceId, destId, amount, req.getParameter("description"));
            req.setAttribute("success", "Transferencia realizada.");
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
        }
        render(req, resp);
    }

    private void render(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("accounts", accountLookupDao.findActiveForSelect());
        req.getRequestDispatcher("/WEB-INF/jsp/admin/transferencia.jsp").forward(req, resp);
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
