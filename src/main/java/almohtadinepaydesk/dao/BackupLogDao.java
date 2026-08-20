package almohtadinepaydesk.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.database.DatabaseConnection;
import almohtadinepaydesk.models.BackupLog;

public class BackupLogDao {

    private String lastErrorMessage = "";

    public boolean createBackupLog(BackupLog backupLog) {
        String sql = """
                INSERT INTO backup_logs (file_name, folder_path, status, message, created_by_user_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, backupLog.getFileName());
            statement.setString(2, backupLog.getFolderPath());
            statement.setString(3, backupLog.getStatus());
            statement.setString(4, backupLog.getMessage());

            if (backupLog.getCreatedByUserId() == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setInt(5, backupLog.getCreatedByUserId());
            }

            boolean saved = statement.executeUpdate() > 0;
            lastErrorMessage = saved ? "" : "Aucun journal de sauvegarde n'a \u00e9t\u00e9 enregistr\u00e9.";
            return saved;
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public List<BackupLog> listBackupLogs() {
        List<BackupLog> backupLogs = new ArrayList<>();
        String sql = """
                SELECT *
                FROM backup_logs
                ORDER BY backup_date DESC, id DESC
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                backupLogs.add(mapBackupLog(resultSet));
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return backupLogs;
    }

    public String getLastErrorMessage() {
        if (lastErrorMessage == null || lastErrorMessage.isBlank()) {
            return "Une erreur est survenue pendant l'op\u00e9ration.";
        }

        return lastErrorMessage;
    }

    private BackupLog mapBackupLog(ResultSet resultSet) throws SQLException {
        BackupLog backupLog = new BackupLog();
        backupLog.setId(resultSet.getInt("id"));
        backupLog.setFileName(resultSet.getString("file_name"));
        backupLog.setFolderPath(resultSet.getString("folder_path"));
        backupLog.setStatus(resultSet.getString("status"));
        backupLog.setMessage(resultSet.getString("message"));

        int userId = resultSet.getInt("created_by_user_id");
        if (!resultSet.wasNull()) {
            backupLog.setCreatedByUserId(userId);
        }

        if (resultSet.getTimestamp("backup_date") != null) {
            backupLog.setBackupDate(resultSet.getTimestamp("backup_date").toLocalDateTime());
        }

        return backupLog;
    }

    private String createFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "La table des sauvegardes n'existe pas. Cliquez sur Initialiser la base.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }
}
