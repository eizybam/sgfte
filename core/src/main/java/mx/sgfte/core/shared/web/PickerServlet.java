package mx.sgfte.core.shared.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.cards.CardDao;
import mx.sgfte.core.users.CardholderDao;

import java.io.IOException;
import java.sql.SQLException;

import static java.lang.Math.clamp;

@WebServlet("/admin/picker")
public class PickerServlet extends HttpServlet {
    private static final int PAGE_SIZE = 6;

    private final CardholderDao cardholderDao = new CardholderDao();
    private final CardDao cardDao = new CardDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");

        String type = req.getParameter("type");
        if ("cardholder".equals(type)) {
            try {
                cardholders(req, resp);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else if ("issue-options".equals(type)) {
            issueOptions(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown picker type: " + type);
        }
    }

    private void cardholders(HttpServletRequest req, HttpServletResponse resp) throws IOException, SQLException, ServletException {
        String search = trimToNull(req.getParameter("q"));

        int total = cardholderDao.countForPicker(search);
        int pageCount = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int page = clamp(parsePage(req.getParameter("page")), pageCount);

        req.setAttribute("rows", cardholderDao.findForPicker(search, (page -1) * PAGE_SIZE, PAGE_SIZE));
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageCount", pageCount);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/picker-cardholders.jsp").forward(req, resp);
    }

    private void issueOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Long cardholderId = parseId(req.getParameter("cardholderId"));
        if (cardholderId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "cardholderId inválido");
            return;
        }
        req.setAttribute("targets", cardDao.findIssueTargets(cardholderId));
        req.getRequestDispatcher("/WEB-INF/jsp/admin/picker-issue-options.jsp").forward(req, resp);
    }

    private int clamp(int page, int pageCount) {
        return Math.min(Math.max(page, 1), pageCount);
    }

    private int parsePage(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try { return Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { return 1; }
    }

    private String trimToNull(String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
