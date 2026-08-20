package mx.sgfte.core.notifications;

import mx.sgfte.core.audit.AuditEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * One row of the cardholder's Notificaciones screen (Figma: "Notificaciones").
 *
 * Mirrors AuditLog's own trick: the title is never stored, it is resolved from
 * event_type through AuditEvent.labelOf() every time this is read, so renaming
 * a label in the enum relabels history instead of leaving stale text behind.
 */
public class NotificationRow {

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d 'de' MMMM", ES_MX);

    private final long id;
    private final String eventType;
    private final NotificationCategory category;
    private final String detail;
    private final LocalDateTime createdAt;

    public NotificationRow(long id, String eventType, NotificationCategory category,
                           String detail, LocalDateTime createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.category = category;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getDetail() { return detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** El título, resuelto del código guardado — nunca al revés. */
    public String getTitle() { return AuditEvent.labelOf(eventType); }

    public NotificationCategory getCategory() { return category; }

    /** "Seguridad" / "Administrativas", para la píldora bajo el título. */
    public String getCategoryLabel() {
        return category == NotificationCategory.SEGURIDAD ? "Seguridad" : "Administrativas";
    }

    /** Sprite id del icono cuadrado, elegido por el módulo del evento. */
    public String getIcon() { return NotificationIcons.forEvent(eventType); }

    /** Clave de agrupación: HOY, AYER, o la fecha, para partir la lista en secciones. */
    public String getGroupKey() {
        if (createdAt == null) return "";
        // El día se decide en la zona de la empresa, no en la del contenedor:
        // si no, un aviso de las 19:00 de CDMX ya contaba como de mañana.
        LocalDate day = local().toLocalDate();
        LocalDate today = mx.sgfte.core.shared.time.AppTime.today();
        if (day.equals(today)) return "HOY";
        if (day.equals(today.minusDays(1))) return "AYER";
        return day.toString();
    }

    /** "Hoy" / "Ayer" / "23 de mayo", el encabezado que se ve una vez por grupo. */
    public String getGroupLabel() {
        return switch (getGroupKey()) {
            case "HOY"  -> "Hoy";
            case "AYER" -> "Ayer";
            case ""     -> "";
            default     -> DAY_MONTH.format(local());
        };
    }

    /**
     * "Hace 10 minutos" hoy; "Ayer, 14:35" o la fecha para todo lo demás — igual
     * que dibuja el marco. Hoy es relativo porque es lo que se lee de un
     * vistazo; más allá de hoy, la hora exacta importa más que "hace 30 horas".
     */
    public String getWhen() {
        if (createdAt == null) return "";
        if ("HOY".equals(getGroupKey())) return relative();
        if ("AYER".equals(getGroupKey())) return "Ayer, " + TIME.format(local());
        return getGroupLabel() + ", " + TIME.format(local());
    }

    /** Lo guardado es UTC; en pantalla va en la zona de la empresa. Ver AppTime. */
    private LocalDateTime local() {
        return mx.sgfte.core.shared.time.AppTime.display(createdAt);
    }

    /*
      Ojo: aquí NO se convierte nada, y es correcto. Esto mide una duración
      entre dos instantes, y los dos están en el mismo reloj —lo guardado en
      UTC y el now() del contenedor, también UTC—. Traducir sólo uno de los dos
      convertiría "hace 3 minutos" en "hace 6 horas".
    */
    private String relative() {
        long minutes = ChronoUnit.MINUTES.between(createdAt, LocalDateTime.now());
        if (minutes < 1) return "Hace instantes";
        if (minutes < 60) return "Hace " + minutes + (minutes == 1 ? " minuto" : " minutos");
        long hours = minutes / 60;
        return "Hace " + hours + (hours == 1 ? " hora" : " horas");
    }
}
