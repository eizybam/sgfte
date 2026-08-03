package mx.sgfte.core.audit;

import java.time.LocalDateTime;

/** One immutable audit row: what happened, who did it, when. */
public class AuditLog {
    private Long id;
    private String eventType;
    private String detail;
    private String actor;
    private LocalDateTime createdAt;

    public AuditLog() {}
    public AuditLog(String eventType, String detail, String actor) {
        this.eventType = eventType; this.detail = detail; this.actor = actor;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
