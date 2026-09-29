package almohtadinepaydesk.database;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Local settings: environment overrides local properties, then development defaults. */
public final class DatabaseConfig {
    public static Settings getSettings() throws java.sql.SQLException {
        try { return load(Path.of("paydesk.properties"), System.getenv()); }
        catch (IllegalArgumentException | IllegalStateException e) {
            throw new java.sql.SQLException("Invalid local database configuration.", "PDCFG");
        }
    }

    public static Settings load(Path file, Map<String, String> environment) {
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read local database configuration.");
            }
        }
        return new Settings(value(properties, environment, "host", "localhost"),
                value(properties, environment, "port", "3306"),
                value(properties, environment, "database", "almohtadine_paydesk_db"),
                value(properties, environment, "user", "root"),
                value(properties, environment, "password", ""),
                value(properties, environment, "mysqldump", ""));
    }

    private static String value(Properties properties, Map<String, String> environment, String key, String fallback) {
        return environment.getOrDefault("PAYDESK_DB_" + key.toUpperCase(java.util.Locale.ROOT),
                properties.getProperty("db." + key, fallback));
    }

    public record Settings(String host, String port, String database, String user, String password, String mysqldump) {
        public Settings {
            if (!host.matches("[A-Za-z0-9._-]+") || !database.matches("[A-Za-z0-9_]+")) {
                throw new IllegalArgumentException("Invalid database host or database name.");
            }
            try {
                int number = Integer.parseInt(port);
                if (number < 1 || number > 65535) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Database port must be between 1 and 65535.");
            }
        }
        public String jdbcUrl(boolean selectDatabase) {
            return "jdbc:mysql://" + host + ":" + port + "/" + (selectDatabase ? database : "")
                    + "?useSSL=false&serverTimezone=UTC&connectTimeout=5000&socketTimeout=15000";
        }
        // Never include credentials in generated diagnostics or record rendering.
        @Override public String toString() { return "Database settings (credentials omitted)"; }
    }
    private DatabaseConfig() { }
}
