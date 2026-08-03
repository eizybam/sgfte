package mx.sgfte.core.deletion.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.deletion.DeletionDao;
import mx.sgfte.core.deletion.DeletionService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;

/**
 * Employees management (admin): list active cardholders, delete one.
 * Deleting a cardholder reintegrates ALL their account balances and invalidates
 * all their cards (business rule 4).
 */
@WebServlet("/admin/empleados")
public class CardholderAdminServlet extends HttpServlet {

    private final DeletionService deletionService = new DeletionService();
    private final DeletionDao deletionDao = new DeletionDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        render(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Long cardholderId = parseId(req.getParameter("cardholderId"));
        try {
            if (cardholderId == null) throw new ValidationException(java.util.List.of("Tarjetahabiente inválido"));
            deletionService.deleteCardholder(cardholderId);
            req.setAttribute("success", "Tarjetahabiente eliminado. Saldos reintegrados y tarjetas invalidadas.");
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
        }
        render(req, resp);
    }

    private void render(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("cardholders", deletionDao.findActiveCardholders());
        req.getRequestDispatcher("/WEB-INF/jsp/admin/empleados.jsp").forward(req, resp);
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
