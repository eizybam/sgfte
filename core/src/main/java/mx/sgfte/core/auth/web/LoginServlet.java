package mx.sgfte.core.auth.web;

import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.audit.AuditEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.auth.AppUser;
import mx.sgfte.core.auth.AuthService;
import mx.sgfte.core.auth.Role;
import mx.sgfte.core.auth.SessionUser;

import java.io.IOException;
import java.util.Optional;

/**
 * GET  /login  -> show the form (or bounce to home if already logged in).
 * POST /login  -> authenticate; on success create a fresh session + redirect.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final AuditLogService audit = new AuditLogService();

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object principal = session == null ? null : session.getAttribute("user");
        if (principal instanceof SessionUser user) {
            // Send them to their OWN area: a cardholder bounced to /admin/home
            // would just be redirected straight back out by AuthFilter.
            resp.sendRedirect(req.getContextPath() + Role.homeFor(user.getRole()));
            return;
        }
        forwardToForm(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        Optional<AppUser> authed = authService.authenticate(email, password);
        if (authed.isEmpty()) {
            // Se registra ANTES de responder: un intento fallido es justo lo que
            // hay que poder rastrear después.
            audit.record(AuditEvent.LOGIN_FAILED, "Correo: " + email, email, req.getRemoteAddr());
            req.setAttribute("error", "Credenciales inválidas.");
            req.setAttribute("email", email);   // keep what they typed
            forwardToForm(req, resp);
            return;
        }

        AppUser user = authed.get();

        // Prevent session fixation: discard any pre-login session, start fresh.
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = req.getSession(true);
        SessionUser principal = new SessionUser(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getRole(), user.getCardholderId());

        // El código de empleado lo enseña la cabecera del portal en todas sus
        // pantallas; se resuelve aquí para no consultarlo en cada petición.
        if (user.getCardholderId() != null) {
            try {
                new mx.sgfte.core.users.CardholderDao().findDetail(user.getCardholderId())
                        .ifPresent(d -> principal.setEmployeeCode(d.getEmployeeCode()));
            } catch (RuntimeException e) {
                // Entrar no puede fallar por no poder pintar un código.
                System.err.println("[LOGIN] no se pudo leer el código de empleado: " + e.getMessage());
            }
        }
        session.setAttribute("user", principal);
        session.setMaxInactiveInterval(30 * 60); // 30 minutes

        audit.record(AuditEvent.LOGIN_OK, "Rol: " + user.getRole(),
                     user.getEmail(), req.getRemoteAddr());

        resp.sendRedirect(req.getContextPath() + Role.homeFor(user.getRole()));
    }

    private void forwardToForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
    }
}
