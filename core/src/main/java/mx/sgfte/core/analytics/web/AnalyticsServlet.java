package mx.sgfte.core.analytics.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.analytics.AnalyticsDao;
import mx.sgfte.core.analytics.AnalyticsPeriod;
import mx.sgfte.core.analytics.DashboardDao;
import mx.sgfte.core.concentrator.ConcentratorDao;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Analytics microservice: the same aggregates the Analíticas screen draws,
 * as JSON, for any other client.
 *
 * It accepts the same ?period= as the screen and reads the same DAOs over the
 * same window, so the endpoint and the page cannot drift into two different
 * ideas of what "this month" means.
 *
 * Still builds the JSON by hand: one endpoint does not justify a dependency.
 */
@WebServlet("/admin/analytics.json")
public class AnalyticsServlet extends HttpServlet {

    private final DashboardDao dashboardDao = new DashboardDao();
    private final AnalyticsDao analyticsDao = new AnalyticsDao();
    private final ConcentratorDao concentratorDao = new ConcentratorDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        AnalyticsPeriod period = AnalyticsPeriod.of(req.getParameter("period"));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = period.start(now);
        LocalDateTime prevFrom = period.previousStart(now);

        Map<String, Integer> cardsByType = analyticsDao.activeCardsByType();

        StringBuilder json = new StringBuilder();
        json.append('{')
            .append("\"period\":\"").append(period.getCode()).append("\",")
            .append("\"from\":\"").append(from).append("\",")
            .append("\"to\":\"").append(now).append("\",")
            .append("\"concentratorBalance\":").append(dashboardDao.concentratorBalance()).append(',')
            .append("\"concentratorBalanceBefore\":").append(nullable(concentratorDao.balanceAsOf(from))).append(',')
            .append("\"dispersed\":").append(analyticsDao.dispersedBetween(from, now)).append(',')
            .append("\"dispersedPrevious\":").append(analyticsDao.dispersedBetween(prevFrom, from)).append(',')
            .append("\"activeCardholders\":").append(dashboardDao.activeCardholders()).append(',')
            .append("\"newCardholders\":").append(analyticsDao.newCardholdersBetween(from, now)).append(',')
            .append("\"activeAccounts\":").append(dashboardDao.activeAccounts()).append(',')
            .append("\"cardsPhysical\":").append(cardsByType.getOrDefault("PHYSICAL", 0)).append(',')
            .append("\"cardsDigital\":").append(cardsByType.getOrDefault("DIGITAL", 0)).append(',')
            .append("\"transfersTotal\":").append(analyticsDao.transfersTotal(from, now)).append(',')
            .append("\"transfersCount\":").append(analyticsDao.transfersCount(from, now)).append(',');

        appendPurposes(json, "dispersionByPurpose", analyticsDao.dispersionByPurpose(from, now));
        json.append(',');
        appendPurposes(json, "transfersByPurpose", analyticsDao.transfersByPurpose(from, now));

        json.append(",\"movementsByWeekday\":[");
        int[] perDay = analyticsDao.movementsByWeekday(from, now);
        for (int i = 0; i < perDay.length; i++) {
            if (i > 0) json.append(',');
            json.append(perDay[i]);
        }
        json.append("]}");

        try (PrintWriter out = resp.getWriter()) {
            out.print(json);
        }
    }

    private void appendPurposes(StringBuilder json, String key, Map<String, BigDecimal> byPurpose) {
        json.append('"').append(key).append("\":{");
        int i = 0;
        for (Map.Entry<String, BigDecimal> e : byPurpose.entrySet()) {
            if (i++ > 0) json.append(',');
            json.append('"').append(escape(e.getKey())).append("\":").append(e.getValue());
        }
        json.append('}');
    }

    /** A missing baseline is null, not 0 — the difference matters to a consumer. */
    private String nullable(BigDecimal value) {
        return value == null ? "null" : value.toString();
    }

    /** Category names come from the catalogue, so they can carry quotes. */
    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
