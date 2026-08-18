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
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * GET  /admin/concentradora — the "Cuenta Concentradora" screen (Figma 2097:313).
 * POST /admin/concentradora — adds funds to the Concentrator.
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

    /** Leídos por esta pantalla y por AdminHomeServlet para reabrir el modal si falló. */
    public static final String FLASH_ERRORS = "fundErrors";
    public static final String FLASH_AMOUNT = "fundAmount";

    private final ConcentratorService service = new ConcentratorService();
    private final ConcentratorDao ledger = new ConcentratorDao();
    private final AccountLookupDao accountLookupDao = new AccountLookupDao();
    private final AccountDao accountDao = new AccountDao();

    /*
      Cuántas filas caben en cada panel del marco: 5 y 2, y ya no hay una
      segunda cifra. "Ver historial completo" era este mismo panel con otro
      tope (?ledger=all) porque no existía dónde mandar a quien quisiera más;
      ahora existe /admin/movimientos, que además busca, filtra y pagina.
     */
    private static final int MOVEMENT_ROWS = 5;
    private static final int REINTEGRATION_ROWS = 2;

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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BigDecimal amount = parseAmount(req.getParameter("amount"));
        String method = req.getParameter("method");

        HttpSession session = req.getSession();
        try {
            // El actor del ledger es el correo, igual que en la bitácora.
            service.fund(amount, AuditLogService.actorOf(req));

            // El método de fondeo va en la bitácora, no en el ledger: es contexto
            // operativo, no un hecho financiero. El ledger guarda el importe y el
            // saldo resultante, que es lo que tiene que cuadrar.
            audit.record(AuditEvent.CONCENTRATOR_FUNDED,
                    (method == null || method.isBlank() ? "" : method + " · ") + "$" + amount, req);

            mx.sgfte.core.shared.web.OperationResult.success("¡Fondeo aplicado!",
                            "La Concentradora recibió los fondos",
                            "FONDEO CONFIRMADO",
                            "El saldo ya está disponible para dispersar.")
                    .amount("Monto fondeado", amount)
                    .detail("Método", method)
                    .detail("Destino", "Cuenta Concentradora")
                    .when(java.time.LocalDateTime.now())
                    .primary("Ver concentradora", "/admin/concentradora")
                    .flash(session);
        } catch (ValidationException e) {
            session.setAttribute(FLASH_ERRORS, e.getErrors());
            session.setAttribute(FLASH_AMOUNT, req.getParameter("amount"));
        }
        // Redirect y no forward: fondear mueve dinero, refrescar no debe repetirlo.
        resp.sendRedirect(req.getContextPath() + backTo(req));
    }

    /**
     * Dos pantallas abren el modal de fondeo. Se acepta sólo el literal
     * "concentradora", nunca una URL, para que no sea una redirección abierta.
     */
    private String backTo(HttpServletRequest req) {
        return "concentradora".equals(req.getParameter("returnTo"))
                ? "/admin/concentradora" : "/admin/home";
    }

    /**
     * Pasa el resultado de fondear o dispersar de la sesión a la petición.
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
                FLASH_ERRORS,
                FLASH_AMOUNT,
        };
        for (String key : keys) {
            Object value = session.getAttribute(key);
            if (value != null) {
                req.setAttribute(key, value);
                session.removeAttribute(key);
            }
        }
    }

    /** Parses a money string into BigDecimal, or null if empty/invalid. */
    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
