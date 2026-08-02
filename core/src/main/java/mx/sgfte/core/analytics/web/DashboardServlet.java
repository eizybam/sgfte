package mx.sgfte.core.analytics.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.analytics.DashboardDao;

import java.io.IOException;

/** Admin dashboard: KPI cards + a chart (chart data comes from /admin/analytics.json). */
@WebServlet("/admin/dashboard")
public class DashboardServlet extends HttpServlet {

    private final DashboardDao dao = new DashboardDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("concentratorBalance", dao.concentratorBalance());
        req.setAttribute("totalInAccounts", dao.totalInAccounts());
        req.setAttribute("activeCardholders", dao.activeCardholders());
        req.setAttribute("activeAccounts", dao.activeAccounts());
        req.setAttribute("activeCards", dao.activeCards());
        req.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
    }
}
