package mx.sgfte.core.portal.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.notifications.NotificationLogDao;

import java.io.IOException;

/**
 * GET /app/notificaciones — "Notificaciones": every email the cardholder has
 * been sent, newest first.
 *
 * Same shape as PortalMovementsServlet: the one filter lives in the URL, not
 * in JavaScript, so a filtered view can be linked and reloaded. Protected by
 * AuthFilter (/app/*).
 */
@WebServlet("/app/notificaciones")
public class PortalNotificationsServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;

    private final NotificationLogDao notificationLogDao = new NotificationLogDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        long cardholderId = PortalSupport.cardholderId(req);
        String category = normalizeCategory(req.getParameter("cat"));

        int total = notificationLogDao.count(cardholderId, category);
        int pageCount = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int page = clamp(parsePage(req.getParameter("page")), pageCount);

        req.setAttribute("rows", notificationLogDao.find(cardholderId, category,
                (page - 1) * PAGE_SIZE, PAGE_SIZE));
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageCount", pageCount);
        req.setAttribute("cat", category);

        req.getRequestDispatcher("/WEB-INF/jsp/app/notificaciones.jsp").forward(req, resp);
    }

    /** SEGURIDAD, ADMINISTRATIVA, o null (todas); cualquier otra cosa es null. */
    private String normalizeCategory(String raw) {
        if ("SEGURIDAD".equals(raw) || "ADMINISTRATIVA".equals(raw)) return raw;
        return null;
    }

    private int parsePage(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try { return Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { return 1; }
    }

    private int clamp(int page, int pageCount) {
        return page < 1 ? 1 : Math.min(page, pageCount);
    }
}
