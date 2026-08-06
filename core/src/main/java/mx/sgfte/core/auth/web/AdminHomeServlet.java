package mx.sgfte.core.auth.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.analytics.DashboardDao;
import mx.sgfte.core.analytics.PurposeShare;
import mx.sgfte.core.concentrator.AccountLookupDao;
import mx.sgfte.core.concentrator.web.DispersionServlet;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GET /admin/home — "Vista Global": the administrator's landing screen.
 *
 * Shows what the Figma frame "Vista principal de Admin" (201:22) shows: the
 * Concentrator balance, the headline KPIs, and how the money is split across
 * purposes. It also hosts the "Dispersión de fondos" modal (2177:376), so it
 * loads the account list the dropdown needs.
 *
 * Reads through the existing DAOs — nothing here writes.
 */
@WebServlet("/admin/home")
public class AdminHomeServlet extends HttpServlet {

    private final DashboardDao dashboardDao = new DashboardDao();
    private final AccountLookupDao accountLookupDao = new AccountLookupDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("concentratorBalance", dashboardDao.concentratorBalance());
        req.setAttribute("activeCardholders", dashboardDao.activeCardholders());
        req.setAttribute("dispersionThisMonth", dashboardDao.dispersionThisMonth());
        req.setAttribute("purposes", buildShares(dashboardDao.balanceByPurpose()));

        // Destinos del modal de dispersión
        req.setAttribute("accounts", accountLookupDao.findActiveForSelect());

        consumeFlash(req);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/home.jsp").forward(req, resp);
    }

    /**
     * Moves the outcome of a dispersion off the session and onto the request.
     *
     * It is read once and removed, so a refresh does not show the message again.
     * When the attempt failed, dispersionErrors is what tells the JSP to reopen
     * the modal with the typed values still in it.
     */
    private void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;

        String[] keys = {
                DispersionServlet.FLASH_SUCCESS,
                DispersionServlet.FLASH_ERRORS,
                DispersionServlet.FLASH_ACCOUNT,
                DispersionServlet.FLASH_AMOUNT,
                // Del fondeo: si falló, su modal se reabre con lo tecleado.
                mx.sgfte.core.concentrator.web.ConcentratorServlet.FLASH_ERRORS,
                mx.sgfte.core.concentrator.web.ConcentratorServlet.FLASH_AMOUNT,
        };
        for (String key : keys) {
            Object value = session.getAttribute(key);
            if (value != null) {
                req.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    /**
     * Turns raw per-purpose totals into percentages for the distribution panel.
     * Guards against a zero total: with no money in the system every share is 0%
     * instead of a division by zero.
     */
    private List<PurposeShare> buildShares(Map<String, BigDecimal> byPurpose) {
        BigDecimal total = byPurpose.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PurposeShare> shares = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, BigDecimal> e : byPurpose.entrySet()) {
            int percent = total.signum() == 0 ? 0
                    : e.getValue().multiply(BigDecimal.valueOf(100))
                       .divide(total, 0, RoundingMode.HALF_UP).intValue();
            shares.add(new PurposeShare(e.getKey(), e.getValue(), percent, (i % 4) + 1));
            i++;
        }
        return shares;
    }
}
