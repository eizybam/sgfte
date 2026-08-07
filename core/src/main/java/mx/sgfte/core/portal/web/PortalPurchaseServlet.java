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

/**
 * "Hacer un gasto" — simulates a real card purchase (Mis tarjetas).
 *
 * Same shape as PortalTransferServlet: the form is a modal, not a screen, so
 * GET has nothing to render; post/redirect/get so a refresh cannot replay a
 * purchase that already happened.
 */
@WebServlet("/app/gasto")
public class PortalPurchaseServlet extends HttpServlet {

    private final PortalService portalService = new PortalService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/app/home");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long cardholderId = PortalSupport.cardholderId(req);
        Long accountId = PortalSupport.parseId(req.getParameter("accountId"));
        BigDecimal amount = PortalSupport.parseAmount(req.getParameter("amount"));
        String merchant = req.getParameter("merchant");
        String backTo = backTo(req);

        try {
            portalService.spend(cardholderId, accountId == null ? -1 : accountId, amount, merchant);

            mx.sgfte.core.shared.web.OperationResult.success("¡Gasto registrado!",
                            "Tu compra se aplicó correctamente",
                            "COMPRA CONFIRMADA",
                            "El monto ya se descontó del saldo de la cuenta.")
                    .amount("Monto gastado", amount)
                    .detail("Comercio", merchant)
                    .detail("Cuenta", accountLabel(cardholderId, accountId))
                    .when(java.time.LocalDateTime.now())
                    .secondary("Volver al inicio", "/app/home")
                    .primary("Ver mis tarjetas", "/app/tarjetas")
                    .flash(req.getSession());

            resp.sendRedirect(req.getContextPath() + backTo);
            return;
        } catch (ValidationException e) {
            mx.sgfte.core.shared.web.OperationResult.rejected("Gasto rechazado",
                            "La operación no pudo completarse",
                            String.join(" ", e.getErrors()))
                    .amount("Monto gastado", amount)
                    .detail("Comercio", merchant)
                    .detail("Cuenta", accountLabel(cardholderId, accountId))
                    .when(java.time.LocalDateTime.now())
                    .secondary("Volver al inicio", "/app/home")
                    .primary("Reintentar", backTo)
                    .flash(req.getSession());

            resp.sendRedirect(req.getContextPath() + backTo);
            return;
        } catch (AccountNotOwnedException e) {
            // Tampered accountId. Say nothing specific about it.
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
    }

    /**
     * De vuelta a la pantalla desde la que se abrió el modal — el dashboard
     * o Mis tarjetas—, no siempre la misma. Se acepta sólo el literal
     * "tarjetas", nunca una URL, para que no sea una redirección abierta
     * (mismo truco que ConcentratorServlet.backTo()).
     */
    private String backTo(HttpServletRequest req) {
        return "tarjetas".equals(req.getParameter("returnTo")) ? "/app/tarjetas" : "/app/home";
    }

    /** Cómo se lee la cuenta en la tarjeta de resultado. */
    private String accountLabel(long cardholderId, Long accountId) {
        if (accountId == null) return null;
        return portalService.myAccounts(cardholderId).stream()
                .filter(a -> a.getId() == accountId)
                .map(a -> a.getPurpose() + " · " + a.getAccountNumber())
                .findFirst()
                .orElse(null);
    }
}
