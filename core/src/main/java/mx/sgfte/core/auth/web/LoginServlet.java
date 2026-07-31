package mx.sgfte.core.auth.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.auth.AppUser;
import mx.sgfte.core.auth.AuthService;
import mx.sgfte.core.auth.SessionUser;

import java.io.IOException;
import java.util.Optional;

/**
 * GET  /login  -> show the form (or bounce to home if already logged in).
 * POST /login  -> authenticate; on success create a fresh session + redirect.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            resp.sendRedirect(req.getContextPath() + "/admin/home");
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
        session.setAttribute("user", new SessionUser(user.getId(), user.getFullName(), user.getRole()));
        session.setMaxInactiveInterval(30 * 60); // 30 minutes

        // Cardholder home is a future slice; admins land on the dashboard.
        String target = "ADMIN".equals(user.getRole()) ? "/admin/home" : "/app/home";
        resp.sendRedirect(req.getContextPath() + target);
    }

    private void forwardToForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
    }
}
