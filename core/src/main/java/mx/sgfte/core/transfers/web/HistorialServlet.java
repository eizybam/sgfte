package mx.sgfte.core.transfers.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.concentrator.AccountLookupDao;
import mx.sgfte.core.movements.MovementQueryDao;

import java.io.IOException;

/**
 * Movement history for one account. Pick an account -> see its ledger rows.
 * Under /admin/* for now (protected); moves to /app/* in the final flow (Module 6).
 */
@WebServlet("/admin/historial")
public class HistorialServlet extends HttpServlet {

    private final MovementQueryDao movementQueryDao = new MovementQueryDao();
    private final AccountLookupDao accountLookupDao = new AccountLookupDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("accounts", accountLookupDao.findActiveForSelect());
        Long accountId = parseId(req.getParameter("accountId"));
        if (accountId != null) {
            req.setAttribute("selectedAccountId", accountId);
            req.setAttribute("movements", movementQueryDao.findByAccount(accountId));
        }
        req.getRequestDispatcher("/WEB-INF/jsp/admin/historial.jsp").forward(req, resp);
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
