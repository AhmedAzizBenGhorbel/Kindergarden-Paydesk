package almohtadinepaydesk.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    public static Connection getServerConnection() throws SQLException {
        var settings = DatabaseConfig.getSettings();
        return DriverManager.getConnection(settings.jdbcUrl(false), settings.user(), settings.password());
    }

    public static Connection getConnection() throws SQLException {
        var settings = DatabaseConfig.getSettings();
        return DriverManager.getConnection(settings.jdbcUrl(true), settings.user(), settings.password());
    }

    private DatabaseConnection() {
    }
}
