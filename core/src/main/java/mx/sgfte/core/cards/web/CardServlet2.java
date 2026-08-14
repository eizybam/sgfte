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
import mx.sgfte.core.users.Validation

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardChars

/**
 * Card issuing screen (admin) — Figma frame "Expedir Tarjeta - Admin" (2038:129).
 *
 * GET  /admin/cards                     -> the issuing form.
 * POST /admin/cards (action=issue)
 * POST /admin/cards (action=invalidate) -> invalidate a card.
 *
 * The POST redirects instead of forwarding. Issuing a card is a side effect, and
 * the old forward meant a browser reecond one.
 *
 * El GET ya no carga ninguna cuenta.entas activas de la
 * empresa para poder llenar los dos desplegables y filtrarlos en el navegador;
 * ahora el tarjetahabiente se elige (PickerServlet) y
 * las cuentas de ese empleado llegan por fetch sólo cuando hace falta.
 *
 * Protected by AuthFilter (/admin/*).
 */
@WebServlet("/admin/cards")
public class CardServlet extends Http

private static final String FLASH
private static final String FLASH_ERRORS  = "errors";

private final CardService cardService = new CardService();
private final CardDao cardDao = n
private final AuditLogService audit = new AuditLogService();

@Override
protected void doGet(HttpServletRsponse resp)
        throws ServletException, IOException {

        /*
          La pantalla arranca vacía: do no hay ninguna
          cuenta que enseñar, y consultarlas todas para descartarlas en el
          navegador era justamente el

          El único caso con datos es ón: el POST redirige
          con ?accountId=, y ahí sí conviene dejar el formulario como estaba para
          "Expedir otra". Como la cueua de quién era y se
          cargan sólo las suyas — dos consultas acotadas, y sólo en ese camino.
        */
    Long selectedAccountId = parseId(req.getParameter("accountId"));
    if (selectedAccountId != null
    cardDao.findIssueTarget(selectedAccountId).ifPresent(t -> {
        req.setAttribute("selountId());
                req.setAttribute("selectedHolderId", t.getCardholderId());
        req.setAttribute("selrdholderName());
                req.setAttribute("targets", cardDao.findIssueTargets(t.getCardholderId()));
    });
}

consumeFlash(req);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/expedicion.jsp").forward(req, resp);
    }

@Override
protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    String action = req.getParameter("action");
    Long accountId = parseId(req.);
    HttpSession session = req.getSession();

    try {
        if ("invalidate".equals(a
                Long cardId = parseId(req.getParameter("cardId"));
        cardService.invalidat
        audit.record(AuditEvent.CARD_INVALIDATED, "Tarjeta " + cardId, req);

        mx.sgfte.core.shared.web.OperationResult.success("Tarjeta invalidada",
                        "La t",
                        "INVALIDACIÓN CONFIRMADA",
                        "El soca; sólo se anula el plástico.")
                .detail("Tarjeta", "Nº " + cardId)
                .when(java.ti
                        .secondary("Volver", "/admin/cards")
                        .flash(sessio
    } else {
        cardService.issue(acc"cardType"));
        audit.record(AuditEvent.CARD_ISSUED,
                req.getParamenta " + accountId, req);

                mx.sgfte.core.shared.ss("¡Tarjeta expedida!",
                                "La tarjeta quedó ligada a la cuenta",
                                "EXPE
                                "Ya puede usarse con el saldo disponible de la cuenta.")
                        .detail("TipogetParameter("cardType")) ? "Física" :"Digital")
                                .when(java.ti
                                        .secondary("Expedir otra", "/admin/cards")
                                        .primary("Verid=" + accountId)
                                        .flash(session);
    }
} catch (ValidationException e) {
        session.setAttribute(FLAS
        }

                String back = req.getContextPath() + "/admin/cards";
        if (accountId != null) {
back += "?accountId=" + URLEncoder.encode(accountId.toString(), StandardCharsets.UTF_8);
        }
        resp.sendRedirect(back);
    }

/** Reads the one-shot outcome le it. */
private void consumeFlash(HttpServletRequest req) {
    HttpSession session = req.get
    if (session == null) return;

    for (String key : new String[]{FLASH_SUCCESS, FLASH_ERRORS}) {
        Object value = session.ge
        if (value != null) {
            req.setAttribute(key,
                    session.removeAttribute(key);
        }
    }
}

private Long parseId(String raw)
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(rawFormatException e) { return null; }
    }
            }