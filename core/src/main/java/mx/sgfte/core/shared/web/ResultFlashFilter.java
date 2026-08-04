package mx.sgfte.core.shared.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Moves the operation outcome from the session onto the request, once.
 *
 * It is a filter and not another consumeFlash() in every servlet because the
 * result modal shows on whichever screen the redirect lands on — and that is
 * already six different servlets. Six copies of the same three lines is six
 * chances for one of them to forget to clear the session, which would leave the
 * card reappearing on every page until the user logs out.
 *
 * Only on GET: a POST never renders, it redirects, and consuming the outcome
 * there would eat it before anyone saw it.
 */
@WebFilter(urlPatterns = {"/admin/*", "/app/*"})
public class ResultFlashFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest req && "GET".equals(req.getMethod())) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                Object result = session.getAttribute(OperationResult.KEY);
                if (result != null) {
                    req.setAttribute(OperationResult.KEY, result);
                    session.removeAttribute(OperationResult.KEY);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
