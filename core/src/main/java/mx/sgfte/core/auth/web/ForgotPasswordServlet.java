package mx.sgfte.core.auth.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.auth.PasswordTokenService;

import java.io.IOException;

/**
 * GET  /forgot-password -> the "enter your email" form.
 * POST /forgot-password -> always the same generic confirmation, whether or
 *                          not the email matched a login — never reveals
 *                          which addresses exist in the system.
 */
@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private final PasswordTokenService passwordTokenService = new PasswordTokenService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String email = req.getParameter("email");
        passwordTokenService.requestPasswordReset(email);

        req.setAttribute("sent", true);
        req.setAttribute("email", email);
        req.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(req, resp);
    }
}