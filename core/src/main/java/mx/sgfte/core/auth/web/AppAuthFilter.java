package mx.sgfte.core.auth.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.auth.SessionUser;

import java.io.IOException;

/**
 * Gate for the employee area (/app/*).
 *
 * Requires a valid session AND a login tied to a cardholder file. That second
 * condition matters: every /app screen is scoped by cardholder_id, and an ADMIN
 * has cardholder_id = NULL in app_user. Letting an admin through would produce
 * either an empty screen or a NullPointerException, so admins go back to their
 * own area instead.
 *
 * Because this filter guarantees it, the /app servlets can assume a cardholder
 * is present and don't each have to re-check.
 */
@WebFilter("/app/*")
public class AppAuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        Object principal = session == null ? null : session.getAttribute("user");

        if (principal == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // A login with no cardholder file (i.e. an admin) has nothing to show here.
        if (!(principal instanceof SessionUser user) || !user.isCardholder()) {
            resp.sendRedirect(req.getContextPath() + "/admin/home");
            return;
        }

        chain.doFilter(request, response);
    }
}
