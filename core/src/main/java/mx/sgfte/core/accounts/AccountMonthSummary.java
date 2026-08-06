package mx.sgfte.core.accounts;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * "Resumen del mes" panel of the account detail screen.
 *
 * The frame also lists a "Límite mensual". There is no such column anywhere in
 * the schema — no per-account or per-category cap exists — so it is not modelled
 * here and the view renders a dash for it. Adding it later means a real
 * migration plus a rule about who sets it.
 */
public class AccountMonthSummary {

    private final BigDecimal dispersed;      // depósitos del mes corriente
    private final int movements;             // movimientos del mes corriente
    private final LocalDateTime lastDeposit; // nullable: puede no haber recargas
    private final int activeCards;

    public AccountMonthSummary(BigDecimal dispersed, int movements,
                               LocalDateTime lastDeposit, int activeCards) {
        this.dispersed = dispersed;
        this.movements = movements;
        this.lastDeposit = lastDeposit;
        this.activeCards = activeCards;
    }

    public BigDecimal getDispersed() { return dispersed; }
    public int getMovements() { return movements; }
    public LocalDateTime getLastDeposit() { return lastDeposit; }
    public int getActiveCards() { return activeCards; }
}
