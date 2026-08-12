package mx.sgfte.core.analytics.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.analytics.AnalyticsPeriod;
import mx.sgfte.core.analytics.AnalyticsReport;
import mx.sgfte.core.analytics.AnalyticsReportService;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * GET /admin/dashboard — marco Figma "Analitica - Admin (v2)" (2029:6).
 *
 * El selector de periodo re-escala toda la página, así que casi todas las
 * cifras se calculan dos veces: sobre la ventana elegida y sobre la
 * inmediatamente anterior, que es contra la que compara cada "vs periodo
 * anterior". Ese cálculo vive ahora en AnalyticsReportService, compartido con
 * la exportación a CSV; aquí sólo se reparte a la vista.
 *
 * Sólo lectura. Protegido por AuthFilter (/admin/*).
 */
@WebServlet("/admin/dashboard")
public class DashboardServlet extends HttpServlet {

    private final AnalyticsReportService reports = new AnalyticsReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        AnalyticsPeriod period = AnalyticsPeriod.of(req.getParameter("period"));
        AnalyticsReport r = reports.build(period, LocalDateTime.now());

        req.setAttribute("period", period);
        req.setAttribute("periods", AnalyticsPeriod.values());

        req.setAttribute("concentratorBalance", r.getConcentratorBalance());
        req.setAttribute("concentratorDelta", r.getConcentratorDelta());
        req.setAttribute("dispersed", r.getDispersed());
        req.setAttribute("dispersedDelta", r.getDispersedDelta());

        req.setAttribute("activeCardholders", r.getActiveCardholders());
        req.setAttribute("newCardholders", r.getNewCardholders());
        req.setAttribute("activeCards", r.getActiveCards());
        req.setAttribute("physicalCards", r.getPhysicalCards());
        req.setAttribute("digitalCards", r.getDigitalCards());

        req.setAttribute("dispersionBars", r.getDispersionBars());
        req.setAttribute("purposes", r.getPurposes());
        req.setAttribute("weekdayBars", r.getWeekdayBars());
        req.setAttribute("weekTotal", r.getWeekTotal());
        req.setAttribute("topSpenders", r.getTopSpenders());

        req.setAttribute("transfersTotal", r.getTransfersTotal());
        req.setAttribute("transfersCount", r.getTransfersCount());
        req.setAttribute("transferPurposes", r.getTransferPurposes());

        req.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
    }
}
