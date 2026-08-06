package mx.sgfte.core.portal.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.portal.AccountNotOwnedException;
import mx.sgfte.core.portal.PortalService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * P2P transfer for the employee (HU-07, Figma: "Transferencia").
 *
 * The source dropdown lists only the employee's OWN accounts; the destination
 * dropdown fills in once a source is chosen, with colleagues' accounts of the
 * same purpose (RN-07). Picking a source re-submits the form with GET, the same
 * pattern the admin historial screen uses, so no JavaScript is needed.
 *
 * Note this is the same TransferService the admin screen calls. The rule about
 * matching purposes is not re-implemented here — this servlet only adds "the
 * source must be mine".
 */
@WebServlet("/app/transferencia")
public class PortalTransferServlet extends HttpServlet {

    private final PortalService portalService = new PortalService();

    /** El formulario es un modal del panel; aquí no hay pantalla que pintar. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/app/home");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long cardholderId = PortalSupport.cardholderId(req);
        Long sourceId = PortalSupport.parseId(req.getParameter("sourceId"));
        Long destId = PortalSupport.parseId(req.getParameter("destId"));
        BigDecimal amount = PortalSupport.parseAmount(req.getParameter("amount"));

        /*
          Post/redirect/get. Antes reenviaba, así que recargar reintentaba la
          transferencia —en una pantalla que mueve dinero—. El desenlace viaja
          en la sesión y lo pinta la tarjeta de resultado.
         */
        try {
            portalService.transfer(cardholderId, sourceId, destId, amount,
                    req.getParameter("description"));

            // Copia del marco "Transferencia Exitosa - Screen" (2169:410).
            mx.sgfte.core.shared.web.OperationResult.success("¡Transferencia exitosa!",
                            "Tu transferencia se realizó correctamente",
                            "TRANSACCIÓN CONFIRMADA",
                            "Los fondos se enviaron a una cuenta del mismo propósito.")
                    .amount("Monto enviado", amount)
                    .detail("Cuenta destino", peerLabel(cardholderId, destId))
                    .when(java.time.LocalDateTime.now())
                    /*
                      El marco añade "ID Transacción · 023477". No existe: una
                      transferencia escribe dos movimientos y el servicio no
                      devuelve ningún identificador único de la operación. Se
                      omite en vez de enseñar un número inventado.
                     */
                    .secondary("Volver al inicio", "/app/home")
                    .primary("Ver mi cuenta", "/app/cuenta?id=" + sourceId)
                    .flash(req.getSession());

            resp.sendRedirect(req.getContextPath() + "/app/home");
            return;
        } catch (ValidationException e) {
            // Copia del marco "Transferencia Rechazada" (2074:266).
            mx.sgfte.core.shared.web.OperationResult.rejected("Transferencia rechazada",
                            "La operación no pudo completarse",
                            String.join(" ", e.getErrors()))
                    .amount("Monto enviado", amount)
                    .detail("Cuenta origen", myLabel(cardholderId, sourceId))
                    .detail("Cuenta destino", peerLabel(cardholderId, destId))
                    .when(java.time.LocalDateTime.now())
                    .secondary("Volver al inicio", "/app/home")
                    .primary("Reintentar", "/app/home")
                    .flash(req.getSession());

            /*
              Directo al panel, NO a /app/transferencia: esa ruta ya sólo
              redirige aquí, y ResultFlashFilter habría consumido la tarjeta en
              ese GET intermedio —que no pinta nada— dejándola sin enseñar.
             */
            resp.sendRedirect(req.getContextPath() + "/app/home");
            return;
        } catch (AccountNotOwnedException e) {
            // Tampered sourceId. Say nothing specific about it.
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    /** Loads both dropdowns and shows the form. */

    /** Cómo se lee una cuenta propia en la tarjeta de resultado. */
    private String myLabel(long cardholderId, Long accountId) {
        if (accountId == null) return null;
        return portalService.myAccounts(cardholderId).stream()
                .filter(a -> a.getId() == accountId)
                .map(a -> a.getPurpose() + " · " + a.getAccountNumber())
                .findFirst()
                .orElse(null);
    }

    /** Y cómo se lee la del compañero, buscándola entre los destinos elegibles. */
    private String peerLabel(long cardholderId, Long accountId) {
        if (accountId == null) return null;
        return portalService.peersByAccount(cardholderId).values().stream()
                .flatMap(java.util.List::stream)
                .filter(p -> p.getAccountId() == accountId)
                .map(mx.sgfte.core.portal.PeerOption::getLabel)
                .findFirst()
                .orElse(null);
    }
}
