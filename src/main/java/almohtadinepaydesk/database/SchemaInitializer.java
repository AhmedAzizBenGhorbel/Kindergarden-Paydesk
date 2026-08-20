package almohtadinepaydesk.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import almohtadinepaydesk.security.PasswordUtil;

public class SchemaInitializer {

    private static final String SCHEMA_FILE = "/database/schema.sql";
    private static final String SEED_FILE = "/database/seed.sql";

    public static boolean initializeDatabase() {
        try (Connection connection = DatabaseConnection.getServerConnection()) {
            executeSqlFile(connection, SCHEMA_FILE);
            executeSqlFile(connection, SEED_FILE);
            updateChildrenTableForCurrentVersion(connection);
            upgradeDefaultAdminPassword(connection);
            return true;
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void executeSqlFile(Connection connection, String resourcePath)
            throws IOException, SQLException {
        String sql = readResourceFile(resourcePath);
        String sqlWithoutComments = removeCommentLines(sql);
        String[] commands = sqlWithoutComments.split(";");

        try (Statement statement = connection.createStatement()) {
            for (String command : commands) {
                if (!command.isBlank()) {
                    statement.execute(command);
                }
            }
        }
    }

    private static void updateChildrenTableForCurrentVersion(Connection connection) throws SQLException {
        addColumnIfMissing(
                connection,
                "children",
                "class_group",
                "ALTER TABLE children ADD COLUMN class_group VARCHAR(80) NULL AFTER last_name");

        addColumnIfMissing(
                connection,
                "children",
                "registration_date",
                "ALTER TABLE children ADD COLUMN registration_date DATE NULL AFTER parent_phone");

        addColumnIfMissing(
                connection,
                "children",
                "monthly_fee",
                "ALTER TABLE children ADD COLUMN monthly_fee DECIMAL(10,3) NOT NULL DEFAULT 0.000 AFTER registration_date");
    }

    private static void addColumnIfMissing(Connection connection, String tableName, String columnName, String alterSql)
            throws SQLException {
        if (!columnExists(connection, tableName, columnName)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(alterSql);
            }
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS column_count
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("column_count") > 0;
            }
        }
    }

    private static void upgradeDefaultAdminPassword(Connection connection) throws SQLException {
        String selectSql = """
                SELECT id, password_hash, password_salt
                FROM users
                WHERE username = 'admin'
                """;

        try (PreparedStatement selectStatement = connection.prepareStatement(selectSql);
                ResultSet resultSet = selectStatement.executeQuery()) {

            if (resultSet.next()) {
                int userId = resultSet.getInt("id");
                String currentHash = resultSet.getString("password_hash");
                String currentSalt = resultSet.getString("password_salt");

                if (currentSalt == null || currentSalt.isBlank() || "admin123".equals(currentHash)) {
                    String newSalt = PasswordUtil.generateSalt();
                    String newHash = PasswordUtil.hashPassword("admin123", newSalt);
                    updateDefaultAdminPassword(connection, userId, newHash, newSalt);
                }
            }
        }
    }

    private static void updateDefaultAdminPassword(Connection connection, int userId, String hash, String salt)
            throws SQLException {
        String updateSql = "UPDATE users SET password_hash = ?, password_salt = ? WHERE id = ?";

        try (PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
            updateStatement.setString(1, hash);
            updateStatement.setString(2, salt);
            updateStatement.setInt(3, userId);
            updateStatement.executeUpdate();
        }
    }

    private static String readResourceFile(String resourcePath) throws IOException {
        try (InputStream inputStream = SchemaInitializer.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Fichier introuvable: " + resourcePath);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String removeCommentLines(String sql) {
        StringBuilder result = new StringBuilder();
        String[] lines = sql.split("\\R");

        for (String line : lines) {
            if (!line.trim().startsWith("--")) {
                result.append(line).append(System.lineSeparator());
            }
        }

        return result.toString();
    }

    private SchemaInitializer() {
    }
}
