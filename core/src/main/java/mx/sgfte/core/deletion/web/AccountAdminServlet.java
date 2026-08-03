package mx.sgfte.core.deletion.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.accounts.AccountDao;
import mx.sgfte.core.accounts.AccountRow;
import mx.sgfte.core.categories.CategoryDao;
import mx.sgfte.core.deletion.DeletionService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.util.List;

/**
 * Accounts management (admin) — Figma frame "Gestion de Cuentas" (225:41).
 *
 * GET  /admin/cuentas             -> the filtered, paged table.
 * POST /admin/cuentas (accountId) -> delete an account (its balance is reintegrated).
 *
 * The three toolbar filters and the page number live in the query string, so a
 * filtered view can be linked, bookmarked and reloaded, and the pager needs no
 * client-side state at all.
 */
@WebServlet("/admin/cuentas")
public class AccountAdminServlet extends HttpServlet {

    /**
     * Rows per page. The frame's table is 562px tall and holds exactly six 80px
     * rows, so six is what reproduces it — and it is the only number to change
     * if the screen should show more.
     */
    private static final int PAGE_SIZE = 6;

    private static final String FLASH_SUCCESS = "success";
    private static final String FLASH_ERRORS  = "errors";

    private final DeletionService deletionService = new DeletionService();
    private final AccountDao accountDao = new AccountDao();
    private final CategoryDao categoryDao = new CategoryDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String search = trimToNull(req.getParameter("q"));
        String status = normalizeStatus(req.getParameter("status"));
        Long purposeId = parseId(req.getParameter("purpose"));

        int total = accountDao.countForAdmin(search, status, purposeId);
        int pageCount = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int page = clamp(parsePage(req.getParameter("page")), pageCount);

        List<AccountRow> rows =
                accountDao.findForAdmin(search, status, purposeId, (page - 1) * PAGE_SIZE, PAGE_SIZE);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageCount", pageCount);
        req.setAttribute("categories", categoryDao.findAllActive());

        // Se devuelven para que el buscador, el segmentado y la píldora vuelvan
        // a dibujarse con lo que el admin eligió.
        req.setAttribute("q", search);
        req.setAttribute("status", status == null ? "" : status);
        req.setAttribute("purpose", purposeId);

        consumeFlash(req);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/cuentas.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long accountId = parseId(req.getParameter("accountId"));
        HttpSession session = req.getSession();
        try {
            if (accountId == null) throw new ValidationException(List.of("Cuenta inválida"));
            deletionService.deleteAccount(accountId);
            session.setAttribute(FLASH_SUCCESS,
                    "Cuenta eliminada. Su saldo se reintegró a la Concentradora.");
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
        }
        // Redirect y no forward: borrar reintegra saldo, y refrescar no debe repetirlo.
        resp.sendRedirect(req.getContextPath() + "/admin/cuentas");
    }

    private void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;
        for (String key : new String[]{FLASH_SUCCESS, FLASH_ERRORS}) {
            Object value = session.getAttribute(key);
            if (value != null) {
                req.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    /** Sólo los dos estados reales filtran; cualquier otra cosa significa "todas". */
    private String normalizeStatus(String raw) {
        if ("ACTIVE".equals(raw) || "INACTIVE".equals(raw)) return raw;
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

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
