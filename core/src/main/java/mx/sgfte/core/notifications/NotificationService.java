package mx.sgfte.core.notifications;

import mx.sgfte.core.audit.AuditLogService;

/**
 * Notifications microservice (BASE / stub). For now it just logs the notification
 * to the console and to the audit trail. Sending real email/push (JavaMail) is a
 * later enhancement — this keeps the concept and the call sites in place.
 */
public class NotificationService {

    private final AuditLogService auditLogService = new AuditLogService();

    public void send(String to, String subject, String body) {
        // TODO (siguiente iteración): enviar email real con JavaMail.
        System.out.println("[NOTIFY] to=" + to + " | " + subject + " | " + body);
        auditLogService.record("NOTIFICATION", subject + " -> " + to, "system");
    }
}
