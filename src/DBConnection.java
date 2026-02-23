import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class DBConnection {

    private static final String URL =
        "jdbc:mysql://localhost:3306/voting_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "admin";

    static {
        loadMySqlDriver();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private static void loadMySqlDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return;
        } catch (ClassNotFoundException ignored) {
            // Try loading from local lib paths when classpath is missing the connector.
        }

        String[] candidatePaths = {
            "lib/mysql-connector-j-9.6.0.jar",
            "../lib/mysql-connector-j-9.6.0.jar",
            "C:/Users/Administrator/Desktop/Java_Project/lib/mysql-connector-j-9.6.0.jar"
        };

        for (String path : candidatePaths) {
            File jar = new File(path);
            if (!jar.exists()) {
                continue;
            }

            try {
                URL jarUrl = jar.toURI().toURL();
                URLClassLoader loader = new URLClassLoader(new URL[] { jarUrl }, DBConnection.class.getClassLoader());
                Class<?> driverClass = Class.forName("com.mysql.cj.jdbc.Driver", true, loader);
                Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
                DriverManager.registerDriver(new DriverShim(driver));
                return;
            } catch (Exception ignored) {
                // Try next candidate.
            }
        }

        throw new RuntimeException("MySQL JDBC driver not found. Add mysql-connector-j-9.6.0.jar to classpath or lib folder.");
    }

    private static class DriverShim implements Driver {
        private final Driver driver;

        DriverShim(Driver driver) {
            this.driver = driver;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return driver.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }
    }
}
