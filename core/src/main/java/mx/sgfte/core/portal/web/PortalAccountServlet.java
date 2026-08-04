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
            /*
              Como mucho dos tarjetas: una física y una digital, que es la regla
              del negocio y desde V6 también la del índice. Se eligen aquí y no
              en el JSP para que la vista NO pueda recibir una tercera: aunque
              quedaran datos viejos de antes del índice, la pantalla no se
              descuadra.
             */
            java.util.List<mx.sgfte.core.cards.Card> cards =
                    portalService.cardsOf(cardholderId, accountId);
            /*
              Como mucho dos, y la física primero: es la que el marco pone
              delante. Se entrega ya ordenada para que el JSP recorra una lista
              en vez de repetir el mismo bloque dos veces con nombres distintos.
             */
            java.util.List<mx.sgfte.core.cards.Card> shown = new java.util.ArrayList<>();
            var physical = firstActiveOfType(cards, "PHYSICAL");
            var digital = firstActiveOfType(cards, "DIGITAL");
            if (physical != null) shown.add(physical);
            if (digital != null) shown.add(digital);
            req.setAttribute("cards", shown);

            req.setAttribute("activity", portalService.accountActivity(cardholderId, accountId));

            // Lo que necesita el modal de transferencia.
            req.setAttribute("myAccounts", portalService.myAccounts(cardholderId));
            req.setAttribute("peersByAccount", portalService.peersByAccount(cardholderId));
        } catch (AccountNotOwnedException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.getRequestDispatcher("/WEB-INF/jsp/app/cuenta.jsp").forward(req, resp);
    }

    /** The account's active card of that type, or null if it has none. */
    private mx.sgfte.core.cards.Card firstActiveOfType(
            java.util.List<mx.sgfte.core.cards.Card> cards, String type) {
        return cards.stream()
                .filter(c -> type.equals(c.getCardType()) && "ACTIVE".equals(c.getStatus()))
                .findFirst()
                .orElse(null);
    }
}
