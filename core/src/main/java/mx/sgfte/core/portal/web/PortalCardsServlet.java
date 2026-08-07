package mx.sgfte.core.portal.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.portal.PortalService;

import java.io.IOException;

/**
 * GET /app/tarjetas — "Mis tarjetas": every card across every account the
 * cardholder owns, one place to tell them apart by account instead of
 * hunting through each account's own screen.
 *
 * Reached from the "Mis tarjetas" quick-access item on the dashboard and the
 * account view — both used to be inert (no screen existed yet).
 *
 * Protected by AuthFilter (/app/*).
 */
@WebServlet("/app/tarjetas")
public class PortalCardsServlet extends HttpServlet {

    private final PortalService portalService = new PortalService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long cardholderId = PortalSupport.cardholderId(req);

        req.setAttribute("cards", portalService.myCards(cardholderId));
        // El modal de "Hacer un gasto" necesita el <select> de cuentas.
        req.setAttribute("myAccounts", portalService.myAccounts(cardholderId));

        req.getRequestDispatcher("/WEB-INF/jsp/app/tarjetas.jsp").forward(req, resp);
    }
}
