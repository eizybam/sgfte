package mx.sgfte.core.deletion.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.concentrator.AccountLookupDao;
import mx.sgfte.core.deletion.DeletionService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;

/**
 * Accounts management (admin): list active accounts, delete one (with reintegration).
 * GET  /admin/cuentas               -> list.
 * POST /admin/cuentas (action=delete) -> delete account.
 */
@WebServlet("/admin/cuentas")
public class AccountAdminServlet extends HttpServlet {

    private final DeletionService deletionService = new DeletionService();
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
        try {
            if (accountId == null) throw new ValidationException(java.util.List.of("Cuenta inválida"));
            deletionService.deleteAccount(accountId);
            req.setAttribute("success", "Cuenta eliminada. Su saldo se reintegró a la Concentradora.");
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
        }
        render(req, resp);
    }

    private void render(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("accounts", accountLookupDao.findActiveForSelect());
        req.getRequestDispatcher("/WEB-INF/jsp/admin/cuentas.jsp").forward(req, resp);
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
