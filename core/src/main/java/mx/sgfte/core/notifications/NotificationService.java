package mx.sgfte.core.notifications;

import mx.sgfte.core.audit.AuditEvent;
import mx.sgfte.core.audit.AuditLogService;

import jakarta.mail.MessagingException;

public class NotificationService {

    private final AuditLogService auditLogService = new AuditLogService();
    private final EmailSender emailSender = new EmailSender();

    public void send(String to, String subject, String body) {
        System.out.println("[NOTIFY] to=" + to + " | " + subject + " | " + body);

        try {
            emailSender.send(to, subject, body);
            auditLogService.record(AuditEvent.NOTIFICATION, subject + " -> " + to, "system", null);
        } catch (MessagingException e) {
            System.err.println("[NOTIFY][ERROR] No se pudo enviar a " + to + ": " + e.getMessage());
            auditLogService.record(AuditEvent.NOTIFICATION, subject + " -> " + to + " [FALLÓ: " + e.getMessage() + "]", "system", null);
        }
    }
}