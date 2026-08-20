package almohtadinepaydesk.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.database.DatabaseConnection;
import almohtadinepaydesk.models.SchoolYear;

public class SchoolYearDao {

    private String lastErrorMessage = "";

    public boolean createSchoolYear(SchoolYear schoolYear) {
        String sql = """
                INSERT INTO school_years (name, start_date, end_date, active)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, schoolYear.getName());
            statement.setDate(2, Date.valueOf(schoolYear.getStartDate()));
            statement.setDate(3, Date.valueOf(schoolYear.getEndDate()));
            statement.setBoolean(4, schoolYear.isActive());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                lastErrorMessage = "Aucune ann\u00e9e scolaire n'a \u00e9t\u00e9 ajout\u00e9e.";
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    schoolYear.setId(generatedKeys.getInt(1));
                }
            }

            lastErrorMessage = "";
            return true;
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean setActiveSchoolYear(int schoolYearId) {
        String deactivateSql = "UPDATE school_years SET active = FALSE";
        String activateSql = "UPDATE school_years SET active = TRUE WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement deactivateStatement = connection.prepareStatement(deactivateSql);
                    PreparedStatement activateStatement = connection.prepareStatement(activateSql)) {

                deactivateStatement.executeUpdate();
                activateStatement.setInt(1, schoolYearId);

                if (activateStatement.executeUpdate() == 0) {
                    connection.rollback();
                    lastErrorMessage = "Ann\u00e9e scolaire introuvable.";
                    return false;
                }

                connection.commit();
                lastErrorMessage = "";
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public List<SchoolYear> listAllSchoolYears() {
        List<SchoolYear> schoolYears = new ArrayList<>();
        String sql = "SELECT * FROM school_years ORDER BY name DESC";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                schoolYears.add(mapSchoolYear(resultSet));
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return schoolYears;
    }

    public SchoolYear findActiveSchoolYear() {
        String sql = "SELECT * FROM school_years WHERE active = TRUE LIMIT 1";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                lastErrorMessage = "";
                return mapSchoolYear(resultSet);
            }
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return null;
    }

    public boolean existsByName(String name) {
        String sql = "SELECT id FROM school_years WHERE name = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return true;
        }
    }

    public String getLastErrorMessage() {
        if (lastErrorMessage == null || lastErrorMessage.isBlank()) {
            return "Une erreur est survenue pendant l'op\u00e9ration.";
        }

        return lastErrorMessage;
    }

    private SchoolYear mapSchoolYear(ResultSet resultSet) throws SQLException {
        SchoolYear schoolYear = new SchoolYear();
        schoolYear.setId(resultSet.getInt("id"));
        schoolYear.setName(resultSet.getString("name"));
        schoolYear.setStartDate(resultSet.getDate("start_date").toLocalDate());
        schoolYear.setEndDate(resultSet.getDate("end_date").toLocalDate());
        schoolYear.setActive(resultSet.getBoolean("active"));

        if (resultSet.getTimestamp("created_at") != null) {
            schoolYear.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        }

        if (resultSet.getTimestamp("updated_at") != null) {
            schoolYear.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        }

        return schoolYear;
    }

    private String createFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("duplicate")) {
            return "Cette ann\u00e9e scolaire existe d\u00e9j\u00e0.";
        }

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "La table des ann\u00e9es scolaires n'existe pas. Cliquez sur Initialiser la base.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }
}
