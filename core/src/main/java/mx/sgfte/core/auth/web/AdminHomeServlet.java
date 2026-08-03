package mx.sgfte.core.auth.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.analytics.DashboardDao;
import mx.sgfte.core.analytics.PurposeShare;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GET /admin/home — "Vista Global": the administrator's landing screen.
 *
 * Used to be a placeholder with only a logout button. Now it shows what the
 * Figma frame "Vista principal de Admin" (201:22) shows: the Concentrator
 * balance, the headline KPIs, and how the money is split across purposes.
 *
 * Reads through the existing analytics DAO — no new query layer, and nothing
 * here writes.
 */
@WebServlet("/admin/home")
public class AdminHomeServlet extends HttpServlet {

    private final DashboardDao dashboardDao = new DashboardDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("concentratorBalance", dashboardDao.concentratorBalance());
        req.setAttribute("totalInAccounts", dashboardDao.totalInAccounts());
        req.setAttribute("activeCardholders", dashboardDao.activeCardholders());
        req.setAttribute("activeAccounts", dashboardDao.activeAccounts());
        req.setAttribute("activeCards", dashboardDao.activeCards());
        req.setAttribute("purposes", buildShares(dashboardDao.balanceByPurpose()));

        req.getRequestDispatcher("/WEB-INF/jsp/admin/home.jsp").forward(req, resp);
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
