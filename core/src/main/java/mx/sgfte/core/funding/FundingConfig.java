package mx.sgfte.core.funding;

/**
 * Configuración del fondeo.
 *
 * Mismo patrón que MailConfig: se lee del entorno y hay un respaldo para que la
 * demo arranque sin configurar nada.
 */
public final class FundingConfig {

    /**
     * Secreto compartido con el banco para firmar las notificaciones.
     *
     * El respaldo NO es un secreto: es una cadena que dice en voz alta que
     * nadie la configuró, y así aparece en los registros el día que alguien
     * despliegue esto sin variable de entorno. Poner aquí una clave de verdad
     * —como pasó con SGFTE_MAIL_PASSWORD, que sigue en el código con una
     * contraseña de aplicación de Gmail dentro— sólo consigue que quien tenga
     * el repositorio tenga la clave.
     *
     * En producción: SGFTE_BANK_SECRET en el .env, que ya está en .gitignore.
     */
    public static final String BANK_SECRET =
            env("SGFTE_BANK_SECRET", "CAMBIAME-secreto-de-desarrollo-no-usar-en-produccion");

    /** ¿Se está corriendo con el respaldo? Lo usa la pantalla para avisar. */
    public static boolean usingDefaultSecret() {
        return BANK_SECRET.startsWith("CAMBIAME");
    }

    private FundingConfig() {}

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
