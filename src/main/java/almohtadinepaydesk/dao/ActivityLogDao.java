package almohtadinepaydesk.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.database.DatabaseConnection;
import almohtadinepaydesk.models.ActivityLog;

public class ActivityLogDao {

    private String lastErrorMessage = "";

    public boolean log(Integer userId, String action, String tableName, Integer recordId, String details) {
        String sql = """
                INSERT INTO activity_logs (user_id, action, table_name, record_id, details)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            if (userId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, userId);
            }

            statement.setString(2, action);
            statement.setString(3, tableName);

            if (recordId == null) {
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(4, recordId);
            }

            statement.setString(5, details);
            boolean saved = statement.executeUpdate() > 0;
            lastErrorMessage = saved ? "" : "Aucun journal d'activit\u00e9 n'a \u00e9t\u00e9 enregistr\u00e9.";
            return saved;
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public List<ActivityLog> listRecentLogs() {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = """
                SELECT al.*,
                       CASE
                           WHEN u.full_name IS NOT NULL AND u.full_name <> '' THEN u.full_name
                           WHEN u.username IS NOT NULL THEN u.username
                           ELSE 'Syst\u00e8me'
                       END AS user_full_name
                FROM activity_logs al
                LEFT JOIN users u ON u.id = al.user_id
                ORDER BY al.activity_date DESC, al.id DESC
                LIMIT 300
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                logs.add(mapActivityLog(resultSet));
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return logs;
    }

    public String getLastErrorMessage() {
        if (lastErrorMessage == null || lastErrorMessage.isBlank()) {
            return "Une erreur est survenue pendant l'op\u00e9ration.";
        }

        return lastErrorMessage;
    }

    private ActivityLog mapActivityLog(ResultSet resultSet) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setId(resultSet.getInt("id"));

        int userId = resultSet.getInt("user_id");
        if (!resultSet.wasNull()) {
            log.setUserId(userId);
        }

        log.setAction(resultSet.getString("action"));
        log.setTableName(resultSet.getString("table_name"));

        int recordId = resultSet.getInt("record_id");
        if (!resultSet.wasNull()) {
            log.setRecordId(recordId);
        }

        log.setDetails(resultSet.getString("details"));
        log.setUserFullName(resultSet.getString("user_full_name"));

        if (resultSet.getTimestamp("activity_date") != null) {
            log.setActivityDate(resultSet.getTimestamp("activity_date").toLocalDateTime());
        }

        return log;
    }

    private String createFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "La table des journaux d'activit\u00e9 n'existe pas. Cliquez sur Initialiser la base.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }
}
