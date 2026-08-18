package mx.sgfte.core.concentrator.web;

import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.audit.AuditEvent;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import mx.sgfte.core.accounts.AccountDao;
import mx.sgfte.core.concentrator.AccountLookupDao;
import mx.sgfte.core.concentrator.ConcentratorDao;
import mx.sgfte.core.concentrator.ConcentratorService;
import mx.sgfte.core.concentrator.ConcentratorSummary;
import mx.sgfte.core.funding.FundingDepositDao;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * GET /admin/concentradora — the "Cuenta Concentradora" screen (Figma 2097:313).
 *
 * Sólo lectura desde V12. Tenía un POST que fondeaba con el monto que se
 * tecleara en un modal; ese camino desapareció junto con el modal, porque el
 * saldo ya no sube por una acción de esta pantalla sino cuando el banco reporta
 * un depósito en /api/banco/deposito.
 *
 * The screen is the ledger's front page: balance, the last movements that
 * produced it, the last reintegrations that came back in, and the month's
 * totals. It summarises; the whole ledger — both books, searchable — lives in
 * /admin/movimientos, which is where the two "ver todo" links go. Until V3
 * there was no ledger at all to show, which is why this was the last admin
 * screen still on the old skeleton.
 *
 * Protected by AuthFilter (/admin/*).
 */
@WebServlet("/admin/concentradora")
public class ConcentratorServlet extends HttpServlet {

    private final AuditLogService audit = new AuditLogService();

    private final ConcentratorService service = new ConcentratorService();
    private final ConcentratorDao ledger = new ConcentratorDao();
    private final AccountLookupDao accountLookupDao = new AccountLookupDao();
    private final AccountDao accountDao = new AccountDao();
    private final FundingDepositDao deposits = new FundingDepositDao();

    /*
      Cuántas filas caben en cada panel del marco: 5 y 2, y ya no hay una
      segunda cifra. "Ver historial completo" era este mismo panel con otro
      tope (?ledger=all) porque no existía dónde mandar a quien quisiera más;
      ahora existe /admin/movimientos, que además busca, filtra y pagina.
     */
    private static final int MOVEMENT_ROWS = 5;
    private static final int REINTEGRATION_ROWS = 2;
    /** Caben tres tarjetas de depósito sin que el panel empuje al resumen. */
    private static final int DEPOSIT_ROWS = 3;

    private static final DateTimeFormatter DAY_YEAR =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("es-MX"));

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setAttribute("concentrator", service.getConcentrator());

        req.setAttribute("movements", ledger.findRecent(MOVEMENT_ROWS));
        req.setAttribute("reintegrations",
                ledger.findRecentByType("REINTEGRATION", REINTEGRATION_ROWS));

        ConcentratorSummary summary = ledger.summary();
        req.setAttribute("summary", summary);
        req.setAttribute("lastReintegrationLabel", summary.lastReintegration() == null
                ? null : summary.lastReintegration().format(DAY_YEAR));

        // "Cuentas activas" pertenece al módulo de cuentas, no al ledger.
        req.setAttribute("activeAccounts", accountDao.countForAdmin(null, "ACTIVE", null));

        // De dónde vino el dinero: el respaldo bancario de los últimos fondeos (V12).
        req.setAttribute("deposits", deposits.findRecent(DEPOSIT_ROWS));

        consumeFlash(req);

            /*
      Cuando el intento anterior falló, el modal se reabre con lo que el admin
      había tecleado. Antes la cuenta elegida se recuperaba sola, porque estaban
      TODAS en el <select> y bastaba con marcar la suya. Ahora el desplegable no
      existe: hay que traer su etiqueta, y sólo la suya.
    */
        Object retryId = req.getAttribute(DispersionServlet.FLASH_ACCOUNT);
        if (retryId != null && !retryId.toString().isBlank()) {
            try {
                accountLookupDao.findLabel(Long.parseLong(retryId.toString()))
                        .ifPresent(l -> req.setAttribute("dispersionAccountLabel", l));
            } catch (NumberFormatException ignored) {
                // Un id ilegible sólo significa que el campo se reabre vacío.
            }
        }

        req.getRequestDispatcher("/WEB-INF/jsp/admin/concentradora.jsp").forward(req, resp);
    }

    /**
     * Pasa el resultado de dispersar de la sesión a la petición.
     *
     * Se lee una vez y se borra, así un refresco no repite el mensaje. Si el
     * intento falló, la lista de errores es lo que le dice al JSP que reabra el
     * modal con lo que ya se había tecleado.
     */
    private void consumeFlash(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return;

        String[] keys = {
                DispersionServlet.FLASH_SUCCESS,
                DispersionServlet.FLASH_ERRORS,
                DispersionServlet.FLASH_ACCOUNT,
                DispersionServlet.FLASH_AMOUNT,
        };
        for (String key : keys) {
            Object value = session.getAttribute(key);
            if (value != null) {
                req.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

}
