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
              Todas las tarjetas vivas de la cuenta, la física antes que la digital.

              Antes se quedaba con UNA activa de cada tipo. No era la regla del
              negocio, era el hueco que había en la pantalla: dibujaba dos
              solapadas y una tercera la descuadraba. El precio lo pagaban las
              BLOQUEADAS, que desaparecían de la cuenta — perder la tarjeta la
              borraba de la vista y no quedaba desde dónde reactivarla, aunque
              Mis tarjetas sí la enseñara. Ahora la rejilla fluye y se desplaza,
              así que la vista puede enseñar lo que la cuenta tiene.

              Las INVALIDATED siguen fuera: son definitivas y no vuelven, el
              mismo criterio que usa Mis tarjetas.

              La física primero porque es la que el marco pone delante; después
              por id, que es el orden en que se expidieron.
             */
            java.util.List<mx.sgfte.core.cards.Card> shown =
                    portalService.cardsOf(cardholderId, accountId).stream()
                            .filter(c -> "ACTIVE".equals(c.getStatus())
                                    || "BLOCKED".equals(c.getStatus()))
                            .sorted(java.util.Comparator
                                    .comparingInt((mx.sgfte.core.cards.Card c) -> c.isPhysical() ? 0 : 1)
                                    .thenComparing(mx.sgfte.core.cards.Card::getId))
                            .toList();
            req.setAttribute("cards", shown);

            req.setAttribute("activity", portalService.accountActivity(cardholderId, accountId));

            // Lo que necesita el modal de transferencia.
            req.setAttribute("myAccounts", portalService.myAccounts(cardholderId));
        } catch (AccountNotOwnedException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        req.getRequestDispatcher("/WEB-INF/jsp/app/cuenta.jsp").forward(req, resp);
    }

}
