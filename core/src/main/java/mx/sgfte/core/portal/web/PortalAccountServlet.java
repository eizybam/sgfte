package mx.sgfte.core.portal.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.portal.AccountNotOwnedException;
import mx.sgfte.core.portal.PortalAccount;
import mx.sgfte.core.portal.PortalService;

import java.io.IOException;

/**
 * GET /app/cuenta?id=N — detail of ONE of the employee's accounts (Figma:
 * "Vista de cuenta de Tarjetahabiente" + "Historial de movimientos"): balance,
 * the cards that give access to it, and its full ledger.
 *
 * The ?id= parameter is attacker-controlled, so it is resolved against the
 * session's cardholder before anything is read. An id belonging to someone else
 * gets a 404 — the same answer as an id that does not exist, so the response
 * reveals nothing about other people's accounts.
 */
@WebServlet("/app/cuenta")
public class PortalAccountServlet extends HttpServlet {

    private final PortalService portalService = new PortalService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long cardholderId = PortalSupport.cardholderId(req);
        Long accountId = PortalSupport.parseId(req.getParameter("id"));

        if (accountId == null) {
            resp.sendRedirect(req.getContextPath() + "/app/home");
            return;
        }

        try {
            PortalAccount account = portalService.myAccount(cardholderId, accountId);
            req.setAttribute("account", account);
            req.setAttribute("cards", portalService.cardsOf(cardholderId, accountId));
            req.setAttribute("movements", portalService.movementsOf(cardholderId, accountId));
        } catch (AccountNotOwnedException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.getRequestDispatcher("/WEB-INF/jsp/app/cuenta.jsp").forward(req, resp);
    }
}
