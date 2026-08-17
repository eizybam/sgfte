package mx.sgfte.core.deletion.web;

import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.audit.AuditEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.deletion.DeletionService;
import mx.sgfte.core.users.CardholderAdminRow;
import mx.sgfte.core.users.CardholderDao;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.util.List;

/**
 * Employees management (admin) — Figma frame "Gestor de empleados" (287:104).
 *
 * GET  /admin/empleados                  -> the filtered, paged table.
 * POST /admin/empleados (cardholderId)   -> delete a cardholder.
 *
 * Deleting a cardholder reintegrates ALL their account balances and invalidates
 * all their cards (business rule 4), so the POST redirects rather than forwards:
 * a refresh must not be able to replay it.
 *
 * Filters and page live in the query string, same as the accounts table.
 */
@WebServlet("/admin/empleados")
public class CardholderAdminServlet extends HttpServlet {

    /** Six 80px rows is what the frame's 562px table holds. */
    private static final int PAGE_SIZE = 6;

    private static final String FLASH_SUCCESS = "success";
    private static final String FLASH_ERRORS  = "errors";

    private final DeletionService deletionService = new DeletionService();
    private final CardholderDao cardholderDao = new CardholderDao();
    private final mx.sgfte.core.departments.DepartmentDao departmentDao =
            new mx.sgfte.core.departments.DepartmentDao();
    private final AuditLogService audit = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String search = trimToNull(req.getParameter("q"));
        String status = normalizeStatus(req.getParameter("status"));
        String department = trimToNull(req.getParameter("dept"));

        int total = cardholderDao.countForAdmin(search, status, department);
        int pageCount = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int page = clamp(parsePage(req.getParameter("page")), pageCount);

        List<CardholderAdminRow> rows = cardholderDao.findForAdmin(
                search, status, department, (page - 1) * PAGE_SIZE, PAGE_SIZE);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageCount", pageCount);
        req.setAttribute("departments", cardholderDao.distinctDepartments());
        // El desplegable del modal de alta necesita el id, no sólo el nombre.
        req.setAttribute("departmentOptions", departmentDao.findAllActive());
        req.setAttribute("q", search);
        req.setAttribute("status", status == null ? "" : status);
        req.setAttribute("dept", department == null ? "" : department);

        consumeFlash(req);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/empleados.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Long cardholderId = parseId(req.getParameter("cardholderId"));
        HttpSession session = req.getSession();
        try {
            if (cardholderId == null) {
                throw new ValidationException(List.of("Tarjetahabiente inválido"));
            }
            deletionService.deleteCardholder(cardholderId);
            audit.record(AuditEvent.CARDHOLDER_DELETED, "Empleado " + cardholderId, req);

            mx.sgfte.core.shared.web.OperationResult.success("Empleado eliminado",
                            "Los fondos regresaron a la Concentradora",
                            "REINTEGRACIÓN CONFIRMADA",
                            "El saldo se devolvió a la Cuenta Concentradora y sus tarjetas quedaron invalidadas.")
                    .detail("Empleado", "Nº " + cardholderId)
                    .when(java.time.LocalDateTime.now())
                    .secondary("Ver empleados", "/admin/empleados")
                    .primary("Ver Concentradora", "/admin/concentradora")
                    .flash(session);
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
        }
        resp.sendRedirect(req.getContextPath() + "/admin/empleados");
    }

    private void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;
        String[] keys = {
                FLASH_SUCCESS, FLASH_ERRORS,
                // Del alta: si falló, el modal se reabre con lo tecleado.
                mx.sgfte.core.users.web.CardholderServlet.FLASH_ERRORS,
                mx.sgfte.core.users.web.CardholderServlet.FLASH_NAME,
                mx.sgfte.core.users.web.CardholderServlet.FLASH_EMAIL,
        };
        for (String key : keys) {
            Object value = session.getAttribute(key);
            if (value != null) {
                req.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    /** Only the two real statuses filter; anything else means "todos". */
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
