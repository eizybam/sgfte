package mx.sgfte.core.analytics.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.analytics.AnalyticsDao;
import mx.sgfte.core.analytics.AnalyticsPeriod;
import mx.sgfte.core.analytics.ChartBar;
import mx.sgfte.core.analytics.DashboardDao;
import mx.sgfte.core.analytics.PurposeShare;
import mx.sgfte.core.analytics.TopSpender;
import mx.sgfte.core.concentrator.ConcentratorDao;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GET /admin/dashboard — Figma frame "Analitica - Admin (v2)" (2029:6).
 *
 * The period selector scopes the whole page, so most figures are computed
 * twice: once over the chosen window and once over the window immediately
 * before it, which is what every "vs periodo anterior" compares against.
 *
 * Read-only. Protected by AuthFilter (/admin/*).
 */
@WebServlet("/admin/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final int TOP_SPENDERS = 5;

    private final DashboardDao dashboardDao = new DashboardDao();
    private final AnalyticsDao analyticsDao = new AnalyticsDao();
    private final ConcentratorDao concentratorDao = new ConcentratorDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        AnalyticsPeriod period = AnalyticsPeriod.of(req.getParameter("period"));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = period.start(now);
        LocalDateTime prevFrom = period.previousStart(now);

        req.setAttribute("period", period);
        req.setAttribute("periods", AnalyticsPeriod.values());

        // ---- Fila de KPIs ---------------------------------------------------
        BigDecimal balance = dashboardDao.concentratorBalance();
        req.setAttribute("concentratorBalance", balance);
        req.setAttribute("concentratorDelta", percentChange(concentratorDao.balanceAsOf(from), balance));

        BigDecimal dispersed = analyticsDao.dispersedBetween(from, now);
        req.setAttribute("dispersed", dispersed);
        req.setAttribute("dispersedDelta",
                percentChange(analyticsDao.dispersedBetween(prevFrom, from), dispersed));

        req.setAttribute("activeCardholders", dashboardDao.activeCardholders());
        req.setAttribute("newCardholders", analyticsDao.newCardholdersBetween(from, now));

        Map<String, Integer> byType = analyticsDao.activeCardsByType();
        req.setAttribute("activeCards", byType.values().stream().mapToInt(Integer::intValue).sum());
        req.setAttribute("physicalCards", byType.getOrDefault("PHYSICAL", 0));
        req.setAttribute("digitalCards", byType.getOrDefault("DIGITAL", 0));

        // ---- Gráficas y paneles ---------------------------------------------
        req.setAttribute("dispersionBars", dispersionBars(period, from, now));
        req.setAttribute("purposes", shares(analyticsDao.dispersionByPurpose(from, now)));

        int[] perDay = analyticsDao.movementsByWeekday(from, now);
        req.setAttribute("weekdayBars", weekdayBars(perDay));
        req.setAttribute("weekTotal", java.util.Arrays.stream(perDay).sum());

        req.setAttribute("topSpenders", topSpenders(from, now));

        req.setAttribute("transfersTotal", analyticsDao.transfersTotal(from, now));
        req.setAttribute("transfersCount", analyticsDao.transfersCount(from, now));
        req.setAttribute("transferPurposes", shares(analyticsDao.transfersByPurpose(from, now)));

        req.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
    }

    /**
     * Percent change between two figures.
     *
     * Returns null — which the page renders as a dash — when there is no
     * baseline or the baseline was zero. Reporting "+100%" against nothing, or
     * dividing by zero, would both be worse than admitting there is nothing to
     * compare against yet.
     */
    private BigDecimal percentChange(BigDecimal before, BigDecimal after) {
        if (before == null || before.signum() == 0 || after == null) return null;
        return after.subtract(before)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(before, 1, RoundingMode.HALF_UP);
    }

    private List<ChartBar> dispersionBars(AnalyticsPeriod period,
                                          LocalDateTime from, LocalDateTime now) {
        int buckets = period.buckets();
        BigDecimal[] totals =
                analyticsDao.dispersionBuckets(from, now, buckets, period.bucketsAreMonths());

        BigDecimal max = BigDecimal.ZERO;
        for (BigDecimal t : totals) if (t.compareTo(max) > 0) max = t;

        List<ChartBar> bars = new ArrayList<>();
        for (int i = 0; i < buckets; i++) {
            bars.add(new ChartBar(period.bucketLabel(i, now.toLocalDate()),
                                  totals[i], scale(totals[i], max), i == buckets - 1));
        }
        return bars;
    }

    /** Monday-first bars; the busiest day is the one the frame paints brighter. */
    private List<ChartBar> weekdayBars(int[] perDay) {
        String[] labels = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

        int max = 0;
        for (int n : perDay) max = Math.max(max, n);

        int busiest = -1;
        for (int i = 0; i < perDay.length && max > 0; i++) {
            if (perDay[i] == max) { busiest = i; break; }
        }

        List<ChartBar> bars = new ArrayList<>();
        for (int i = 0; i < perDay.length; i++) {
            int percent = max == 0 ? 0 : perDay[i] * 100 / max;
            bars.add(new ChartBar(labels[i], BigDecimal.valueOf(perDay[i]), percent, i == busiest));
        }
        return bars;
    }

    private List<TopSpender> topSpenders(LocalDateTime from, LocalDateTime now) {
        List<Object[]> rows = analyticsDao.topSpenders(from, now, TOP_SPENDERS);

        BigDecimal top = rows.isEmpty() ? BigDecimal.ZERO : (BigDecimal) rows.get(0)[1];
        List<TopSpender> out = new ArrayList<>();
        for (Object[] r : rows) {
            BigDecimal amount = (BigDecimal) r[1];
            out.add(new TopSpender((String) r[0], amount, scale(amount, top), (Integer) r[2]));
        }
        return out;
    }

    /** Turns per-purpose totals into shares of the window's total. */
    private List<PurposeShare> shares(Map<String, BigDecimal> byPurpose) {
        BigDecimal total = byPurpose.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

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

    /** value as a 0..100 share of max, guarding the empty case. */
    private int scale(BigDecimal value, BigDecimal max) {
        if (max == null || max.signum() == 0) return 0;
        return value.multiply(BigDecimal.valueOf(100))
                    .divide(max, 0, RoundingMode.HALF_UP).intValue();
    }
}
