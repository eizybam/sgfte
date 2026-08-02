package mx.sgfte.core.cards.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.cards.CardService;
import mx.sgfte.core.concentrator.AccountLookupDao;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;

/**
 * Card issuing screen (admin).
 * GET  /admin/cards               -> form + (optionally) the chosen account's cards.
 * POST /admin/cards (action=issue)      -> issue a card.
 * POST /admin/cards (action=invalidate) -> invalidate a card.
 * Protected by AuthFilter (/admin/*). Reuses M1's AccountLookupDao for the dropdown.
 */
@WebServlet("/admin/cards")
public class CardServlet extends HttpServlet {

    private final CardService cardService = new CardService();
    private final AccountLookupDao accountLookupDao = new AccountLookupDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Long accountId = parseId(req.getParameter("accountId"));
        render(req, resp, accountId);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = req.getParameter("action");
        Long accountId = parseId(req.getParameter("accountId"));
        try {
            if ("invalidate".equals(action)) {
                cardService.invalidate(parseId(req.getParameter("cardId")));
                req.setAttribute("success", "Tarjeta invalidada.");
            } else {
                cardService.issue(accountId, req.getParameter("cardType"));
                req.setAttribute("success", "Tarjeta expedida.");
            }
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
        }
        render(req, resp, accountId);
    }

    private void render(HttpServletRequest req, HttpServletResponse resp, Long accountId)
            throws ServletException, IOException {
        req.setAttribute("accounts", accountLookupDao.findActiveForSelect());
        req.setAttribute("selectedAccountId", accountId);
        if (accountId != null) {
            req.setAttribute("cards", cardService.cardsOf(accountId));
        }
        req.getRequestDispatcher("/WEB-INF/jsp/admin/expedicion.jsp").forward(req, resp);
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
