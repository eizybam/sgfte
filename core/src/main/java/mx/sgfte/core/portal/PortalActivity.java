package mx.sgfte.core.portal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * One line of "ACTIVIDAD RECIENTE" on the cardholder's dashboard (Figma 64:4).
 *
 * Movements across every account the cardholder owns, newest first — the panel
 * mixes purchases and fund assignments, so it cannot be scoped to one account.
 */
public class PortalActivity {

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMMM", ES_MX);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mma", Locale.US);

    private final String movementType;
    private final String description;
    private final String purpose;
    private final BigDecimal amount;
    private final LocalDateTime createdAt;

    public PortalActivity(String movementType, String description, String purpose,
                          BigDecimal amount, LocalDateTime createdAt) {
        this.movementType = movementType;
        this.description = description;
        this.purpose = purpose;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public String getMovementType() { return movementType; }
    public BigDecimal getAmount() { return amount; }
    public String getPurpose() { return purpose; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Money arriving in the cardholder's accounts, shown in the accent colour. */
    public boolean isInflow() {
        return "DEPOSIT".equals(movementType) || "TRANSFER_IN".equals(movementType);
    }

    /**
     * What the row reads.
     *
     * A purchase carries its own description ("Pemex autopista km. 54"); a
     * deposit from the Concentrator usually does not, and the frame labels those
     * "Asignación de fondos", which is exactly what they are.
     */
    public String getLabel() {
        if (description != null && !description.isBlank()) return description;
        return switch (movementType) {
            case "DEPOSIT"       -> "Asignación de fondos";
            case "TRANSFER_IN"   -> "Transferencia recibida";
            case "TRANSFER_OUT"  -> "Transferencia enviada";
            case "REINTEGRATION" -> "Reintegración";
            default              -> "Movimiento";
        };
    }

    /**
     * "Hoy, 9:23am" · "Ayer, 11:07pm" · "23 mayo, 2:24pm", as the frame writes it.
     *
     * Today and yesterday get a word instead of a date because on a panel you
     * glance at, "Hoy" reads faster than working out whether 4 August was today.
     */
    public String getWhen() {
        if (createdAt == null) return "";
        LocalDate day = createdAt.toLocalDate();
        LocalDate today = LocalDate.now();
        String prefix;
        if (day.equals(today)) prefix = "Hoy";
        else if (day.equals(today.minusDays(1))) prefix = "Ayer";
        else prefix = DAY_MONTH.format(createdAt);
        return prefix + ", " + TIME.format(createdAt).toLowerCase(Locale.US);
    }

    /** Sprite id for the little square icon, chosen from the account's purpose. */
    public String getIcon() { return PortalIcons.forPurpose(purpose); }
}
