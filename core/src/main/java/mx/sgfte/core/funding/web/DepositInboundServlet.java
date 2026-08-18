package mx.sgfte.core.funding.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.concentrator.ConcentratorDao;
import mx.sgfte.core.funding.DuplicateDepositException;
import mx.sgfte.core.funding.FundingConfig;
import mx.sgfte.core.funding.FundingDeposit;
import mx.sgfte.core.funding.FundingService;
import mx.sgfte.core.users.ValidationException;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * POST /api/banco/deposito — el banco avisa que llegó dinero.
 *
 * Éste es el único camino por el que sube el saldo de la Concentradora. No hay
 * pantalla de "fondear" con un campo de monto, porque un fondeo no es algo que
 * un administrador haga: es algo que le pasa a la cuenta y que el sistema
 * registra.
 *
 * Es máquina a máquina, así que no pasa por AuthFilter —no hay sesión que
 * comprobar— y lo protege la firma HMAC. Es el mismo trato que da un webhook de
 * un PSP real (STP, Openpay): cuerpo firmado con un secreto compartido.
 *
 * Recibe application/x-www-form-urlencoded y no JSON a propósito: el proyecto
 * no tiene librería de JSON y añadir una para catorce campos planos no se paga.
 *
 * Los códigos importan porque el que llama es un sistema y decide qué hacer con
 * ellos:
 *   200 aplicado · 400 el depósito no cuadra · 401 firma inválida
 *   409 esa referencia ya estaba registrada (reintentar no arregla nada)
 *
 * La auditoría se escribe DESPUÉS de que la transacción cerró, igual que en el
 * resto del sistema: primero el hecho, luego el rastro.
 */
@WebServlet("/api/banco/deposito")
public class DepositInboundServlet extends HttpServlet {

    private final FundingService funding = new FundingService();
    private final ConcentratorDao concentrator = new ConcentratorDao();
    private final AuditLogService audit = new AuditLogService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");

        if (!DepositSignature.verify(req, FundingConfig.BANK_SECRET)) {
            /*
              Alguien llamó al endpoint sin conocer el secreto. No es un error
              de captura: es un intento de meter dinero desde fuera, así que va
              como CRIT y se guarda la referencia que traía para poder buscarla.
             */
            audit.record(AuditEvent.DEPOSIT_BAD_SIGNATURE,
                    "ref: " + shorten(req.getParameter("referencia")), req);
            fail(resp, HttpServletResponse.SC_UNAUTHORIZED, "Firma inválida");
            return;
        }

        FundingDeposit deposit;
        try {
            deposit = read(req);
        } catch (IllegalArgumentException e) {
            audit.record(AuditEvent.DEPOSIT_REJECTED, e.getMessage(), req);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        try {
            FundingDeposit applied =
                    funding.register(deposit, concentrator.findSingleton().getClabe());

            audit.record(AuditEvent.DEPOSIT_RECEIVED,
                    applied.getCanal() + " · " + applied.getReferencia()
                            + " · $" + applied.getMonto()
                            + " · " + applied.getOrdenanteNombre(), req);

            resp.setStatus(HttpServletResponse.SC_OK);
            write(resp, "{\"estado\":\"aplicado\",\"deposito\":" + applied.getId()
                    + ",\"referencia\":\"" + escape(applied.getReferencia()) + "\"}");

        } catch (DuplicateDepositException e) {
            audit.record(AuditEvent.DEPOSIT_REJECTED,
                    "Referencia repetida: " + e.getReferencia(), req);
            fail(resp, HttpServletResponse.SC_CONFLICT, e.getMessage());

        } catch (ValidationException e) {
            audit.record(AuditEvent.DEPOSIT_REJECTED, String.join(" · ", e.getErrors()), req);
            fail(resp, HttpServletResponse.SC_BAD_REQUEST, String.join(" · ", e.getErrors()));
        }
    }

    /**
     * Arma el depósito con lo que llegó.
     *
     * Sólo se ocupa de lo que no puede juzgar el servicio: que el monto y la
     * fecha tengan forma de monto y de fecha. Todo lo demás —qué exige cada
     * canal, si la CLABE existe, si el destino somos nosotros— es regla de
     * negocio y vive en FundingService.
     */
    private FundingDeposit read(HttpServletRequest req) {
        FundingDeposit d = new FundingDeposit();
        d.setCanal(upper(req.getParameter("canal")));
        d.setReferencia(trim(req.getParameter("referencia")));
        d.setOrdenanteNombre(trim(req.getParameter("ordenanteNombre")));
        d.setOrdenanteRfc(upper(req.getParameter("ordenanteRfc")));
        d.setOrdenanteClabe(trim(req.getParameter("ordenanteClabe")));
        d.setInstitucion(trim(req.getParameter("institucion")));
        d.setSucursal(trim(req.getParameter("sucursal")));
        d.setBeneficiarioClabe(trim(req.getParameter("beneficiarioClabe")));
        d.setConcepto(trim(req.getParameter("concepto")));

        String monto = trim(req.getParameter("monto"));
        if (monto == null) throw new IllegalArgumentException("Falta el monto del depósito");
        try {
            d.setMonto(new BigDecimal(monto));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El monto no es una cantidad: " + monto);
        }

        String numerica = trim(req.getParameter("referenciaNumerica"));
        if (numerica != null) {
            try {
                d.setReferenciaNumerica(Integer.valueOf(numerica));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("La referencia numérica no es un número");
            }
        }

        // ISO-8601 sin zona: es lo que manda el simulador y lo que produce un
        // <input type="datetime-local">. Si no viene, es ahora.
        String fecha = trim(req.getParameter("fechaOperacion"));
        if (fecha == null) {
            d.setFechaOperacion(LocalDateTime.now());
        } else {
            try {
                d.setFechaOperacion(LocalDateTime.parse(fecha));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("La fecha de operación no es válida: " + fecha);
            }
        }
        return d;
    }

    private void fail(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        write(resp, "{\"estado\":\"rechazado\",\"motivo\":\"" + escape(message) + "\"}");
    }

    private void write(HttpServletResponse resp, String body) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        try (PrintWriter out = resp.getWriter()) {
            out.print(body);
        }
    }

    /** Sin librería de JSON, la respuesta se arma a mano y hay que escaparla. */
    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", " ").replace("\r", " ");
    }

    /** Para no volcar en la bitácora lo que sea que haya mandado un desconocido. */
    private String shorten(String value) {
        if (value == null || value.isBlank()) return "(sin referencia)";
        String trimmed = value.trim();
        return trimmed.length() <= 40 ? trimmed : trimmed.substring(0, 40) + "…";
    }

    private String trim(String value) {
        if (value == null) return null;
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }

    private String upper(String value) {
        String t = trim(value);
        return t == null ? null : t.toUpperCase(java.util.Locale.ROOT);
    }

    /** Sólo POST: un depósito no se consulta por GET. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Allow", "POST");
        fail(resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Este endpoint sólo acepta POST");
    }

    /** Expuesto para el simulador, que necesita nombrar los mismos campos. */
    public static final List<String> FIELDS = List.of(
            "canal", "referencia", "ordenanteNombre", "ordenanteRfc", "ordenanteClabe",
            "institucion", "sucursal", "beneficiarioClabe", "monto", "concepto",
            "referenciaNumerica", "fechaOperacion");
}
