package almohtadinepaydesk.database;

import almohtadinepaydesk.config.AppConfig;

public class DatabaseConfig {

    public static final String SERVER_URL = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC";
    public static final String URL = "jdbc:mysql://localhost:3306/"
            + AppConfig.DATABASE_NAME
            + "?useSSL=false&serverTimezone=UTC";
    public static final String USER = "root";
    public static final String PASSWORD = "";

    private DatabaseConfig() {
    }
}
