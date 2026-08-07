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
import mx.sgfte.core.auth.Role;
import mx.sgfte.core.auth.SessionUser;

import java.io.IOException;

/**
 * Gate for the administration area (RF-13, RNF-05).
 *
 * Two checks, in order:
 *   1) authentication — no session, no entry (bounce to /login);
 *   2) authorization  — only ADMIN may pass; a cardholder is sent back to its
 *      own area instead of being shown an error page.
 *
 * The mapping also covers /accounts and /cardholders. Those two live outside
 * /admin/* for historical reasons (they were the first vertical slices) and were
 * therefore reachable with no session at all — anyone could create cardholders
 * and accounts. Listing them here closes that hole without breaking the URLs the
 * JSP forms already post to. Moving them under /admin/ is a follow-up worth doing
 * once the in-flight branches land.
 */
@WebFilter({"/admin/*", "/accounts", "/cardholders"})
public class AuthFilter implements Filter {

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

        // Anyone who is not an ADMIN belongs in the employee area.
        if (!(principal instanceof SessionUser user) || !user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + Role.homeFor(Role.CARDHOLDER));
            return;
        }

        chain.doFilter(request, response);
    }
}
