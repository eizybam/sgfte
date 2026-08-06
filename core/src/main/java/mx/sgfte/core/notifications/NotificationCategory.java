package mx.sgfte.core.notifications;

import mx.sgfte.core.audit.AuditEvent;

/**
 * The two buckets the cardholder's Notificaciones screen filters by.
 *
 * A cardholder does not think in the admin's five audit modules (Seguridad,
 * Fondos, Cuentas, Tarjetas, Empleados) — they think "is this about my login/
 * password" or "is this about my account". Derived from AuditEvent.Module so
 * the mapping cannot drift from the vocabulary audit_log already uses.
 */
public enum NotificationCategory {
    SEGURIDAD,
    ADMINISTRATIVA;

    public static NotificationCategory of(AuditEvent event) {
        return AuditEvent.Module.SEGURIDAD.equals(event.getModule()) ? SEGURIDAD : ADMINISTRATIVA;
    }
}
