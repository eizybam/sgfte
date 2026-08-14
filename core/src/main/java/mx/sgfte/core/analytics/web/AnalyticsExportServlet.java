package mx.sgfte.core.analytics.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.analytics.AnalyticsPeriod;
import mx.sgfte.core.analytics.AnalyticsReport;
import mx.sgfte.core.analytics.AnalyticsReportService;
import mx.sgfte.core.analytics.ChartBar;
import mx.sgfte.core.analytics.PurposeShare;
import mx.sgfte.core.analytics.TopSpender;
import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;
import mx.sgfte.core.shared.web.Csv;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * GET /admin/analytics.csv — el reporte de Analíticas como archivo.
 *
 * Acepta el mismo ?period= que la pantalla y lee del mismo
 * AnalyticsReportService, así que el archivo y lo que se ve en el monitor no
 * pueden discrepar: son el mismo cálculo, formateado dos veces.
 *
 * Un CSV por secciones y no once archivos: la pantalla se lee como un informe
 * y el archivo también debe leerse así.
 *
 * Sólo lectura. Protegido por AuthFilter (/admin/*).
 */
@WebServlet("/admin/analytics.csv")
public class AnalyticsExportServlet extends HttpServlet {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final AnalyticsReportService reports = new AnalyticsReportService();
    private final AuditLogService audit = new AuditLogService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        AnalyticsPeriod period = AnalyticsPeriod.of(req.getParameter("period"));
        LocalDateTime now = LocalDateTime.now();
        AnalyticsReport r = reports.build(period, now);

        // 1) Armar el archivo entero ANTES de tocar la respuesta.
        Csv csv = new Csv();
        header(csv, r, req);
        indicators(csv, r);
        dispersionOverTime(csv, r);
        purposeSection(csv, "DISPERSIÓN POR PROPÓSITO", "Monto dispersado (MXN)", r.getPurposes());
        transfers(csv, r);
        weekdays(csv, r);
        topSpenders(csv, r);

        // 2) Y sólo entonces escribirlo.
        String fileName = "sgfte-analiticas-" + period.getCode()
                + "-" + now.format(FILE_STAMP) + ".csv";
        resp.setContentType("text/csv");
        resp.setCharacterEncoding("UTF-8");   // antes de getWriter(), o no aplica
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        resp.setHeader("Cache-Control", "no-store");

        try (PrintWriter out = resp.getWriter()) {
            out.print('\uFEFF');   // BOM: sin él, Excel en Windows rompe los acentos
            out.print(csv);
        }

        // 3) Registrar la exportación ya consumada.
        audit.record(AuditEvent.REPORT_EXPORTED, "Periodo " + period.getLabel(), req);
    }

    /** Portada: sin esto, dentro de un mes nadie sabe de cuándo es el archivo. */
    private void header(Csv csv, AnalyticsReport r, HttpServletRequest req) {
        csv.row("SGFTE · Reporte de analíticas");
        csv.row("Periodo", r.getPeriod().getLabel());
        csv.row("Desde", r.getFrom().format(STAMP));
        csv.row("Hasta", r.getTo().format(STAMP));
        csv.row("Generado por", AuditLogService.actorOf(req));
        csv.blank();
    }

    /**
     * Los KPI de la fila superior.
     *
     * Los montos van crudos, sin "$" ni separadores de miles: en una hoja de
     * cálculo, un número con formato deja de ser un número y no se puede sumar.
     * Quien reciba el archivo le pone formato de moneda en dos clics; deshacer
     * un "$1,458,200.00" convertido en texto cuesta mucho más.
     *
     * La celda de variación queda vacía cuando no hay base contra la que
     * comparar — el equivalente de la raya que dibuja la pantalla.
     */
    private void indicators(Csv csv, AnalyticsReport r) {
        csv.row("INDICADORES");
        csv.row("Indicador", "Valor", "Variación vs periodo anterior (%)");
        csv.row("Saldo de la concentradora (MXN)", r.getConcentratorBalance(), r.getConcentratorDelta());
        csv.row("Dispersado en el periodo (MXN)", r.getDispersed(), r.getDispersedDelta());
        csv.row("Tarjetahabientes activos", r.getActiveCardholders(), null);
        csv.row("Nuevos tarjetahabientes en el periodo", r.getNewCardholders(), null);
        csv.row("Tarjetas activas", r.getActiveCards(), null);
        csv.row("Tarjetas físicas", r.getPhysicalCards(), null);
        csv.row("Tarjetas digitales", r.getDigitalCards(), null);
        csv.row("Transferencias entre cuentas (MXN)", r.getTransfersTotal(), null);
        csv.row("Número de transferencias", r.getTransfersCount(), null);
        csv.blank();
    }

    /** La gráfica de barras, cubo por cubo. Es el dato, no el dibujo. */
    private void dispersionOverTime(Csv csv, AnalyticsReport r) {
        csv.row("DISPERSIÓN EN EL TIEMPO");
        csv.row(r.getPeriod().bucketsAreMonths() ? "Mes" : "Día", "Monto dispersado (MXN)");
        for (ChartBar bar : r.getDispersionBars()) {
            csv.row(bar.getLabel(), bar.getValue());
        }
        csv.blank();
    }

    /** Los dos repartos por propósito comparten forma, así que comparten método. */
    private void purposeSection(Csv csv, String title, String amountHeader,
                                List<PurposeShare> shares) {
        csv.row(title);
        csv.row("Propósito", amountHeader, "% del total");
        if (shares.isEmpty()) {
            csv.row("(sin movimientos en el periodo)");
        }
        for (PurposeShare s : shares) {
            csv.row(s.getPurpose(), s.getAmount(), s.getPercent());
        }
        csv.blank();
    }

    private void transfers(Csv csv, AnalyticsReport r) {
        purposeSection(csv, "TRANSFERENCIAS POR PROPÓSITO",
                "Monto transferido (MXN)", r.getTransferPurposes());
    }

    private void weekdays(Csv csv, AnalyticsReport r) {
        csv.row("ACTIVIDAD POR DÍA DE LA SEMANA");
        csv.row("Día", "Movimientos");
        for (ChartBar bar : r.getWeekdayBars()) {
            csv.row(bar.getLabel(), bar.getValue());
        }
        csv.row("Total", r.getWeekTotal());
        csv.blank();
    }

    private void topSpenders(Csv csv, AnalyticsReport r) {
        csv.row("TOP TARJETAHABIENTES POR MONTO RECIBIDO");
        csv.row("Posición", "Tarjetahabiente", "Monto (MXN)", "% del primero");
        int position = 1;
        for (TopSpender t : r.getTopSpenders()) {
            csv.row(position++, t.getHolderName(), t.getAmount(), t.getPercent());
        }
        if (r.getTopSpenders().isEmpty()) {
            csv.row("(sin dispersiones en el periodo)");
        }
    }
}
