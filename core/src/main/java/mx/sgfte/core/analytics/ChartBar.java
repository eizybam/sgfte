package mx.sgfte.core.analytics;

import java.math.BigDecimal;

/**
 * One bar of the two charts on Analíticas.
 *
 * `percent` is the height relative to the tallest bar, worked out once here so
 * the JSP never has to divide — the same reason PurposeShare carries its own
 * percentage on the dashboard.
 */
public class ChartBar {

    private final String label;
    private final BigDecimal value;
    private final int percent;     // 0..100, alto relativo a la barra más alta
    private final boolean latest;  // la última: el marco la pinta más viva

    public ChartBar(String label, BigDecimal value, int percent, boolean latest) {
        this.label = label;
        this.value = value;
        this.percent = percent;
        this.latest = latest;
    }

    public String getLabel() { return label; }
    public BigDecimal getValue() { return value; }
    public int getPercent() { return percent; }
    public boolean isLatest() { return latest; }
}
