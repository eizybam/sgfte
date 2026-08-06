package mx.sgfte.core.analytics;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The window the Analíticas screen is looking at — the Hoy / 7 días / 30 días /
 * 12 meses selector.
 *
 * It scopes the WHOLE page, so every panel and the "vs periodo anterior"
 * comparison are measured against the same window. The previous window is the
 * same length immediately before this one, which is what makes the deltas
 * comparable rather than arbitrary.
 */
public enum AnalyticsPeriod {

    TODAY("hoy", "Hoy", "HOY"),
    D7("7d", "7 días", "7 DÍAS"),
    D30("30d", "30 días", "30 DÍAS"),
    M12("12m", "12 meses", "12 MESES");

    private final String code;    // el que viaja en la URL
    private final String label;   // el del selector
    private final String caps;    // para los rótulos monoespaciados de los KPI

    AnalyticsPeriod(String code, String label, String caps) {
        this.code = code;
        this.label = label;
        this.caps = caps;
    }

    public String getCode()  { return code; }
    public String getLabel() { return label; }
    public String getCaps()  { return caps; }

    /** Unknown or missing code falls back to the frame's default, 30 días. */
    public static AnalyticsPeriod of(String code) {
        if (code != null) {
            for (AnalyticsPeriod p : values()) {
                if (p.code.equalsIgnoreCase(code.trim())) return p;
            }
        }
        return D30;
    }

    /** Start of this window. "Hoy" means since midnight, not the last 24 hours. */
    public LocalDateTime start(LocalDateTime now) {
        return switch (this) {
            case TODAY -> now.toLocalDate().atStartOfDay();
            case D7    -> now.minusDays(7);
            case D30   -> now.minusDays(30);
            case M12   -> now.minusMonths(12);
        };
    }

    /** Start of the window immediately before this one, of the same length. */
    public LocalDateTime previousStart(LocalDateTime now) {
        return switch (this) {
            case TODAY -> now.toLocalDate().minusDays(1).atStartOfDay();
            case D7    -> now.minusDays(14);
            case D30   -> now.minusDays(60);
            case M12   -> now.minusMonths(24);
        };
    }

    /**
     * How many buckets the "Dispersión" chart draws and how wide each one is.
     * The frame shows twelve; shorter windows split into days so the shape still
     * says something instead of collapsing into one bar.
     */
    public int buckets() { return 12; }

    /** True when buckets are months rather than days. */
    public boolean bucketsAreMonths() { return this == M12; }

    /** Label for a bucket index (0 = oldest). */
    public String bucketLabel(int index, LocalDate today) {
        if (bucketsAreMonths()) {
            LocalDate month = today.minusMonths(buckets() - 1L - index);
            String name = month.getMonth()
                    .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("es-MX"));
            name = name.replace(".", "");
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        LocalDate day = today.minusDays(dayStep() * (buckets() - 1L - index));
        return String.valueOf(day.getDayOfMonth());
    }

    /** Days covered by one bucket when the window is measured in days. */
    public int dayStep() {
        return switch (this) {
            case TODAY -> 1;   // no se usa: hoy cabe en un solo cubo
            case D7    -> 1;
            case D30   -> 3;   // 12 cubos × 3 días ≈ 30
            case M12   -> 0;
        };
    }
}
