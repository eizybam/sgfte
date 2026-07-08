package mx.sgfte.core.users.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.users.Cardholder;
import mx.sgfte.core.users.CardholderService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;

/**
 * Controller for cardholder registration.
 * GET  /cardholders -> show the form.
 * POST /cardholders -> validate, register, and report the result.
 * Only HTTP orchestration lives here; the rules are in CardholderService.
 */
@WebServlet("/cardholders")
public class CardholderServlet extends HttpServlet {

    private final CardholderService service = new CardholderService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        forwardForm(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Cardholder ch = new Cardholder(
                req.getParameter("firstName"),
                req.getParameter("lastName"),
                req.getParameter("email"),
                req.getParameter("phone"));

        try {
            long id = service.register(ch);
            req.setAttribute("successId", id);
            req.setAttribute("cardholder", ch);
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            req.setAttribute("cardholder", ch); // keep the typed values
        }
        forwardForm(req, resp);
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/cardholders/form.jsp").forward(req, resp);
    }
}
