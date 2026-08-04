package mx.sgfte.core.notifications;

final class MailConfig {

    static final String HOST = "smtp.gmail.com";
    static final int PORT = 587;
    static final String USERNAME = "sgftenotification@gmail.com";
    static final String PASSWORD = "ikvb xwxn xdbc jdqt";
    static final boolean AUTH = true;
    static final boolean STARTTLS = true;
    private MailConfig() {}

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}