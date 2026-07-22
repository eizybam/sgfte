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

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                CONFIG.getProperty("db.url"),
                CONFIG.getProperty("db.user"),
                CONFIG.getProperty("db.password"));
    }
}
