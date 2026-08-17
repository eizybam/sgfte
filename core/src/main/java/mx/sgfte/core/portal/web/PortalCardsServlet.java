package mx.sgfte.core.portal.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.portal.PortalService;

import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.shared.web.OperationResult;
import mx.sgfte.core.users.ValidationException;

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
    private final AuditLogService audit = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long cardholderId = PortalSupport.cardholderId(req);

        req.setAttribute("cards", portalService.myCards(cardholderId));
        // El modal de "Hacer un gasto" necesita el <select> de cuentas.
        req.setAttribute("myAccounts", portalService.myAccounts(cardholderId));

        req.getRequestDispatcher("/WEB-INF/jsp/app/tarjetas.jsp").forward(req, resp);
    }

    /**
     * Bloquear o reactivar UNA TARJETA PROPIA.
     *
     * cardholderId sale SIEMPRE de la sesión, nunca del formulario: es la regla
     * del área /app. El cardId sí viene del formulario, y por eso el WHERE del
     * DAO lo cruza con el dueño antes de tocar nada.
     *
     * Post/redirect/get: un F5 no debe repetir el bloqueo.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long cardholderId = PortalSupport.cardholderId(req);
        Long cardId = PortalSupport.parseId(req.getParameter("cardId"));
        boolean blocking = !"unblock".equals(req.getParameter("action"));
        HttpSession session = req.getSession();

        try {
            if (cardId == null) throw new ValidationException(java.util.List.of("Tarjeta inválida."));

            if (blocking) {
                portalService.blockMyCard(cardholderId, cardId);
                audit.record(AuditEvent.CARD_BLOCKED, "Tarjeta " + cardId, req);
                OperationResult.success("Tarjeta bloqueada",
                                "Ya no puede usarse para pagar",
                                "BLOQUEO CONFIRMADO",
                                "Puedes reactivarla tú mismo cuando la encuentres. Tu saldo no se toca.")
                        .when(java.time.LocalDateTime.now())
                        .secondary("Ver mis tarjetas", "/app/tarjetas")
                        .flash(session);
            } else {
                portalService.unblockMyCard(cardholderId, cardId);
                audit.record(AuditEvent.CARD_UNBLOCKED, "Tarjeta " + cardId, req);
                OperationResult.success("Tarjeta reactivada",
                                "Vuelve a poder usarse",
                                "REACTIVACIÓN CONFIRMADA",
                                "Tus movimientos anteriores siguen igual.")
                        .when(java.time.LocalDateTime.now())
                        .secondary("Ver mis tarjetas", "/app/tarjetas")
                        .flash(session);
            }
        } catch (ValidationException e) {
            OperationResult.rejected("No se pudo cambiar la tarjeta",
                            "La operación no se aplicó",
                            String.join(" ", e.getErrors()))
                    .when(java.time.LocalDateTime.now())
                    .secondary("Ver mis tarjetas", "/app/tarjetas")
                    .flash(session);
        }
        resp.sendRedirect(req.getContextPath() + "/app/tarjetas");
    }
}
