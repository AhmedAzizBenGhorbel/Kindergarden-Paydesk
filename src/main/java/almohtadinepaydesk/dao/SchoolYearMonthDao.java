package almohtadinepaydesk.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.database.DatabaseConnection;
import almohtadinepaydesk.models.SchoolYearMonth;

public class SchoolYearMonthDao {

    private static final String[][] DEFAULT_MONTHS = {
            {"Septembre", "9", "1"},
            {"Octobre", "10", "2"},
            {"Novembre", "11", "3"},
            {"D\u00e9cembre", "12", "4"},
            {"Janvier", "1", "5"},
            {"F\u00e9vrier", "2", "6"},
            {"Mars", "3", "7"},
            {"Avril", "4", "8"},
            {"Mai", "5", "9"},
            {"Juin", "6", "10"}
    };

    private String lastErrorMessage = "";

    public void createDefaultMonthsIfMissing(int schoolYearId) {
        String sql = """
                INSERT INTO school_year_months
                    (school_year_id, month_name, month_number, display_order, active)
                VALUES (?, ?, ?, ?, TRUE)
                ON DUPLICATE KEY UPDATE
                    month_number = VALUES(month_number),
                    display_order = VALUES(display_order)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            for (String[] month : DEFAULT_MONTHS) {
                statement.setInt(1, schoolYearId);
                statement.setString(2, month[0]);
                statement.setInt(3, Integer.parseInt(month[1]));
                statement.setInt(4, Integer.parseInt(month[2]));
                statement.addBatch();
            }

            statement.executeBatch();
            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }
    }

    public List<SchoolYearMonth> listBySchoolYearId(int schoolYearId) {
        createDefaultMonthsIfMissing(schoolYearId);

        List<SchoolYearMonth> months = new ArrayList<>();
        String sql = """
                SELECT *
                FROM school_year_months
                WHERE school_year_id = ?
                ORDER BY display_order
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, schoolYearId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    months.add(mapSchoolYearMonth(resultSet));
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return months;
    }

    public boolean updateActiveMonths(List<SchoolYearMonth> months) {
        String sql = "UPDATE school_year_months SET active = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            for (SchoolYearMonth month : months) {
                statement.setBoolean(1, month.isActive());
                statement.setInt(2, month.getId());
                statement.addBatch();
            }

            statement.executeBatch();
            lastErrorMessage = "";
            return true;
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

    private SchoolYearMonth mapSchoolYearMonth(ResultSet resultSet) throws SQLException {
        SchoolYearMonth month = new SchoolYearMonth();
        month.setId(resultSet.getInt("id"));
        month.setSchoolYearId(resultSet.getInt("school_year_id"));
        month.setMonthName(resultSet.getString("month_name"));
        month.setMonthNumber(resultSet.getInt("month_number"));
        month.setDisplayOrder(resultSet.getInt("display_order"));
        month.setActive(resultSet.getBoolean("active"));

        if (resultSet.getTimestamp("created_at") != null) {
            month.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        }

        if (resultSet.getTimestamp("updated_at") != null) {
            month.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        }

        return month;
    }

    private String createFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "La table des mois actifs n'existe pas. Cliquez sur Initialiser la base.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }
}
