package mx.sgfte.core.movements.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.movements.GlobalMovementRow;
import mx.sgfte.core.movements.MovementQueryDao;
import mx.sgfte.core.shared.web.Csv;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * GET /admin/movimientos.csv — el ledger completo como archivo.
 *
 * Mismos filtros que la pantalla y sin paginar: lo que se descarga es "lo que
 * estoy viendo", no los diez renglones de esta página. Nadie quiere cuarenta
 * archivos de diez filas.
 *
 * <p>Los filtros los lee {@link MovementFilters}, el mismo objeto que usa la
 * pantalla. No es una preferencia de estilo: en la exportación de la bitácora
 * el CSV lee nombres de parámetro distintos a los que manda el enlace, así que
 * el archivo sale sin filtrar y no avisa. Compartiendo el lector, ese fallo no
 * se puede escribir aquí.
 *
 * <p>Sólo lectura, pero SÍ se audita. Un CSV con el rastro completo del dinero
 * saliendo del sistema es exactamente el hecho que una bitácora existe para
 * conservar — quién se llevó una copia y con qué filtro.
 *
 * Protegido por AuthFilter (/admin/*).
 */
@WebServlet("/admin/movimientos.csv")
public class MovementsExportServlet extends HttpServlet {

    /**
     * El mismo tope que la exportación de la bitácora.
     *
     * Cuando el filtro devuelve más, el archivo se corta — y lo DICE en su
     * portada. Un CSV recortado en silencio es peor que uno que no se descarga:
     * quien lo recibe suma una columna y saca una conclusión sobre datos que no
     * están completos.
     */
    private static final int MAX_ROWS = 4000;

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MovementQueryDao movements = new MovementQueryDao();
    private final AuditLogService audit = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        MovementFilters f = MovementFilters.from(req);
        LocalDateTime now = LocalDateTime.now();

        int total = movements.countGlobal(f.search(), f.scope(), f.type(),
                f.categoryId(), f.accountId(), f.period());
        List<GlobalMovementRow> rows = movements.findGlobal(f.search(), f.scope(), f.type(),
                f.categoryId(), f.accountId(), f.period(), 0, MAX_ROWS);

        // 1) Armar el archivo entero ANTES de tocar la respuesta.
        Csv csv = new Csv();
        header(csv, f, req, now, total, rows.size());
        table(csv, rows);

        // 2) Y sólo entonces escribirlo.
        String fileName = "sgfte-movimientos-" + now.format(FILE_STAMP) + ".csv";
        resp.setContentType("text/csv");
        resp.setCharacterEncoding("UTF-8");   // antes de getWriter(), o no aplica
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        resp.setHeader("Cache-Control", "no-store");

        try (PrintWriter out = resp.getWriter()) {
            out.print('﻿');   // BOM: sin él, Excel en Windows rompe los acentos
            out.print(csv);
        }

        // 3) Registrar la exportación ya consumada.
        audit.record(AuditEvent.MOVEMENTS_EXPORTED,
                rows.size() + " de " + total + " movimientos · " + describe(f), req);
    }

    /**
     * Portada: de cuándo es el archivo, quién lo sacó y con qué filtro.
     *
     * Sin esto, dentro de un mes nadie sabe si ese CSV es "todo" o "sólo la
     * Concentradora de los últimos 7 días", y las dos cosas se parecen mucho
     * una vez abiertas en una hoja de cálculo.
     */
    private void header(Csv csv, MovementFilters f, HttpServletRequest req,
                        LocalDateTime now, int total, int exported) {
        csv.row("SGFTE · Movimientos");
        csv.row("Generado", now.format(STAMP));
        csv.row("Generado por", AuditLogService.actorOf(req));
        csv.row("Filtro aplicado", describe(f));
        csv.row("Movimientos que cumplen el filtro", total);
        csv.row("Movimientos en este archivo", exported);
        if (total > exported) {
            csv.row("AVISO", "El archivo está recortado al tope de " + MAX_ROWS
                    + " filas. Afina el filtro para llevarte el resto.");
        }
        /*
          El aviso de la partida doble. Una dispersión deja asiento en los dos
          libros, así que sumar la columna de monto con el ámbito en TODOS
          cuenta cada peso dos veces. En la pantalla el problema no existe
          porque no se enseña ningún total; en una hoja de cálculo, sumar una
          columna es lo primero que hace cualquiera.
         */
        csv.row("NOTA",
                "El sistema lleva dos libros y una dispersión se registra en los dos. "
              + "Sumar MONTO con ÁMBITO en TODOS cuenta cada peso dos veces; "
              + "filtra por un ámbito antes de totalizar.");
        csv.blank();
    }

    private void table(Csv csv, List<GlobalMovementRow> rows) {
        csv.row("FECHA", "HORA", "ÁMBITO", "TIPO", "CONCEPTO",
                "CUENTA", "CONTRAPARTE", "TARJETA", "TITULAR", "CÓDIGO EMPLEADO", "PROPÓSITO",
                "CANAL", "REFERENCIA BANCARIA", "ORDENANTE",
                "DIRECCIÓN", "MONTO (MXN)");

        for (GlobalMovementRow m : rows) {
            LocalDateTime at = m.getCreatedAt();
            csv.row(
                    at == null ? null : at.format(DATE),
                    at == null ? null : at.format(TIME),
                    m.getScope(),
                    m.getKind(),
                    // La descripción cruda, no getConcept(): en la pantalla ése
                    // cae a la referencia para no dejar la celda vacía, pero
                    // aquí la referencia tiene columna propia y repetirla sería
                    // llenar dos columnas con el mismo dato.
                    m.getDescription(),
                    m.getAccountLabel(),
                    m.getRelatedNumber(),
                    // Columna propia y no pegada a CUENTA como en la pantalla:
                    // allí comparten renglón por falta de sitio, aquí el sitio
                    // sobra y un dato por columna se puede filtrar y ordenar.
                    m.getCardLabel(),
                    m.getWhoLabel(),
                    m.getEmployeeCode(),
                    m.getCategoryName(),
                    m.getCanal(),
                    m.getReferencia(),
                    m.getOrdenante(),
                    m.isInflow() ? "ENTRADA" : "SALIDA",
                    signed(m));
        }

        if (rows.isEmpty()) {
            csv.row("(ningún movimiento cumple el filtro)");
        }
    }

    /**
     * El monto con signo y crudo: sin "$" ni separadores de miles.
     *
     * Con formato deja de ser un número y no se puede sumar; quien reciba el
     * archivo le pone formato de moneda en dos clics, pero deshacer un
     * "-$1,458.20" convertido en texto cuesta mucho más. El signo lo lleva el
     * propio valor para que una columna filtrada por ámbito sume sola.
     */
    private BigDecimal signed(GlobalMovementRow m) {
        BigDecimal amount = m.getAmount();
        if (amount == null) return null;
        return m.isInflow() ? amount : amount.negate();
    }

    /** El filtro, en una línea que se entienda dentro de un mes. */
    private String describe(MovementFilters f) {
        if (!f.any()) return "Sin filtros (todo el ledger)";

        StringBuilder sb = new StringBuilder();
        append(sb, "búsqueda", f.search());
        append(sb, "ámbito", f.scope());
        append(sb, "tipo", f.type());
        append(sb, "propósito id", f.categoryId());
        append(sb, "cuenta id", f.accountId());
        if (!"TODOS".equals(f.period())) append(sb, "periodo", f.period());
        return sb.toString();
    }

    private void append(StringBuilder sb, String label, Object value) {
        if (value == null) return;
        if (sb.length() > 0) sb.append(" · ");
        sb.append(label).append(": ").append(value);
    }
}
