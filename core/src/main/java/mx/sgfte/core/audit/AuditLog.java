package mx.sgfte.core.audit;

import java.time.LocalDateTime;

/** One immutable audit row: what happened, how serious, who did it, from where. */
public class AuditLog {
    private Long id;
    private String eventType;
    private String severity;    // INFO | ALERTA | CRIT
    private String module;      // Seguridad, Fondos, Cuentas, Tarjetas, Empleados…
    private String detail;
    private String actor;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AuditLog() {}

    public AuditLog(AuditEvent event, String detail, String actor, String ipAddress) {
        this.eventType = event.name();
        this.severity = event.getSeverity();
        this.module = event.getModule();
        this.detail = detail;
        this.actor = actor;
        this.ipAddress = ipAddress;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** What the "ACCIÓN" column shows, resolved from the stored code. */
    public String getAction() { return AuditEvent.labelOf(eventType); }

    /*
      La celda de fecha va en dos líneas. Se formatea aquí y no en el JSP: JSTL
      no sabe con java.time, y partir el toString() por posiciones fijas es
      frágil — LocalDateTime omite los segundos cuando son cero, así que la
      segunda línea saldría cortada justo en los registros de segundo exacto.
    */
    private static final java.time.format.DateTimeFormatter DATE =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final java.time.format.DateTimeFormatter TIME =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

    /*
      createdAt viene de la base en UTC (DEFAULT SYSTIMESTAMP sobre un servidor
      en UTC), así que se traduce a la zona de la empresa justo aquí, al
      convertirse en texto. Ver AppTime: dentro del sistema todo sigue en UTC.
    */
    private java.time.LocalDateTime local() {
        return mx.sgfte.core.shared.time.AppTime.display(createdAt);
    }

    public String getDateLabel() { return createdAt == null ? "" : local().format(DATE); }
    public String getTimeLabel() { return createdAt == null ? "" : local().format(TIME); }
}
