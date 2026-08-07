package mx.sgfte.core.analytics;

import java.math.BigDecimal;

/**
 * One slice of the "Distribución de gasto" panel: a purpose, how much money sits
 * in its accounts, and what share of the total that is.
 *
 * The percentage is computed once in Java rather than in the JSP — EL has no
 * decent way to divide BigDecimals, and doing arithmetic in a view is how
 * rounding bugs get hidden.
 */
public class PurposeShare {

    private final String purpose;
    private final BigDecimal amount;
    private final int percent;   // 0..100, rounded
    private final int colorIndex; // 1..4, picks the badge/bar colour in CSS

    public PurposeShare(String purpose, BigDecimal amount, int percent, int colorIndex) {
        this.purpose = purpose;
        this.amount = amount;
        this.percent = percent;
        this.colorIndex = colorIndex;
    }

    public String getPurpose() { return purpose; }
    public BigDecimal getAmount() { return amount; }
    public int getPercent() { return percent; }
    public int getColorIndex() { return colorIndex; }
}
