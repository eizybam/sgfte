package mx.sgfte.core.users.web;

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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/admin/empleados");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String fullName = req.getParameter("fullName");
        String email = req.getParameter("email");
        String department = req.getParameter("department");

        HttpSession session = req.getSession();
        try {
            service.registerFromFullName(fullName, email, department);
            session.setAttribute("success", "Empleado registrado.");
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
            session.setAttribute(FLASH_NAME, fullName);
            session.setAttribute(FLASH_EMAIL, email);
        }

        resp.sendRedirect(req.getContextPath() + "/admin/empleados");
    }
}
