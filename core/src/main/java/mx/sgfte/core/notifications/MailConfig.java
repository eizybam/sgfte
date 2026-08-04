package mx.sgfte.core.notifications;

/**
 * Datos del servidor de correo.
 *
 * Se leen del entorno y, si no está definido, se cae en el valor de abajo para
 * que la demo siga funcionando sin configurar nada. El ayudante env() ya estaba
 * escrito aquí pero no se usaba: las constantes se leían directas.
 *
 * OJO: SGFTE_MAIL_PASSWORD tiene como respaldo una contraseña de aplicación de
 * Gmail escrita en el código, y este archivo está en git. Quien tenga acceso al
 * repositorio la tiene. Conviene revocarla en la cuenta de Google, generar otra
 * y pasarla por variable de entorno; borrarla de aquí no la saca del historial.
 */
final class MailConfig {

    static final String HOST = env("SGFTE_MAIL_HOST", "smtp.gmail.com");

    /*
      465 con SSL directo, y no 587 con STARTTLS.

      El 587 quedó descartado a la vista de los hechos: en la red donde corre
      esto la conexión TCP abre pero la negociación de STARTTLS no recibe
      respuesta y muere por tiempo agotado. El 465 cifra desde el saludo, así
      que no hay negociación intermedia que filtrar.

      Para volver al otro transporte: SGFTE_MAIL_PORT=587 y SGFTE_MAIL_SSL=false.
     */
    static final int PORT = Integer.parseInt(env("SGFTE_MAIL_PORT", "465"));
    static final boolean SSL = Boolean.parseBoolean(env("SGFTE_MAIL_SSL", "true"));
    static final String USERNAME = env("SGFTE_MAIL_USER", "sgftenotification@gmail.com");
    static final String PASSWORD = env("SGFTE_MAIL_PASSWORD", "ikvb xwxn xdbc jdqt");
    static final boolean AUTH = Boolean.parseBoolean(env("SGFTE_MAIL_AUTH", "true"));
    static final boolean STARTTLS = Boolean.parseBoolean(env("SGFTE_MAIL_STARTTLS", "true"));

    /** Remitente visible. Si no se indica, el propio usuario de la cuenta. */
    static final String FROM = env("SGFTE_MAIL_FROM", USERNAME);

    private MailConfig() {}

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
