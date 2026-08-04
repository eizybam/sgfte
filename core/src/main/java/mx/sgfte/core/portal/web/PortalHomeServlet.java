package mx.sgfte.core.portal.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.portal.PortalAccount;
import mx.sgfte.core.portal.PortalService;

import java.io.IOException;
import java.util.List;

/**
 * GET /app/home — the employee's main screen (Figma: "Vista principal de
 * Tarjetahabiente"): every account they own, its purpose, balance and card
 * count, plus the combined total.
 *
 * Replaces the welcome-message stub that used to live in auth/web.
 */
@WebServlet("/app/home")
public class PortalHomeServlet extends HttpServlet {

    private final PortalService portalService = new PortalService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long cardholderId = PortalSupport.cardholderId(req);

        List<PortalAccount> accounts = portalService.myAccounts(cardholderId);
        java.math.BigDecimal total = portalService.totalBalance(accounts);

        req.setAttribute("accounts", accounts);
        req.setAttribute("total", total);
        req.setAttribute("activity", portalService.recentActivity(cardholderId));
        req.setAttribute("monthChange", portalService.monthChangePercent(cardholderId, total));

        req.getRequestDispatcher("/WEB-INF/jsp/app/home.jsp").forward(req, resp);
    }
}
