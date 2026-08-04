package mx.sgfte.core.portal.web;

import jakarta.servlet.ServletException;
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp, PortalSupport.parseId(req.getParameter("sourceId")));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
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

            mx.sgfte.core.shared.web.OperationResult.success("¡Transferencia enviada!",
                            "El saldo ya está en la cuenta de tu compañero",
                            "TRANSFERENCIA CONFIRMADA",
                            "Sólo se transfiere entre cuentas del mismo propósito.")
                    .amount("Monto transferido", amount)
                    .detail("Concepto", req.getParameter("description"))
                    .when(java.time.LocalDateTime.now())
                    .secondary("Volver al inicio", "/app/home")
                    .primary("Ver mi cuenta", "/app/cuenta?id=" + sourceId)
                    .flash(req.getSession());

            resp.sendRedirect(req.getContextPath() + "/app/home");
            return;
        } catch (ValidationException e) {
            mx.sgfte.core.shared.web.OperationResult.rejected("Transferencia rechazada",
                            "La operación no pudo completarse",
                            String.join(" ", e.getErrors()))
                    .amount("Monto solicitado", amount)
                    .when(java.time.LocalDateTime.now())
                    .secondary("Volver al inicio", "/app/home")
                    .primary("Reintentar", "/app/transferencia"
                            + (sourceId == null ? "" : "?sourceId=" + sourceId))
                    .flash(req.getSession());

            resp.sendRedirect(req.getContextPath() + "/app/transferencia"
                    + (sourceId == null ? "" : "?sourceId=" + sourceId));
            return;
        } catch (AccountNotOwnedException e) {
            // Tampered sourceId. Say nothing specific about it.
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    /** Loads both dropdowns and shows the form. */
    private void render(HttpServletRequest req, HttpServletResponse resp, Long sourceId)
            throws ServletException, IOException {
        long cardholderId = PortalSupport.cardholderId(req);

        req.setAttribute("myAccounts", portalService.myAccounts(cardholderId));
        req.setAttribute("selectedSourceId", sourceId);
        if (sourceId != null) {
            List<?> peers = portalService.peersFor(cardholderId, sourceId);
            req.setAttribute("peers", peers);
        }

        req.getRequestDispatcher("/WEB-INF/jsp/app/transferencia.jsp").forward(req, resp);
    }
}
