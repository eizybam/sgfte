package mx.sgfte.core.audit;

/**
 * The vocabulary of the audit trail: what can be recorded, how serious it is,
 * which module it belongs to, and how it reads in the Logs screen.
 *
 * Keeping the four together in one enum is the point. The alternative — a bare
 * event_type string plus a severity picked at each call site — is how a trail
 * ends up with LOGIN_FAIL logged as INFO in one place and ALERTA in another,
 * and with two spellings of the same event.
 *
 * The Spanish label is what the "ACCIÓN" column shows; the database keeps the
 * code, so renaming a label never rewrites history.
 */
public enum AuditEvent {

    // --- Seguridad ---
    LOGIN_OK      (Severity.INFO,   Module.SEGURIDAD, "Inicio de sesión"),
    LOGIN_FAILED  (Severity.ALERTA, Module.SEGURIDAD, "Intento de login fallido"),
    LOGOUT        (Severity.INFO,   Module.SEGURIDAD, "Cierre de sesión"),

    // --- Fondos ---
    CONCENTRATOR_FUNDED (Severity.INFO,   Module.FONDOS, "Concentradora fondeada"),
    DISPERSION          (Severity.INFO,   Module.FONDOS, "Dispersión de fondos"),
    DISPERSION_REJECTED (Severity.ALERTA, Module.FONDOS, "Dispersión rechazada"),
    TRANSFER            (Severity.INFO,   Module.FONDOS, "Transferencia entre cuentas"),

    // --- Cuentas ---
    ACCOUNT_CREATED (Severity.INFO, Module.CUENTAS, "Cuenta creada"),
    ACCOUNT_DELETED (Severity.CRIT, Module.CUENTAS, "Cuenta eliminada"),

    // --- Tarjetas ---
    CARD_ISSUED      (Severity.INFO, Module.TARJETAS, "Nueva tarjeta asignada"),
    CARD_INVALIDATED (Severity.CRIT, Module.TARJETAS, "Tarjeta invalidada"),

    // --- Empleados ---
    CARDHOLDER_CREATED (Severity.INFO, Module.EMPLEADOS, "Empleado registrado"),
    CARDHOLDER_DELETED (Severity.CRIT, Module.EMPLEADOS, "Usuario eliminado"),

    // --- Microservicios ---
    NOTIFICATION (Severity.INFO, Module.NOTIFICACIONES, "Notificación enviada");

    /** Los tres niveles del segmentado de la pantalla. */
    public static final class Severity {
        public static final String INFO   = "INFO";
        public static final String ALERTA = "ALERTA";
        public static final String CRIT   = "CRIT";
        private Severity() {}
    }

    /** Los módulos que alimentan la píldora "MÓDULO". */
    public static final class Module {
        public static final String SEGURIDAD      = "Seguridad";
        public static final String FONDOS         = "Fondos";
        public static final String CUENTAS        = "Cuentas";
        public static final String TARJETAS       = "Tarjetas";
        public static final String EMPLEADOS      = "Empleados";
        public static final String NOTIFICACIONES = "Notificaciones";
        private Module() {}
    }

    private final String severity;
    private final String module;
    private final String label;

    AuditEvent(String severity, String module, String label) {
        this.severity = severity;
        this.module = module;
        this.label = label;
    }

    public String getSeverity() { return severity; }
    public String getModule() { return module; }
    public String getLabel() { return label; }

    /** Label for a stored code, tolerating rows written before this enum existed. */
    public static String labelOf(String eventType) {
        if (eventType == null) return "";
        try {
            return valueOf(eventType).getLabel();
        } catch (IllegalArgumentException e) {
            return eventType;
        }
    }
}
