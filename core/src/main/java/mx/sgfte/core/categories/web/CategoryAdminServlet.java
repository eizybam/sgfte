package mx.sgfte.core.categories.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.categories.CategoryService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;

/**
 * GET  /admin/categorias — the purpose catalogue (Figma 2036:6).
 * POST /admin/categorias — creates a purpose, or retires/restores one.
 *
 * Two actions share the POST because both are small and both belong to the same
 * screen; "action" says which. Everything follows post/redirect/get, so a
 * refresh after creating cannot create a second one.
 *
 * Protected by AuthFilter (/admin/*).
 */
@WebServlet("/admin/categorias")
public class CategoryAdminServlet extends HttpServlet {

    /** Leídos por el GET para reabrir el modal con lo tecleado si falló. */
    public static final String FLASH_ERRORS      = "createErrors";
    public static final String FLASH_NAME        = "createName";
    public static final String FLASH_DESCRIPTION = "createDescription";
    public static final String FLASH_COLOR       = "createColor";
    public static final String FLASH_SUCCESS     = "success";

    private final CategoryService service = new CategoryService();
    private final AuditLogService audit = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("rows", service.catalogue());
        req.setAttribute("colors", colorRange());
        consumeFlash(req);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/categorias.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        String action = req.getParameter("action");

        if ("toggle".equals(action)) {
            toggle(req, session);
        } else {
            create(req, session);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/categorias");
    }

    private void create(HttpServletRequest req, HttpSession session) {
        String name = req.getParameter("name");
        String description = req.getParameter("description");
        Integer color = parseInt(req.getParameter("colorIndex"));
        // Una casilla sin marcar no se envía, así que ausencia = inactiva.
        boolean active = req.getParameter("active") != null;

        try {
            service.create(name, description, color, active);
            audit.record(AuditEvent.CATEGORY_CREATED,
                    name + (active ? "" : " (inactiva)"), req);

            mx.sgfte.core.shared.web.OperationResult.success("¡Categoría creada!",
                            "El catálogo de propósitos ya la incluye",
                            "CATEGORÍA REGISTRADA",
                            active
                                ? "Ya puede elegirse al crear cuentas."
                                : "Nace inactiva: no se ofrecerá hasta que la reactives.")
                    .detail("Categoría", name)
                    .detail("Descripción", description)
                    .detail("Estado", active ? "Activa" : "Inactiva")
                    .when(java.time.LocalDateTime.now())
                    .secondary("Ver categorías", "/admin/categorias")
                    .flash(session);
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
            session.setAttribute(FLASH_NAME, name);
            session.setAttribute(FLASH_DESCRIPTION, description);
            session.setAttribute(FLASH_COLOR, req.getParameter("colorIndex"));
        }
    }

    private void toggle(HttpServletRequest req, HttpSession session) {
        Long id = parseLong(req.getParameter("categoryId"));
        String name = req.getParameter("categoryName");
        try {
            boolean nowActive = service.toggleStatus(id);
            audit.record(nowActive ? AuditEvent.CATEGORY_ACTIVATED : AuditEvent.CATEGORY_RETIRED,
                    name == null ? String.valueOf(id) : name, req);

            mx.sgfte.core.shared.web.OperationResult.success(
                            nowActive ? "Categoría reactivada" : "Categoría retirada",
                            nowActive ? "Vuelve a ofrecerse al crear cuentas"
                                      : "Deja de ofrecerse al crear cuentas",
                            nowActive ? "REACTIVACIÓN CONFIRMADA" : "RETIRO CONFIRMADO",
                            nowActive
                                ? "Las cuentas que ya la usaban nunca dejaron de tenerla."
                                : "No se borra: las cuentas que ya la usan conservan su propósito y su histórico.")
                    .detail("Categoría", name)
                    .detail("Estado", nowActive ? "Activa" : "Inactiva")
                    .when(java.time.LocalDateTime.now())
                    .secondary("Ver categorías", "/admin/categorias")
                    .flash(session);
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
        }
    }

    /** 1..7, para pintar la paleta sin escribirla siete veces en el JSP. */
    private java.util.List<Integer> colorRange() {
        java.util.List<Integer> colors = new java.util.ArrayList<>();
        for (int i = CategoryService.MIN_COLOR; i <= CategoryService.MAX_COLOR; i++) colors.add(i);
        return colors;
    }

    private void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;
        for (String key : new String[] {
                FLASH_SUCCESS, FLASH_ERRORS, FLASH_NAME, FLASH_DESCRIPTION, FLASH_COLOR }) {
            Object value = session.getAttribute(key);
            if (value != null) {
                req.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    private Integer parseInt(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Integer.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
