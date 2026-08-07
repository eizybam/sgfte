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

        List<IssueTarget> targets = cardDao.findIssueTargets();

        req.setAttribute("targets", targets);
        req.setAttribute("holders", distinctHolders(targets));
        req.setAttribute("selectedAccountId", parseId(req.getParameter("accountId")));
        consumeFlash(req);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/expedicion.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String action = req.getParameter("action");
        Long accountId = parseId(req.getParameter("accountId"));
        HttpSession session = req.getSession();

        try {
            if ("invalidate".equals(action)) {
                Long cardId = parseId(req.getParameter("cardId"));
                cardService.invalidate(cardId);
                audit.record(AuditEvent.CARD_INVALIDATED, "Tarjeta " + cardId, req);

                mx.sgfte.core.shared.web.OperationResult.success("Tarjeta invalidada",
                                "La tarjeta ya no puede usarse",
                                "INVALIDACIÓN CONFIRMADA",
                                "El saldo de la cuenta no se toca; sólo se anula el plástico.")
                        .detail("Tarjeta", "Nº " + cardId)
                        .when(java.time.LocalDateTime.now())
                        .secondary("Volver", "/admin/cards")
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

        String back = req.getContextPath() + "/admin/cards";
        if (accountId != null) {
            back += "?accountId=" + URLEncoder.encode(accountId.toString(), StandardCharsets.UTF_8);
        }
        resp.sendRedirect(back);
    }

    /**
     * The cardholders behind the targets, once each, keeping the query's order.
     * A holder with three accounts must still appear once in the first dropdown.
     */
    private Map<Long, String> distinctHolders(List<IssueTarget> targets) {
        Map<Long, String> holders = new LinkedHashMap<>();
        for (IssueTarget t : targets) {
            holders.putIfAbsent(t.getCardholderId(), t.getCardholderName());
        }
        return holders;
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
