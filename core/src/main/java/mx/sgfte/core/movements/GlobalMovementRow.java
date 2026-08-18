package mx.sgfte.core.movements;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Una fila de la vista global de movimientos (/admin/movimientos).
 *
 * Viene de v_movement, así que puede ser de cualquiera de los dos ledgers. Las
 * columnas de cuenta son NULL cuando scope es CONCENTRADORA: esa tabla no
 * guarda a qué cuenta fue el dinero. Los getters de etiqueta resuelven ese
 * hueco aquí y no en el JSP, que no tiene por qué saber que existen dos libros.
 *
 * Clase y no record, y con getters JavaBean: Tomcat 10.1 trae EL 5.0, que
 * resuelve ${m.kind} por getKind() y no ve el accesor de un record. Un record
 * pelado revienta a media renderización y, como el búfer de 8 KB del JSP ya se
 * vació, la respuesta llega truncada en vez de con un 500 — parece que la red
 * se colgó. Es el bug de CategoryAdminRow.
 */
public class GlobalMovementRow {

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter DAY  = DateTimeFormatter.ofPattern("dd/MMM/yyyy", ES_MX);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.US);

    private final String scope;          // CUENTA | CONCENTRADORA
    private final long sourceId;
    private final String movementType;
    private final BigDecimal amount;
    private final String direction;      // IN | OUT
    private final String description;
    private final Long accountId;
    private final String accountNumber;
    private final String holder;
    private final String employeeCode;
    private final String categoryName;
    private final Integer colorIndex;
    private final String relatedNumber;
    private final String actor;
    private final LocalDateTime createdAt;

    public GlobalMovementRow(String scope, long sourceId, String movementType,
                             BigDecimal amount, String direction, String description,
                             Long accountId, String accountNumber, String holder,
                             String employeeCode, String categoryName, Integer colorIndex,
                             String relatedNumber, String actor, LocalDateTime createdAt) {
        this.scope = scope;
        this.sourceId = sourceId;
        this.movementType = movementType;
        this.amount = amount;
        this.direction = direction;
        this.description = description;
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.holder = holder;
        this.employeeCode = employeeCode;
        this.categoryName = categoryName;
        this.colorIndex = colorIndex;
        this.relatedNumber = relatedNumber;
        this.actor = actor;
        this.createdAt = createdAt;
    }

    public String getScope()            { return scope; }
    public long getSourceId()           { return sourceId; }
    public String getMovementType()     { return movementType; }
    public BigDecimal getAmount()       { return amount; }
    public String getDirection()        { return direction; }
    public String getDescription()      { return description; }
    public Long getAccountId()          { return accountId; }
    public String getAccountNumber()    { return accountNumber; }
    public String getHolder()           { return holder; }
    public String getEmployeeCode()     { return employeeCode; }
    public String getCategoryName()     { return categoryName; }
    public Integer getColorIndex()      { return colorIndex; }
    public String getRelatedNumber()    { return relatedNumber; }
    public String getActor()            { return actor; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Lo decide la vista, no el JSP.
     *
     * "Entra" significa cosas distintas en cada libro —un DEPOSIT entra a la
     * cuenta, un FUNDING entra a la Concentradora, y una DISPERSION es entrada
     * de un lado y salida del otro—, así que la regla se resuelve en el UNION,
     * donde se sabe de qué lado se está mirando.
     */
    public boolean isInflow() { return "IN".equals(direction); }

    public boolean isConcentrator() { return "CONCENTRADORA".equals(scope); }

    /**
     * Qué clase de movimiento es.
     *
     * REINTEGRATION sale en los dos ledgers y significa lo mismo —dinero que
     * vuelve—, sólo cambia el lado. La columna de monto ya lo desambigua con el
     * signo, así que no hacen falta dos etiquetas.
     */
    public String getKind() {
        return switch (movementType) {
            case "DEPOSIT"       -> "Recarga de cuenta";
            case "WITHDRAWAL"    -> "Consumo con tarjeta";
            case "TRANSFER_IN"   -> "Transferencia recibida";
            case "TRANSFER_OUT"  -> "Transferencia enviada";
            case "REINTEGRATION" -> "Reintegración";
            case "FUNDING"       -> "Fondeo externo";
            case "DISPERSION"    -> "Dispersión a cuenta";
            default              -> movementType;
        };
    }

    /**
     * Línea del concepto: lo que se escribió al mover el dinero.
     *
     * Cuando no hay nada escrito va un guion, y NO el tipo, aunque el portal sí
     * caiga al tipo en su historial. Allí no existe columna TIPO y el hueco se
     * quedaría vacío; aquí sí existe, y concentrator_movement no guarda
     * descripción en ninguna de sus filas — así que caer al tipo escribía
     * "Dispersión a cuenta" dos veces en la misma fila, en las 200. Repetir un
     * dato no es informar: el guion dice "no se escribió nada" y deja la
     * columna legible.
     */
    public String getConcept() {
        return (description == null || description.isBlank()) ? "—" : description;
    }

    /**
     * Columna CUENTA.
     *
     * La Concentradora no tiene número de cuenta —es una fila única, sin
     * columna de código—, así que se nombra. El resto muestra el suyo.
     *
     * "Concentradora" a secas y no "Cuenta Concentradora": la columna ya se
     * llama CUENTA, así que el prefijo sólo repetía el encabezado, y encima no
     * cabía — se recortaba a "Cuenta Conc…", que es peor que no ponerlo.
     */
    public String getAccountLabel() {
        return isConcentrator() ? "Concentradora" : accountNumber;
    }

    /**
     * Columna TITULAR.
     *
     * Del lado de la Concentradora no hay titular; el dato equivalente es quién
     * la fondeó, y sólo el fondeo lo tiene — una dispersión la dispara el
     * sistema dentro de la transacción y su actor es NULL a propósito. Cuando
     * no hay ninguno de los dos, un guion: dice "aquí no aplica", que es
     * distinto de "se perdió el dato".
     */
    public String getWhoLabel() {
        if (!isConcentrator()) return holder;
        return (actor == null || actor.isBlank()) ? "—" : actor;
    }

    /** "13/Oct/2026" — el mes capitalizado, igual que en el portal. */
    public String getDayLabel() {
        if (createdAt == null) return "";
        String day = DAY.format(createdAt);
        int slash = day.indexOf('/');
        if (slash < 0 || slash + 1 >= day.length()) return day;
        return day.substring(0, slash + 1)
             + Character.toUpperCase(day.charAt(slash + 1))
             + day.substring(slash + 2);
    }

    public String getTimeLabel() { return createdAt == null ? "" : TIME.format(createdAt); }
}
