package mx.sgfte.core.auth.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.auth.PasswordTokenService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * GET  /set-password?token=... -> the "create your password" form, or an
 *      "this link is no longer valid" state if the token is bad/expired/used.
 * POST /set-password            -> sets it and redeems the token.
 *
 * Same page and same servlet for BOTH first-time activation and a later
 * "forgot your password" reset — the token is the only thing that says which
 * login it's for, so there's nothing that needs to differ between the two.
 */
@WebServlet("/set-password")
public class SetPasswordServlet extends HttpServlet {

    private static final int MIN_LENGTH = 8;

    private final PasswordTokenService passwordTokenService = new PasswordTokenService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = req.getParameter("token");
        boolean valid = false;
        try {
            valid = passwordTokenService.validate(token).isPresent();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        req.setAttribute("token", token);
        req.setAttribute("valid", valid);
        forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = req.getParameter("token");
        String password = req.getParameter("password");
        String confirm = req.getParameter("confirmPassword");

        boolean valid = false;
        try {
            valid = passwordTokenService.validate(token).isPresent();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        req.setAttribute("token", token);
        req.setAttribute("valid", valid);

        if (!valid) {
            forward(req, resp);
            return;
        }
        if (password == null || password.length() < MIN_LENGTH) {
            req.setAttribute("error", "La contraseña debe tener al menos " + MIN_LENGTH + " caracteres.");
            forward(req, resp);
            return;
        }
        if (!password.equals(confirm)) {
            req.setAttribute("error", "Las contraseñas no coinciden.");
            forward(req, resp);
            return;
        }

        boolean ok = false;
        try {
            ok = passwordTokenService.setPassword(token, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (!ok) {
            // El token se usó entre el GET y este POST (dos pestañas, doble clic).
            req.setAttribute("valid", false);
            forward(req, resp);
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/login?activated=1");
    }

    private void forward(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/auth/set-password.jsp").forward(req, resp);
    }
}