package mx.sgfte.core.users.web;

import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.audit.AuditEvent;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.users.CardholderService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;

/**
 * Cardholder registration — Figma frame "Registro de tarjetahabiente" (279:104).
 *
 * It used to be a page of its own. In the prototype it is a modal that opens on
 * top of the employee list, so there is nothing left to render here: the GET
 * redirects to that list, and the POST creates and goes back to it.
 *
 * Post/redirect/get, with the outcome in a one-shot session flash: registering
 * inserts a row and burns a value off seq_employee_code, so a refresh must not
 * be able to replay it.
 *
 * Only HTTP orchestration lives here; the rules are in CardholderService.
 */
@WebServlet("/cardholders")
public class CardholderServlet extends HttpServlet {

    /** Leídos por CardholderAdminServlet para reabrir el modal si falló. */
    public static final String FLASH_ERRORS = "registerErrors";
    public static final String FLASH_NAME   = "registerName";
    public static final String FLASH_EMAIL  = "registerEmail";

    private final CardholderService service = new CardholderService();
    private final AuditLogService audit = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/empleados");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        String action = req.getParameter("action");

        if ("toggle".equals(action)) {
            toggle(req, session);
        } else {

            String fullName = req.getParameter("fullName");
            String email = req.getParameter("email");
            String department = req.getParameter("department");


            try {
                long newId = service.registerFromFullName(fullName, email, department);
                audit.record(AuditEvent.CARDHOLDER_CREATED, fullName + " · " + email, req);

                mx.sgfte.core.shared.web.OperationResult.success("¡Empleado registrado!",
                                "Ya puede tener cuentas y tarjetas a su nombre",
                                "ALTA CONFIRMADA",
                                "El código de empleado se asignó automáticamente y no cambia.")
                        .detail("Empleado", fullName)
                        .detail("Código", employeeCodeOf(newId))
                        .detail("Correo", email)
                        .detail("Departamento", department)
                        .when(java.time.LocalDateTime.now())
                        .secondary("Ver empleados", "/admin/empleados")
                        .flash(session);
            } catch (ValidationException e) {
                session.setAttribute(FLASH_ERRORS, e.getErrors());
                session.setAttribute(FLASH_NAME, fullName);
                session.setAttribute(FLASH_EMAIL, email);
            }
        }
        resp.sendRedirect(req.getContextPath() + "/admin/empleados");
    }

    /**
     * El código que acaba de asignarse, leído de vuelta.
     *
     * Lo genera el servicio dentro del alta y no lo devuelve, así que se
     * consulta en lugar de recalcularlo aquí: recalcularlo sería una segunda
     * implementación de la misma regla, libre de desviarse de la primera.
     */
    private String employeeCodeOf(long cardholderId) {
        try {
            return new mx.sgfte.core.users.CardholderDao().findDetail(cardholderId)
                    .map(mx.sgfte.core.users.CardholderDetail::getEmployeeCode)
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;   // el alta ya ocurrió; la tarjeta puede vivir sin el código
        }
    }

    private void toggle(HttpServletRequest req, HttpSession session) {
        Long id = parseLong(req.getParameter("cardholderId"));
        try {
            boolean nowActive = service.toggleStatus(id);

        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
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
