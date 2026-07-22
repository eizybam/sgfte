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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        forwardForm(req, resp, categories);
    }

    private void forwardForm(HttpServletRequest req, HttpServletResponse resp, List<Category> categories)
            throws ServletException, IOException {
        req.setAttribute("cardholders", cardholderDao.findAllActive();
        req.setAttribute("categories", categories);
        req.getRequestDispatcher("/WEB-INF/jsp/accounts/form.jsp").forward(req, resp);
    }
}
