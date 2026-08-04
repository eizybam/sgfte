package mx.sgfte.core.portal.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.portal.PortalDao;
import mx.sgfte.core.portal.PortalService;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * GET /app/movimientos — "Historial de movimientos" (Figma 109:4).
 *
 * Every movement across the cardholder's accounts, with the screen's four
 * filters. All of them live in the URL rather than in JavaScript, like the admin
 * tables: a filtered view can be linked and reloaded, and the pager is plain
 * links.
 *
 * Protected by AuthFilter (/app/*).
 */
@WebServlet("/app/movimientos")
public class PortalMovementsServlet extends HttpServlet {

    private static final int PAGE_SIZE = 5;

    private final PortalService portalService = new PortalService();
    private final PortalDao portalDao = new PortalDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        long cardholderId = PortalSupport.cardholderId(req);

        String search = trimToNull(req.getParameter("q"));
        String direction = normalizeDirection(req.getParameter("dir"));
        Long accountId = PortalSupport.parseId(req.getParameter("account"));
        String period = normalizePeriod(req.getParameter("period"));

        int total = portalDao.countHistory(cardholderId, search, direction, accountId, period);
        int pageCount = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int page = clamp(parsePage(req.getParameter("page")), pageCount);

        req.setAttribute("rows", portalDao.findHistory(cardholderId, search, direction,
                accountId, period, (page - 1) * PAGE_SIZE, PAGE_SIZE));

        BigDecimal[] totals = portalDao.historyTotals(cardholderId, search, direction, accountId, period);
        req.setAttribute("totalSpent", totals[0]);
        req.setAttribute("totalReceived", totals[1]);

        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageCount", pageCount);
        req.setAttribute("myAccounts", portalService.myAccounts(cardholderId));

        // Se devuelven para que el buscador y las píldoras se repinten con lo elegido.
        req.setAttribute("q", search);
        req.setAttribute("dir", direction);
        req.setAttribute("account", accountId);
        req.setAttribute("period", period);

        req.getRequestDispatcher("/WEB-INF/jsp/app/movimientos.jsp").forward(req, resp);
    }

    /** ALL, IN u OUT; cualquier otra cosa es ALL. */
    private String normalizeDirection(String raw) {
        if ("IN".equals(raw) || "OUT".equals(raw)) return raw;
        return "ALL";
    }

    /**
     * El marco arranca en "FECHA · HOY", pero aquí el valor por omisión es
     * TODOS: una cuenta recién creada no tiene movimientos de hoy, y abrir el
     * historial en una tabla vacía parece que está roto.
     */
    private String normalizePeriod(String raw) {
        if ("HOY".equals(raw) || "7D".equals(raw) || "30D".equals(raw)) return raw;
        return "TODOS";
    }

    private int parsePage(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try { return Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { return 1; }
    }

    private int clamp(int page, int pageCount) {
        return page < 1 ? 1 : Math.min(page, pageCount);
    }

    private String trimToNull(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
