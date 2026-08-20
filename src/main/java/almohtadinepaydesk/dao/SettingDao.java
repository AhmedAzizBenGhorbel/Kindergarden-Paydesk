package almohtadinepaydesk.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import almohtadinepaydesk.database.DatabaseConnection;

public class SettingDao {

    private String lastErrorMessage = "";

    public String findValueByKey(String settingKey, String defaultValue) {
        String sql = """
                SELECT setting_value
                FROM settings
                WHERE setting_key = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, settingKey);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    lastErrorMessage = "";
                    return resultSet.getString("setting_value");
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return defaultValue;
    }

    public boolean saveSetting(String settingKey, String settingValue, String description) {
        String sql = """
                INSERT INTO settings (setting_key, setting_value, description)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    setting_value = VALUES(setting_value),
                    description = VALUES(description)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, settingKey);
            statement.setString(2, settingValue);
            statement.setString(3, description);

            boolean saved = statement.executeUpdate() > 0;
            lastErrorMessage = saved ? "" : "Aucun param\u00e8tre n'a \u00e9t\u00e9 enregistr\u00e9.";
            return saved;
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public String getLastErrorMessage() {
        if (lastErrorMessage == null || lastErrorMessage.isBlank()) {
            return "Une erreur est survenue pendant l'op\u00e9ration.";
        }

        return lastErrorMessage;
    }

    private String createFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "La table des param\u00e8tres n'existe pas. Cliquez sur Initialiser la base.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }
}
