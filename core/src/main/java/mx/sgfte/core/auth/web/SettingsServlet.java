package mx.sgfte.core.auth.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.auth.AuthService;
import mx.sgfte.core.auth.SessionUser;
import mx.sgfte.core.auth.UserDao;
import mx.sgfte.core.shared.web.OperationResult;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;

/**
 * Account settings: profile, password change and session management.
 *
 * ONE class with TWO mappings. Both areas need exactly the same thing, but
 * each lives behind its own filter — AuthFilter for /admin/*, AppAuthFilter
 * for /app/* — and is rendered with its own shell. One servlet per area would
 * be two copies of the same POST; a URL outside both areas would not be
 * protected by any filter.
 *
 * The view is chosen by the path the request came in on, not by the role: if
 * an admin reached /app/ajustes, AppAuthFilter would have bounced them first.
 */
@WebServlet({"/admin/ajustes", "/app/ajustes"})
public class SettingsServlet extends HttpServlet {

    private final AuthService authService = new AuthService();
    private final AuditLogService audit = new AuditLogService();
    private final UserDao users = new UserDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        consumeFlash(req);

        /*
          Sí o no, sin cargar el BLOB: la vista sólo elige entre pintar el <img>
          y pintar la inicial. La foto en sí la sirve ProfilePhotoServlet, en su
          propia petición.
         */
        SessionUser user = (SessionUser) req.getSession().getAttribute("user");
        req.setAttribute("hasPhoto", users.hasPhoto(user.getId()));

        req.getRequestDispatcher(viewOf(req)).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession();
        SessionUser user = (SessionUser) session.getAttribute("user");

        try {
            authService.changePassword(user.getId(),
                    req.getParameter("currentPassword"),
                    req.getParameter("newPassword"),
                    req.getParameter("confirmPassword"));

            /*
              The password changed, so the session gets a new id.

              We don't invalidate it: kicking the user to the login screen right
              after a correct change reads like an error. Rotating the id is what
              cuts the value of a cookie someone might have copied before — which
              is what one is trying to prevent by changing the password. The
              session content (the SessionUser) is preserved.
            */
            req.changeSessionId();

            audit.record(AuditEvent.PASSWORD_CHANGED, "Usuario " + user.getEmail(), req);

            OperationResult.success("Contraseña actualizada",
                            "Tu nueva contraseña ya está activa",
                            "CAMBIO CONFIRMADO",
                            "Úsala la próxima vez que entres. Si no fuiste tú, avisa a administración.")
                    .when(java.time.LocalDateTime.now())
                    .flash(session);

        } catch (ValidationException e) {
            session.setAttribute("settingsErrors", e.getErrors());
        }
        resp.sendRedirect(req.getContextPath() + req.getServletPath());
    }

    /** Each area with its own shell; same screen inside. */
    private String viewOf(HttpServletRequest req) {
        return req.getServletPath().startsWith("/admin")
                ? "/WEB-INF/jsp/admin/ajustes.jsp"
                : "/WEB-INF/jsp/app/ajustes.jsp";
    }

    private void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;
        /*
          Dos llaves y no una: los errores de la foto se pintan en la tarjeta de
          la foto, y los de la contraseña en la suya. Con una sola, subir un PDF
          sacaba el aviso rojo debajo de "Cambiar contraseña".
         */
        for (String key : new String[]{"settingsErrors", "photoErrors"}) {
            Object errors = session.getAttribute(key);
            if (errors != null) {
                req.setAttribute(key, errors);
                session.removeAttribute(key);
            }
        }
    }
}
