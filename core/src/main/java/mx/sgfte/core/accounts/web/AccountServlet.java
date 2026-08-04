package mx.sgfte.core.accounts.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.accounts.Account;
import mx.sgfte.core.accounts.AccountService;
import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.categories.Category;
import mx.sgfte.core.categories.CategoryDao;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.util.List;

/**
 * Account creation — Figma frame "Crear Cuenta" (288:28).
 *
 * It used to be a page of its own. In the prototype it is a modal on top of
 * "Gestión de Cuentas", so nothing is rendered here: the GET redirects to that
 * list and the POST creates and returns to it.
 *
 * Post/redirect/get with a one-shot session flash: creating an account inserts a
 * row and burns account numbers off the generator, so a refresh must not replay
 * it.
 */
@WebServlet("/accounts")
public class AccountServlet extends HttpServlet {

    /** Leídos por AccountAdminServlet para reabrir el modal si falló. */
    public static final String FLASH_ERRORS   = "createErrors";
    public static final String FLASH_HOLDER   = "createHolder";
    public static final String FLASH_CATEGORY = "createCategory";

    private final AccountService accountService = new AccountService();
    private final CategoryDao categoryDao = new CategoryDao();
    private final AuditLogService audit = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/cuentas");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        Long cardholderId = parseId(req.getParameter("cardholderId"));
        Long categoryId = parseId(req.getParameter("categoryId"));

        // Se cargan una vez: el nombre de la categoría es lo que da el prefijo.
        List<Category> categories = categoryDao.findAllActive();

        HttpSession session = req.getSession();
        Account account = new Account(cardholderId, categoryId);
        try {
            accountService.create(account, nameOf(categories, categoryId));
            session.setAttribute("success",
                    "Cuenta creada: " + account.getAccountNumber());
            audit.record(AuditEvent.ACCOUNT_CREATED, account.getAccountNumber(), req);
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
            session.setAttribute(FLASH_HOLDER, req.getParameter("cardholderId"));
            session.setAttribute(FLASH_CATEGORY, req.getParameter("categoryId"));
        }

        resp.sendRedirect(req.getContextPath() + "/admin/cuentas");
    }

    private String nameOf(List<Category> categories, Long id) {
        if (id == null) return "";
        for (Category c : categories) {
            if (id.equals(c.getId())) return c.getName();
        }
        return "";
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
