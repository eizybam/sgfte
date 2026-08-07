package mx.sgfte.core.analytics;

import java.math.BigDecimal;

/** One row of "Top cuentas por gasto": who, how much, and the bar's fill. */
public class TopSpender {

    private final String holderName;
    private final BigDecimal amount;
    private final int percent;      // 0..100, relativo al primero de la lista
    private final int purposeColor; // 1..4, mismo criterio que el resto

    public TopSpender(String holderName, BigDecimal amount, int percent, int purposeColor) {
        this.holderName = holderName;
        this.amount = amount;
        this.percent = percent;
        this.purposeColor = purposeColor;
    }

    public String getHolderName() { return holderName; }
    public BigDecimal getAmount() { return amount; }
    public int getPercent() { return percent; }
    public int getPurposeColor() { return purposeColor; }
}
