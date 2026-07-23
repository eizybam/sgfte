package mx.sgfte.core.accounts.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.accounts.Account;
import mx.sgfte.core.accounts.AccountService;
import mx.sgfte.core.categories.Category;
import mx.sgfte.core.categories.CategoryDao;
import mx.sgfte.core.users.CardholderDao;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.util.List;

@WebServlet("/accounts")
public class AccountServlet extends HttpServlet {

    private final AccountService accountService = new AccountService();
    private final CardholderDao cardholderDao = new CardholderDao();
    private final CategoryDao categoryDao = new CategoryDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        forwardForm(req, resp, categoryDao.findAllActive());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Long cardholderId = parseId(req.getParameter("cardholderId"));
        Long categoryId = parseId(req.getParameter("categoryId"));

        // Loaded once: reused for the dropdown AND to get the name for the code prefix.
        List<Category> categories = categoryDao.findAllActive();

        Account account = new Account(cardholderId, categoryId);
        try {
            long id = accountService.create(account, nameOf(categories, categoryId));
            req.setAttribute("successId", id);
            req.setAttribute("successNumber", account.getAccountNumber());
        } catch (ValidationException e) {
            req.setAttribute("errors", e.getErrors());
            // keep the user's selection so the dropdowns stay chosen
            req.setAttribute("selectedCardholderId", cardholderId);
            req.setAttribute("selectedCategoryId", categoryId);
        }
        forwardForm(req, resp, categories);
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, List<Category> categories)
            throws ServletException, IOException {
        req.setAttribute("cardholders", cardholderDao.findAllActive());
        req.setAttribute("categories", categories);
        req.getRequestDispatcher("/WEB-INF/jsp/accounts/form.jsp").forward(req, resp);
    }

    private String nameOf(List<Category> categories, Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        for (Category category : categories) {
            if (categoryId.equals(category.getId())) {
                return category.getName();
            }
        }
        return null;
    }

    /** Parses a select value into a Long, or null if empty/invalid. */
    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
