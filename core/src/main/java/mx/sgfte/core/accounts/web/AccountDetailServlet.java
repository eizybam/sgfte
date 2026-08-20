package mx.sgfte.core.accounts.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.accounts.AccountDao;
import mx.sgfte.core.accounts.AccountDetail;
import mx.sgfte.core.accounts.AccountMonthSummary;
import mx.sgfte.core.accounts.AccountMovementRow;
import mx.sgfte.core.cards.CardService;
import mx.sgfte.core.movements.Movement;
import mx.sgfte.core.movements.MovementQueryDao;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * GET /admin/cuenta?id=… — Figma frame "Detalle de Cuenta - Admin" (2043:155).
 *
 * Read-only. Every action on the screen is a link somewhere else: depositing
 * opens the dispersion modal (which posts to /admin/dispersion and comes back
 * here), issuing a card goes to /admin/cards with this account preselected, and
 * the full history goes to /admin/movimientos filtered to this account.
 *
 * Protected by AuthFilter (/admin/*).
 */
@WebServlet("/admin/cuenta")
public class AccountDetailServlet extends HttpServlet {

    /** The frame lists five; more would overflow its card. */
    private static final int RECENT_MOVEMENTS = 5;

    /** Movement types that add to the account's balance. */
    private static final Set<String> INFLOWS = Set.of("DEPOSIT", "TRANSFER_IN");

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM", ES_MX);
    private static final DateTimeFormatter DAY_YEAR = DateTimeFormatter.ofPattern("dd MMM yyyy", ES_MX);

    private final AccountDao accountDao = new AccountDao();
    private final CardService cardService = new CardService();
    private final MovementQueryDao movementQueryDao = new MovementQueryDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Long accountId = parseId(req.getParameter("id"));
        Optional<AccountDetail> found =
                accountId == null ? Optional.empty() : accountDao.findDetail(accountId);

        if (found.isEmpty()) {
            // Un id inexistente no es un error del sistema: se vuelve al listado.
            resp.sendRedirect(req.getContextPath() + "/admin/cuentas");
            return;
        }

        AccountDetail account = found.get();

        AccountMonthSummary summary = accountDao.monthSummary(account.getId());

        req.setAttribute("account", account);
        req.setAttribute("cards", cardService.cardsOf(account.getId()));
        req.setAttribute("summary", summary);
        req.setAttribute("movements", recentMovements(account.getId()));

        // Se formatea aquí porque JSTL no sabe con java.time.
        if (summary.getLastDeposit() != null) {
            // En la zona de la empresa: lo guardado es UTC, y una recarga de
            // las 19:00 salía fechada al día siguiente.
            req.setAttribute("lastDepositLabel", capitalizeMonth(
                    mx.sgfte.core.shared.time.AppTime.display(summary.getLastDeposit())
                            .format(DAY_YEAR)));
        }

        consumeFlash(req);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/cuenta-detalle.jsp").forward(req, resp);
    }

    /**
     * The newest movements, already shaped for the view: the query returns them
     * newest first, so this only needs to take the first few.
     */
    private List<AccountMovementRow> recentMovements(long accountId) {
        List<Movement> all = movementQueryDao.findByAccount(accountId);
        List<AccountMovementRow> rows = new ArrayList<>();

        for (Movement m : all.subList(0, Math.min(RECENT_MOVEMENTS, all.size()))) {
            rows.add(new AccountMovementRow(
                    capitalizeMonth(m.getCreatedAt().format(DAY)),
                    conceptOf(m),
                    m.getAmount(),
                    INFLOWS.contains(m.getMovementType())));
        }
        return rows;
    }

    /** The ledger's free-text note, or a readable fallback built from the type. */
    private String conceptOf(Movement m) {
        if (m.getDescription() != null && !m.getDescription().isBlank()) {
            return m.getDescription();
        }
        return switch (m.getMovementType()) {
            case "DEPOSIT"       -> "Recarga de cuenta";
            case "WITHDRAWAL"    -> "Retiro";
            case "TRANSFER_IN"   -> "Transferencia recibida";
            case "TRANSFER_OUT"  -> "Transferencia enviada";
            case "REINTEGRATION" -> "Reintegración a Concentradora";
            default              -> m.getMovementType();
        };
    }

    /** Spanish months come out lowercase; the frame shows "12 Jun". */
    private String capitalizeMonth(String formatted) {
        int space = formatted.indexOf(' ');
        if (space < 0 || space + 1 >= formatted.length()) return formatted;
        return formatted.substring(0, space + 1)
             + Character.toUpperCase(formatted.charAt(space + 1))
             + formatted.substring(space + 2);
    }

    private void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;
        for (String key : new String[]{"success", "errors", "dispersionErrors"}) {
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
