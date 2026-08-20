package mx.sgfte.core.portal.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.portal.AccountNotOwnedException;
import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.portal.PeerOption;
import mx.sgfte.core.portal.PortalService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * P2P transfer for the employee (HU-07, Figma: "Transferencia").
 *
 * The source dropdown lists only the employee's OWN accounts. The destination
 * is typed, not picked: the colleague passes on the identifier of their account
 * ("GAS-48HSY") and this servlet resolves it — active, someone else's, and of
 * the same purpose (RN-07). Nobody gets a list of everyone else's accounts.
 *
 * Note this is the same TransferService the admin screen calls. The rule about
 * matching purposes is not re-implemented here — this servlet only adds "the
 * source must be mine".
 */
@WebServlet("/app/transferencia")
public class PortalTransferServlet extends HttpServlet {

    private final PortalService portalService = new PortalService();
    private final AuditLogService audit = new AuditLogService();

    /** El formulario es un modal del panel; aquí no hay pantalla que pintar. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/app/home");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long cardholderId = PortalSupport.cardholderId(req);
        Long sourceId = PortalSupport.parseId(req.getParameter("sourceId"));
        String destNumber = shortened(req.getParameter("destNumber"));
        BigDecimal amount = PortalSupport.parseAmount(req.getParameter("amount"));

        /*
          El destino llega escrito a mano ("GAS-48HSY"), no elegido de una lista,
          así que hay que traducirlo a una cuenta antes de mover nada. Se traduce
          AQUÍ y no en el navegador: la comprobación en vivo mientras teclea es
          una cortesía de la pantalla, y una cortesía no puede ser lo que decide
          a dónde va el dinero. Es la misma consulta, así que un identificador
          que allí salió bien sale bien aquí — salvo que la cuenta haya cambiado
          entre medias, que es justo el caso que esto atrapa.
         */
        PeerOption dest = portalService.peerByNumber(cardholderId, sourceId, destNumber).orElse(null);
        Long destId = dest == null ? null : dest.getAccountId();

        // Para la tarjeta de resultado: el nombre del compañero si se resolvió,
        // y si no, lo que se tecleó — que es lo que hay que corregir.
        String destLabel = dest != null ? dest.getLabel() : destNumber;

        /*
          Post/redirect/get. Antes reenviaba, así que recargar reintentaba la
          transferencia —en una pantalla que mueve dinero—. El desenlace viaja
          en la sesión y lo pinta la tarjeta de resultado.
         */
        try {
            /*
              Un origen nulo NO se atrapa aquí: se deja pasar para que
              PortalService lance AccountNotOwnedException y la respuesta sea un
              404, como cualquier id manipulado. Decir "identificador inválido"
              cuando lo manipulado es el origen confirmaría de qué se trata.
             */
            if (sourceId != null && dest == null) {
                throw new ValidationException(List.of(
                        "No hay ninguna cuenta con el identificador \"" + destNumber + "\" "
                                + "para el propósito de tu cuenta origen. Pídele a tu compañero "
                                + "el que aparece en su pantalla de cuenta."));
            }

            portalService.transfer(cardholderId, sourceId, destId, amount,
                    req.getParameter("description"));

            // El actor aquí es el tarjetahabiente, no un administrador: la
            // bitácora guarda su correo, que es con lo que inició sesión.
            audit.record(AuditEvent.TRANSFER,
                    "P2P de " + sourceId + " a " + destId + " · $" + amount, req);

            // Copia del marco "Transferencia Exitosa - Screen" (2169:410).
            mx.sgfte.core.shared.web.OperationResult.success("¡Transferencia exitosa!",
                            "Tu transferencia se realizó correctamente",
                            "TRANSACCIÓN CONFIRMADA",
                            "Los fondos se enviaron a una cuenta del mismo propósito.")
                    .amount("Monto enviado", amount)
                    .detail("Cuenta destino", destLabel)
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
            audit.record(AuditEvent.TRANSFER_REJECTED,
                    "P2P de " + sourceId + " a " + destId + " · " + String.join(" ", e.getErrors()), req);

            // Copia del marco "Transferencia Rechazada" (2074:266).
            mx.sgfte.core.shared.web.OperationResult.rejected("Transferencia rechazada",
                            "La operación no pudo completarse",
                            String.join(" ", e.getErrors()))
                    .amount("Monto enviado", amount)
                    .detail("Cuenta origen", myLabel(cardholderId, sourceId))
                    .detail("Cuenta destino", destLabel)
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

    /**
     * Lo tecleado, recortado a lo que puede ser un identificador.
     *
     * Se repite en el mensaje de rechazo ("no hay ninguna cuenta con ..."), y el
     * maxlength del campo sólo obliga al navegador. Un identificador son 20
     * caracteres largos; más que eso no es un identificador mal escrito, es
     * alguien probando qué hace la pantalla con un texto de 10 KB.
     */
    private String shortened(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.length() <= 20 ? trimmed : trimmed.substring(0, 20) + "…";
    }

    /** Cómo se lee una cuenta propia en la tarjeta de resultado. */
    private String myLabel(long cardholderId, Long accountId) {
        if (accountId == null) return null;
        return portalService.myAccounts(cardholderId).stream()
                .filter(a -> a.getId() == accountId)
                .map(a -> a.getPurpose() + " · " + a.getAccountNumber())
                .findFirst()
                .orElse(null);
    }

}
