package mx.sgfte.core.analytics;

import mx.sgfte.core.concentrator.ConcentratorDao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * El cálculo de la pantalla de Analíticas, en un solo sitio.
 *
 * Vivía dentro de DashboardServlet, que además de leer parámetros y elegir
 * vista decidía qué es una variación porcentual y cómo se reparte un total.
 * Con dos consumidores —la pantalla y la exportación— eso ya no se sostiene:
 * dos copias de once fórmulas terminan discrepando, y el día que discrepen el
 * CSV y la pantalla darán cifras distintas sin que nadie sepa cuál es la buena.
 *
 * Es un Service de sólo lectura: no abre transacción porque no mueve dinero. La
 * regla de oro del proyecto —el dinero se mueve dentro de un Service, dentro de
 * una transacción— habla de escrituras; aquí no hay ninguna.
 */
public class AnalyticsReportService {

    private static final int TOP_SPENDERS = 5;

    private final DashboardDao dashboardDao;
    private final AnalyticsDao analyticsDao;
    private final ConcentratorDao concentratorDao;

    public AnalyticsReportService() {
        this(new DashboardDao(), new AnalyticsDao(), new ConcentratorDao());
    }

    /** Constructor con DAOs inyectados: es el que permite probarlo con dobles. */
    public AnalyticsReportService(DashboardDao dashboardDao, AnalyticsDao analyticsDao,
                                  ConcentratorDao concentratorDao) {
        this.dashboardDao = dashboardDao;
        this.analyticsDao = analyticsDao;
        this.concentratorDao = concentratorDao;
    }

    /**
     * El reporte completo de una ventana.
     *
     * `now` entra como parámetro y no se toma de LocalDateTime.now() aquí
     * dentro: así todas las cifras del reporte comparten el mismo instante —de
     * otro modo, un KPI podría medirse un milisegundo después que el anterior y
     * caer del otro lado del corte— y además la clase se puede probar con una
     * fecha fija.
     */
    public AnalyticsReport build(AnalyticsPeriod period, LocalDateTime now) {
        LocalDateTime from = period.start(now);
        LocalDateTime prevFrom = period.previousStart(now);

        AnalyticsReport r = new AnalyticsReport();
        r.setPeriod(period);
        r.setFrom(from);
        r.setTo(now);

        BigDecimal balance = dashboardDao.concentratorBalance();
        r.setConcentratorBalance(balance);
        r.setConcentratorDelta(percentChange(concentratorDao.balanceAsOf(from), balance));

        BigDecimal dispersed = analyticsDao.dispersedBetween(from, now);
        r.setDispersed(dispersed);
        r.setDispersedDelta(percentChange(analyticsDao.dispersedBetween(prevFrom, from), dispersed));

        r.setActiveCardholders(dashboardDao.activeCardholders());
        r.setNewCardholders(analyticsDao.newCardholdersBetween(from, now));

        Map<String, Integer> byType = analyticsDao.activeCardsByType();
        r.setActiveCards(byType.values().stream().mapToInt(Integer::intValue).sum());
        r.setPhysicalCards(byType.getOrDefault("PHYSICAL", 0));
        r.setDigitalCards(byType.getOrDefault("DIGITAL", 0));

        r.setDispersionBars(dispersionBars(period, from, now));
        r.setPurposes(shares(analyticsDao.dispersionByPurpose(from, now)));

        int[] perDay = analyticsDao.movementsByWeekday(from, now);
        r.setWeekdayBars(weekdayBars(perDay));
        r.setWeekTotal(Arrays.stream(perDay).sum());

        r.setTopSpenders(topSpenders(from, now));

        r.setTransfersTotal(analyticsDao.transfersTotal(from, now));
        r.setTransfersCount(analyticsDao.transfersCount(from, now));
        r.setTransferPurposes(shares(analyticsDao.transfersByPurpose(from, now)));

        return r;
    }

    // ---- Movidos desde DashboardServlet, sin cambios --------------------------

    /**
     * Variación porcentual entre dos cifras.
     *
     * Devuelve null —que la pantalla dibuja como raya— cuando no hay base o la
     * base era cero. Informar "+100%" contra la nada, o dividir entre cero, son
     * los dos peores que admitir que todavía no hay con qué comparar.
     */
    private BigDecimal percentChange(BigDecimal before, BigDecimal after) {
        if (before == null || before.signum() == 0 || after == null) return null;
        return after.subtract(before)
                .multiply(BigDecimal.valueOf(100))
                .divide(before, 1, RoundingMode.HALF_UP);
    }

    private List<ChartBar> dispersionBars(AnalyticsPeriod period,
                                          LocalDateTime from, LocalDateTime now) {
        int buckets = period.buckets();
        BigDecimal[] totals =
                analyticsDao.dispersionBuckets(from, now, buckets, period.bucketsAreMonths());

        BigDecimal max = BigDecimal.ZERO;
        for (BigDecimal t : totals) if (t.compareTo(max) > 0) max = t;

        List<ChartBar> bars = new ArrayList<>();
        for (int i = 0; i < buckets; i++) {
            bars.add(new ChartBar(period.bucketLabel(i, now.toLocalDate()),
                    totals[i], scale(totals[i], max), i == buckets - 1));
        }
        return bars;
    }

    /** Barras de lunes a domingo; el día más movido es el que el marco pinta vivo. */
    private List<ChartBar> weekdayBars(int[] perDay) {
        String[] labels = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

        int max = 0;
        for (int n : perDay) max = Math.max(max, n);

        int busiest = -1;
        for (int i = 0; i < perDay.length && max > 0; i++) {
            if (perDay[i] == max) { busiest = i; break; }
        }

        List<ChartBar> bars = new ArrayList<>();
        for (int i = 0; i < perDay.length; i++) {
            int percent = max == 0 ? 0 : perDay[i] * 100 / max;
            bars.add(new ChartBar(labels[i], BigDecimal.valueOf(perDay[i]), percent, i == busiest));
        }
        return bars;
    }

    private List<TopSpender> topSpenders(LocalDateTime from, LocalDateTime now) {
        List<Object[]> rows = analyticsDao.topSpenders(from, now, TOP_SPENDERS);

        BigDecimal top = rows.isEmpty() ? BigDecimal.ZERO : (BigDecimal) rows.get(0)[1];
        List<TopSpender> out = new ArrayList<>();
        for (Object[] r : rows) {
            BigDecimal amount = (BigDecimal) r[1];
            out.add(new TopSpender((String) r[0], amount, scale(amount, top), (Integer) r[2]));
        }
        return out;
    }

    /** Convierte totales por propósito en participaciones sobre el total. */
    private List<PurposeShare> shares(Map<String, BigDecimal> byPurpose) {
        BigDecimal total = byPurpose.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PurposeShare> shares = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, BigDecimal> e : byPurpose.entrySet()) {
            int percent = total.signum() == 0 ? 0
                    : e.getValue().multiply(BigDecimal.valueOf(100))
                    .divide(total, 0, RoundingMode.HALF_UP).intValue();
            shares.add(new PurposeShare(e.getKey(), e.getValue(), percent, (i % 4) + 1));
            i++;
        }
        return shares;
    }

    /** value como porción 0..100 de max, cuidando el caso vacío. */
    private int scale(BigDecimal value, BigDecimal max) {
        if (max == null || max.signum() == 0) return 0;
        return value.multiply(BigDecimal.valueOf(100))
                .divide(max, 0, RoundingMode.HALF_UP).intValue();
    }
}
