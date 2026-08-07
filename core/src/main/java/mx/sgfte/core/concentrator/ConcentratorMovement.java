package mx.sgfte.core.concentrator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * One line of the Concentrator ledger (V3).
 *
 * This is a financial record, not an event: it carries the typed amount and the
 * balance it left behind, so the ledger reconciles against the real balance.
 * That is what makes it different from the audit trail, which only says who did
 * what and when.
 */
public class ConcentratorMovement {

    private final long id;
    private final String movementType;   // FUNDING | DISPERSION | REINTEGRATION
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String actor;
    private final LocalDateTime createdAt;

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd MMM", ES_MX);
    private static final DateTimeFormatter DAY_YEAR = DateTimeFormatter.ofPattern("dd MMM yyyy", ES_MX);

    public ConcentratorMovement(long id, String movementType, BigDecimal amount,
                                BigDecimal balanceAfter, String actor, LocalDateTime createdAt) {
        this.id = id;
        this.movementType = movementType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.actor = actor;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getMovementType() { return movementType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getActor() { return actor; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Money coming IN to the Concentrator: funding and reintegrations. */
    public boolean isInflow() {
        return "FUNDING".equals(movementType) || "REINTEGRATION".equals(movementType);
    }

    /**
     * What the CONCEPTO column shows.
     *
     * The frame writes "Dispersión → Gasolina", but the ledger does not store
     * which account the money went to — it records the Concentrator's side of
     * the movement. The destination lives in account_movement, so the arrow is
     * left off rather than guessed at.
     */
    public String getConcept() {
        return switch (movementType) {
            case "FUNDING"       -> "Fondeo externo";
            case "DISPERSION"    -> "Dispersión a cuenta";
            case "REINTEGRATION" -> "Reintegración";
            default              -> movementType;
        };
    }

    public String getDayLabel()  { return createdAt == null ? "" : capitalize(createdAt.format(DAY)); }
    public String getDateLabel() { return createdAt == null ? "" : capitalize(createdAt.format(DAY_YEAR)); }

    /** Spanish months come out lowercase; the frame shows "12 Jun". */
    private String capitalize(String formatted) {
        int space = formatted.indexOf(' ');
        if (space < 0 || space + 1 >= formatted.length()) return formatted;
        return formatted.substring(0, space + 1)
             + Character.toUpperCase(formatted.charAt(space + 1))
             + formatted.substring(space + 2);
    }
}
