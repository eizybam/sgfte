package mx.sgfte.core.departments.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.departments.DepartmentService;
import mx.sgfte.core.shared.web.OperationResult;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;

/**
 * POST /admin/departamentos — alta, edición y retiro/reactivación de un área.
 *
 * No tiene GET propio: la tabla vive en /admin/categorias, junto al catálogo de
 * propósitos, porque las dos son lo mismo —listas que alimentan desplegables— y
 * separarlas serían dos pantallas con una tabla cada una. Por eso todas las
 * respuestas redirigen allí.
 *
 * Servlet aparte y no una rama más de CategoryAdminServlet: son dos entidades
 * distintas, con su propio servicio y su propio DAO. Compartir la pantalla no
 * es motivo para compartir el controlador.
 *
 * Protegido por AuthFilter (/admin/*).
 */
@WebServlet("/admin/departamentos")
public class DepartmentServlet extends HttpServlet {

    /** Leídos por CategoryAdminServlet para reabrir el modal si falló. */
    public static final String FLASH_ERRORS      = "deptErrors";
    public static final String FLASH_NAME        = "deptName";
    public static final String FLASH_DESCRIPTION = "deptDescription";
    public static final String FLASH_EDIT_ID     = "deptEditId";

    private static final String BACK = "/admin/categorias";

    private final DepartmentService service = new DepartmentService();
    private final AuditLogService audit = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + BACK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        String action = req.getParameter("action");

        if ("toggle".equals(action)) {
            toggle(req, session);
        } else if ("update".equals(action)) {
            update(req, session);
        } else {
            create(req, session);
        }
        resp.sendRedirect(req.getContextPath() + BACK);
    }

    private void create(HttpServletRequest req, HttpSession session) {
        String name = req.getParameter("name");
        String description = req.getParameter("description");
        // Una casilla sin marcar no se envía, así que ausencia = inactivo.
        boolean active = req.getParameter("active") != null;

        try {
            service.create(name, description, active);
            audit.record(AuditEvent.DEPARTMENT_CREATED, name + (active ? "" : " (inactivo)"), req);

            OperationResult.success("¡Departamento creado!",
                            "El catálogo de áreas ya lo incluye",
                            "DEPARTAMENTO REGISTRADO",
                            active
                                ? "Ya puede elegirse al dar de alta o editar a un empleado."
                                : "Nace inactivo: no se ofrecerá hasta que lo reactives.")
                    .detail("Departamento", name)
                    .detail("Descripción", description)
                    .detail("Estado", active ? "Activo" : "Inactivo")
                    .when(java.time.LocalDateTime.now())
                    .secondary("Ver catálogo", BACK)
                    .flash(session);
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
            session.setAttribute(FLASH_NAME, name);
            session.setAttribute(FLASH_DESCRIPTION, description);
        }
    }

    private void update(HttpServletRequest req, HttpSession session) {
        Long id = parseLong(req.getParameter("departmentId"));
        String name = req.getParameter("name");
        String description = req.getParameter("description");

        try {
            service.update(id, name, description);
            audit.record(AuditEvent.DEPARTMENT_UPDATED, name, req);

            OperationResult.success("Departamento actualizado",
                            "El catálogo de áreas quedó al día",
                            "ACTUALIZACIÓN CONFIRMADA",
                            "Los empleados de esta área reflejan el cambio en toda la aplicación.")
                    .detail("Departamento", name)
                    .detail("Descripción", description)
                    .when(java.time.LocalDateTime.now())
                    .secondary("Ver catálogo", BACK)
                    .flash(session);
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
            session.setAttribute(FLASH_EDIT_ID, id);
            session.setAttribute(FLASH_NAME, name);
            session.setAttribute(FLASH_DESCRIPTION, description);
        }
    }

    private void toggle(HttpServletRequest req, HttpSession session) {
        Long id = parseLong(req.getParameter("departmentId"));
        String name = req.getParameter("departmentName");
        try {
            boolean nowActive = service.toggleStatus(id);
            audit.record(nowActive ? AuditEvent.DEPARTMENT_ACTIVATED : AuditEvent.DEPARTMENT_RETIRED,
                         name == null ? String.valueOf(id) : name, req);

            OperationResult.success(
                            nowActive ? "Departamento reactivado" : "Departamento retirado",
                            nowActive ? "Vuelve a ofrecerse al registrar empleados"
                                      : "Deja de ofrecerse al registrar empleados",
                            nowActive ? "REACTIVACIÓN CONFIRMADA" : "RETIRO CONFIRMADO",
                            nowActive
                                ? "Los empleados que ya lo tenían nunca dejaron de tenerlo."
                                : "No se borra: los empleados que ya pertenecen a esta área la conservan.")
                    .detail("Departamento", name)
                    .when(java.time.LocalDateTime.now())
                    .secondary("Ver catálogo", BACK)
                    .flash(session);
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
        }
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
