package mx.sgfte.core.cards.web;

import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.audit.AuditEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.cards.CardDao;
import mx.sgfte.core.cards.CardService;
import mx.sgfte.core.cards.IssueTarget;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Card issuing screen (admin) — Figma frame "Expedir Tarjeta - Admin" (2038:129).
 *
 * GET  /admin/cards                     -> the issuing form.
 * POST /admin/cards (action=issue)      -> issue a card.
 * POST /admin/cards (action=invalidate) -> invalidate a card.
 *
 * The POST redirects instead of forwarding. Issuing a card is a side effect, and
 * the old forward meant a browser refresh silently issued a second one.
 *
 * Protected by AuthFilter (/admin/*).
 */
@WebServlet("/admin/cards")
public class CardServlet extends HttpServlet {

    private static final String FLASH_SUCCESS = "success";
    private static final String FLASH_ERRORS  = "errors";

    private final CardService cardService = new CardService();
    private final CardDao cardDao = new CardDao();
    private final AuditLogService audit = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        /*
          La pantalla arranca vacía: sin tarjetahabiente elegido no hay ninguna
          cuenta que enseñar, y consultarlas todas para descartarlas en el
          navegador era justamente el problema.

          El único caso con datos es la vuelta de una expedición: el POST redirige
          con ?accountId=, y ahí sí conviene dejar el formulario como estaba para
          "Expedir otra". Como la cuenta ya se sabe, se averigua de quién era y se
          cargan sólo las suyas — dos consultas acotadas, y sólo en ese camino.
        */
        Long selectedAccountId = parseId(req.getParameter("accountId"));
        if (selectedAccountId != null) {
            cardDao.findIssueTarget(selectedAccountId).ifPresent(t -> {
                req.setAttribute("selectedAccountId", t.getAccountId());
                req.setAttribute("selectedHolderId", t.getCardholderId());
                req.setAttribute("selectedHolderName", t.getCardholderName());
                req.setAttribute("targets", cardDao.findIssueTargets(t.getCardholderId()));
            });
        }

        consumeFlash(req);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/expedicion.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String action = req.getParameter("action");
        Long accountId = parseId(req.getParameter("accountId"));
        HttpSession session = req.getSession();

        try {
            if ("block".equals(action) || "unblock".equals(action)) {
                Long cardId = parseId(req.getParameter("cardId"));
                boolean blocking = "block".equals(action);
                if (blocking) cardService.block(cardId); else cardService.unblock(cardId);

                audit.record(blocking ? AuditEvent.CARD_BLOCKED : AuditEvent.CARD_UNBLOCKED,
                             "Tarjeta " + cardId, req);

                mx.sgfte.core.shared.web.OperationResult.success(
                                blocking ? "Tarjeta bloqueada" : "Tarjeta reactivada",
                                blocking ? "Queda suspendida temporalmente" : "Vuelve a poder usarse",
                                blocking ? "BLOQUEO CONFIRMADO" : "REACTIVACIÓN CONFIRMADA",
                                blocking
                                    ? "Deja de pagar de inmediato, pero no se anula: puede reactivarse."
                                    : "Vuelve a pagar con el saldo disponible de la cuenta.")
                        .detail("Tarjeta", "Nº " + cardId)
                        .when(java.time.LocalDateTime.now())
                        .secondary("Volver", backTo(req, accountId))
                        .flash(session);
            } else if ("invalidate".equals(action)) {
                Long cardId = parseId(req.getParameter("cardId"));
                cardService.invalidate(cardId);
                audit.record(AuditEvent.CARD_INVALIDATED, "Tarjeta " + cardId, req);

                mx.sgfte.core.shared.web.OperationResult.success("Tarjeta invalidada",
                                "La tarjeta ya no puede usarse",
                                "INVALIDACIÓN CONFIRMADA",
                                "Es definitivo: el saldo de la cuenta no se toca, pero la tarjeta no vuelve. Para dar servicio otra vez hay que expedir una nueva.")
                        .detail("Tarjeta", "Nº " + cardId)
                        .when(java.time.LocalDateTime.now())
                        .secondary("Volver", backTo(req, accountId))
                        .flash(session);
            } else {
                cardService.issue(accountId, req.getParameter("cardType"));
                audit.record(AuditEvent.CARD_ISSUED,
                        req.getParameter("cardType") + " · cuenta " + accountId, req);

                mx.sgfte.core.shared.web.OperationResult.success("¡Tarjeta expedida!",
                                "La tarjeta quedó ligada a la cuenta",
                                "EXPEDICIÓN CONFIRMADA",
                                "Ya puede usarse con el saldo disponible de la cuenta.")
                        .detail("Tipo", "PHYSICAL".equals(req.getParameter("cardType")) ? "Física" : "Digital")
                        .when(java.time.LocalDateTime.now())
                        .secondary("Expedir otra", "/admin/cards")
                        .primary("Ver cuenta", "/admin/cuenta?id=" + accountId)
                        .flash(session);
            }
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
        }

        resp.sendRedirect(req.getContextPath() + backTo(req, accountId));
    }

    /**
     * A dónde volver después del POST.
     *
     * Bloquear o invalidar una tarjeta se hace DESDE el detalle de la cuenta, y
     * mandar al admin a "Expedir tarjeta" después de anular una es cambiarle de
     * pantalla sin motivo: pierde el contexto y tiene que volver a navegar.
     * Expedir sí se queda donde estaba, porque ahí encadena "Expedir otra".
     *
     * returnTo se compara contra un literal y NUNCA se usa como URL — misma
     * regla que PortalPurchaseServlet.backTo(): un parámetro que se concatena a
     * un sendRedirect es una redirección abierta.
     */
    private String backTo(HttpServletRequest req, Long accountId) {
        if ("cuenta".equals(req.getParameter("returnTo")) && accountId != null) {
            return "/admin/cuenta?id=" + accountId;
        }
        if (accountId != null) {
            return "/admin/cards?accountId="
                 + URLEncoder.encode(accountId.toString(), StandardCharsets.UTF_8);
        }
        return "/admin/cards";
    }

    /** Reads the one-shot outcome left by the POST and clears it. */
    private void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;

        for (String key : new String[]{FLASH_SUCCESS, FLASH_ERRORS}) {
            Object value = session.getAttribute(key);
            if (value != null) {
                req.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
