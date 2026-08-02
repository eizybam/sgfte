package mx.sgfte.core.analytics.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.analytics.DashboardDao;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Analytics microservice: returns aggregates as JSON so the dashboard (or any
 * client) can draw charts. Base version builds the JSON by hand (no extra lib).
 */
@WebServlet("/admin/analytics.json")
public class AnalyticsServlet extends HttpServlet {

    private final DashboardDao dao = new DashboardDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        StringBuilder json = new StringBuilder();
        json.append('{')
            .append("\"concentratorBalance\":").append(dao.concentratorBalance()).append(',')
            .append("\"totalInAccounts\":").append(dao.totalInAccounts()).append(',')
            .append("\"activeCardholders\":").append(dao.activeCardholders()).append(',')
            .append("\"activeAccounts\":").append(dao.activeAccounts()).append(',')
            .append("\"activeCards\":").append(dao.activeCards()).append(',')
            .append("\"movementsByType\":{");
        Map<String, Integer> counts = dao.movementCountsByType();
        int i = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (i++ > 0) json.append(',');
            json.append('"').append(e.getKey()).append("\":").append(e.getValue());
        }
        json.append("}}");

        try (PrintWriter out = resp.getWriter()) {
            out.print(json);
        }
    }
}
