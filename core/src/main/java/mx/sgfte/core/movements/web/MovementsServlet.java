package mx.sgfte.core.movements.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.categories.CategoryDao;
import mx.sgfte.core.movements.MovementQueryDao;

import java.io.IOException;

/**
 * GET /admin/movimientos — la vista global del ledger.
 *
 * Es la contraparte de /admin/logs: la bitácora dice quién hizo qué, ésta dice
 * cuánto dinero se movió. Lee v_movement, que une los dos libros del sistema
 * —el de las cuentas y el de la Concentradora—, así que "todos los
 * movimientos" aquí significa realmente todos, incluido el fondeo, que no toca
 * ninguna cuenta y por tanto no aparece en account_movement.
 *
 * Todos los filtros viajan en la URL, igual que en las demás tablas: una vista
 * filtrada se puede enlazar, recargar y pegar. De hecho la Concentradora la
 * enlaza dos veces (?ambito=CONC), que es lo que sustituyó a sus recortes
 * ?ledger=all.
 *
 * Sólo lectura: los dos ledgers son inmutables por disparador. No hay POST, no
 * se mueve dinero y no hay nada que auditar — igual que la pantalla de logs.
 *
 * Protegido por AuthFilter (/admin/*).
 */
@WebServlet("/admin/movimientos")
public class MovementsServlet extends HttpServlet {

    /** Diez caben sin que la tabla obligue a hacer scroll a 1440. */
    private static final int PAGE_SIZE = 10;

    private final MovementQueryDao movements = new MovementQueryDao();
    private final CategoryDao categoryDao = new CategoryDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String search   = trimToNull(req.getParameter("q"));
        String scope    = normalizeScope(req.getParameter("ambito"));
        String type     = trimToNull(req.getParameter("tipo"));
        Long categoryId = parseId(req.getParameter("cat"));
        Long accountId  = parseId(req.getParameter("cuenta"));
        String period   = normalizePeriod(req.getParameter("period"));

        int total = movements.countGlobal(search, scope, type, categoryId, accountId, period);
        int pageCount = Math.max(1, (int) Math.ceil(total / (double) PAGE_SIZE));
        int page = clamp(parsePage(req.getParameter("page")), pageCount);

        req.setAttribute("rows", movements.findGlobal(search, scope, type, categoryId,
                accountId, period, (page - 1) * PAGE_SIZE, PAGE_SIZE));
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageCount", pageCount);

        req.setAttribute("types", movements.distinctTypes());
        req.setAttribute("categories", categoryDao.findAllActive());

        // Se devuelven para que el buscador y las píldoras se repinten con lo elegido.
        req.setAttribute("q", search);
        req.setAttribute("ambito", scope == null ? "" : scope);
        req.setAttribute("tipo", type == null ? "" : type);
        req.setAttribute("cat", categoryId);
        req.setAttribute("cuenta", accountId);
        req.setAttribute("period", period);

        req.getRequestDispatcher("/WEB-INF/jsp/admin/movimientos.jsp").forward(req, resp);
    }

    /**
     * Sólo los dos ámbitos reales filtran; cualquier otra cosa es "todos".
     *
     * Se acepta el alias corto CONC porque es lo que escriben los enlaces de la
     * Concentradora: una URL que puede acabar tecleada en una defensa se
     * agradece corta.
     */
    private String normalizeScope(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toUpperCase();
        if ("CONC".equals(value) || "CONCENTRADORA".equals(value)) return "CONCENTRADORA";
        if ("CUENTA".equals(value) || "CUENTAS".equals(value))     return "CUENTA";
        return null;
    }

    /**
     * TODOS es el filtro de fecha apagado, igual que en el portal: una base
     * recién sembrada no tiene movimientos de hoy, y abrir el historial en una
     * tabla vacía parece que está roto.
     */
    private String normalizePeriod(String raw) {
        if ("HOY".equals(raw) || "7D".equals(raw) || "30D".equals(raw)) return raw;
        return "TODOS";
    }

    /*
      `tipo` no se valida contra una lista blanca y no hace falta: viaja como
      parámetro enlazado, así que un tipo inventado devuelve cero filas, no un
      error ni una inyección. El desplegable se llena con distinctTypes().
     */
    private Long parseId(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Long.valueOf(raw.trim()); } catch (NumberFormatException e) { return null; }
    }

    private int parsePage(String raw) {
        if (raw == null || raw.isBlank()) return 1;
        try { return Integer.parseInt(raw.trim()); } catch (NumberFormatException e) { return 1; }
    }

    private int clamp(int page, int pageCount) {
        return Math.min(Math.max(page, 1), pageCount);
    }

    private String trimToNull(String raw) {
        return (raw == null || raw.isBlank()) ? null : raw.trim();
    }
}
