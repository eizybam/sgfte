package mx.sgfte.core.portal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** One row of "Historial de movimientos" (Figma 109:4). */
public class PortalMovementRow {

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MMM/yyyy", ES_MX);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    private final String movementType;
    private final String description;
    private final String purpose;
    private final String accountNumber;
    private final String relatedAccountNumber;
    private final BigDecimal amount;
    private final LocalDateTime createdAt;

    public PortalMovementRow(String movementType, String description, String purpose,
                             String accountNumber, String relatedAccountNumber,
                             BigDecimal amount, LocalDateTime createdAt) {
        this.movementType = movementType;
        this.description = description;
        this.purpose = purpose;
        this.accountNumber = accountNumber;
        this.relatedAccountNumber = relatedAccountNumber;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public BigDecimal getAmount() { return amount; }
    public String getPurpose() { return purpose; }
    public String getAccountNumber() { return accountNumber; }

    public boolean isInflow() {
        return "DEPOSIT".equals(movementType) || "TRANSFER_IN".equals(movementType);
    }

    /** "13/Oct/2026" — el marco capitaliza el mes. */
    public String getDayLabel() {
        if (createdAt == null) return "";
        String day = DAY.format(local());
        int slash = day.indexOf('/');
        return day.substring(0, slash + 1)
             + Character.toUpperCase(day.charAt(slash + 1))
             + day.substring(slash + 2);
    }

    public String getTimeLabel() { return createdAt == null ? "" : TIME.format(local()); }

    /** Lo guardado es UTC; en pantalla va en la zona de la empresa. Ver AppTime. */
    private LocalDateTime local() {
        return mx.sgfte.core.shared.time.AppTime.display(createdAt);
    }

    /** Línea principal del concepto: lo que se escribió al mover el dinero. */
    public String getConcept() {
        return description == null || description.isBlank() ? getKind() : description;
    }

    /** Y debajo, de qué clase de movimiento se trata. */
    public String getKind() {
        return switch (movementType) {
            case "DEPOSIT"       -> "Recarga de cuenta";
            case "WITHDRAWAL"    -> "Consumo con tarjeta";
            case "TRANSFER_IN"   -> "Transferencia recibida";
            case "TRANSFER_OUT"  -> "Transferencia enviada";
            case "REINTEGRATION" -> "Reintegración";
            default              -> movementType;
        };
    }

    /**
     * Columna DESTINATARIO.
     *
     * El marco enseña a veces una tarjeta ("****3456") y a veces una cuenta
     * ("G-28D748"). account_movement no apunta a ninguna tarjeta —sólo a la
     * cuenta y, en las transferencias, a la cuenta relacionada—, así que va esa
     * cuenta. Cuando no hay contraparte (una recarga viene de la Concentradora)
     * se dice de dónde vino en vez de dejar el hueco.
     */
    public String getCounterparty() {
        if (relatedAccountNumber != null && !relatedAccountNumber.isBlank()) {
            return relatedAccountNumber;
        }
        return switch (movementType) {
            case "DEPOSIT"       -> "Cuenta Concentradora";
            case "REINTEGRATION" -> "Cuenta Concentradora";
            default              -> accountNumber;
        };
    }

    public String getIcon() { return PortalIcons.forPurpose(purpose); }
}
