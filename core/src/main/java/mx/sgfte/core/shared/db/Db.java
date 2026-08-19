package mx.sgfte.core.shared.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Minimal JDBC connection factory. Reads db.properties from the classpath.
 * For the demo we use DriverManager; a connection pool (HikariCP) is a later improvement.
 */
public final class Db {

    /*
      Sin esto, un getConnection() no tiene límite: si el listener de Oracle
      tarda en responder —el contenedor apenas arrancando, la máquina bajo
      carga, un blip de red—, el hilo de la petición se queda esperando para
      siempre y la página "carga eternamente" sin ningún error que lo
      explique. Es el mismo problema que ya se resolvió para SMTP en
      EmailSender (props mail.smtp.connectiontimeout/timeout); aquí no tenía
      su equivalente.

      CONNECT_TIMEOUT cubre el handshake TCP inicial; ReadTimeout cubre
      quedarse esperando una respuesta después de ya conectado (una consulta
      que no vuelve). Los dos en milisegundos.
     */
    private static final String CONNECT_TIMEOUT_MS = "5000";
    private static final String READ_TIMEOUT_MS = "10000";

    private static final Properties CONFIG = load();

    static {
        // Explicitly register the Oracle driver. When the driver jar lives inside the
        // WAR (WEB-INF/lib), DriverManager's automatic ServiceLoader registration runs
        // under the container/system classloader and does NOT see the webapp's driver,
        // which surfaces as "No suitable driver found". Loading the class here forces
        // its static initializer to register it via the webapp classloader.
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Oracle JDBC driver (ojdbc11) not on the classpath", e);
        }
    }

    private Db() {}

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = Db.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties not found on the classpath");
            }
            p.load(in);
            return p;
        } catch (Exception e) {
            throw new IllegalStateException("Could not load db.properties", e);
        }
    }

    private static String cfg(String key, String env) {
               String v = System.getenv(env);
               if (v != null && !v.isBlank()) return v;
               return CONFIG.getProperty(key);
    }


    public static Connection getConnection() throws SQLException {
        Properties props = new Properties();
        props.setProperty("user", CONFIG.getProperty("db.user"));
        props.setProperty("password", CONFIG.getProperty("db.password"));
        props.setProperty("oracle.net.CONNECT_TIMEOUT", CONNECT_TIMEOUT_MS);
        props.setProperty("oracle.jdbc.ReadTimeout", READ_TIMEOUT_MS);
        return DriverManager.getConnection(CONFIG.getProperty("db.url"), props);
    }
}
