package mx.sgfte.core.users.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.accounts.AccountDao;
import mx.sgfte.core.accounts.AccountRow;
import mx.sgfte.core.cards.CardDao;
import mx.sgfte.core.users.CardholderDao;
import mx.sgfte.core.users.CardholderDetail;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.io.IOException;

/**
 * GET /admin/empleado?id=… — Figma frame "Detalle de Tarjetahabiente" (2074:294).
 *
 * Read-only. Every action leaves: creating an account, editing the profile,
 * managing accounts and issuing a card all go elsewhere.
 *
 * Protected by AuthFilter (/admin/*).
 */
@WebServlet("/admin/empleado")
public class CardholderDetailServlet extends HttpServlet {

    /** The frame's summary line names a few purposes and then trails off. */
    private static final int PURPOSES_SHOWN = 3;

    private final CardholderDao cardholderDao = new CardholderDao();
    private final AccountDao accountDao = new AccountDao();
    private final CardDao cardDao = new CardDao();
    private final mx.sgfte.core.departments.DepartmentDao departmentDao =
            new mx.sgfte.core.departments.DepartmentDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Long id = parseId(req.getParameter("id"));
        Optional<CardholderDetail> found =
                id == null ? Optional.empty() : cardholderDao.findDetail(id);

        if (found.isEmpty()) {
            // Un id inexistente no es un fallo del sistema: se vuelve al listado.
            resp.sendRedirect(req.getContextPath() + "/admin/empleados");
            return;
        }

        CardholderDetail person = found.get();
        List<AccountRow> accounts = accountDao.findByCardholder(person.getId());

        req.setAttribute("person", person);
        req.setAttribute("accounts", accounts);
        req.setAttribute("cards", cardDao.findByCardholder(person.getId()));
        req.setAttribute("purposeSummary", purposeSummary(accounts));
        // Para el desplegable del modal "Editar perfil".
        req.setAttribute("departmentOptions", departmentDao.findAllActive());

        HttpSession session = req.getSession(false);
        if (session != null) {
            Object errors = session.getAttribute(
                    mx.sgfte.core.users.web.CardholderServlet.FLASH_EDIT_ERRORS);
            if (errors != null) {
                req.setAttribute("editErrors", errors);
                session.removeAttribute(
                        mx.sgfte.core.users.web.CardholderServlet.FLASH_EDIT_ERRORS);
            }
        }

        req.getRequestDispatcher("/WEB-INF/jsp/admin/empleado-detalle.jsp").forward(req, resp);
    }

    /**
     * "Gasolina, Viáticos, Alimentos…" — the purposes of the ACTIVE accounts.
     *
     * A set, because someone can hold two accounts of the same purpose and the
     * line should name it once; ordered, so it does not shuffle between loads.
     */
    private String purposeSummary(List<AccountRow> accounts) {
        Set<String> purposes = new LinkedHashSet<>();
        for (AccountRow a : accounts) {
            if (a.isActive()) purposes.add(a.getPurpose());
        }
        if (purposes.isEmpty()) return "";

        List<String> shown = new ArrayList<>(purposes);
        boolean more = shown.size() > PURPOSES_SHOWN;
        if (more) shown = shown.subList(0, PURPOSES_SHOWN);

        return String.join(", ", shown) + (more ? "…" : "");
    }

    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }
}
