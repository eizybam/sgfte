package mx.sgfte.core.concentrator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The "RESUMEN CONCENTRADORA" panel, read in one pass over the ledger.
 *
 * "Cuentas activas" is not in here: it belongs to the accounts module and the
 * servlet asks AccountDao for it, rather than teaching this query to join a
 * table it has no business knowing about.
 */
public record ConcentratorSummary(
        BigDecimal dispersedThisMonth,
        int movementsThisMonth,
        LocalDateTime lastReintegration,
        BigDecimal reintegratedThisMonth) {
}
