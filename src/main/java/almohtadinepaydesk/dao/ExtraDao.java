package almohtadinepaydesk.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.database.DatabaseConnection;
import almohtadinepaydesk.models.Extra;
import almohtadinepaydesk.models.ExtraType;

public class ExtraDao {

    private String lastErrorMessage = "";

    public boolean createExtra(Extra extra) {
        String sql = """
                INSERT INTO extras
                    (child_id, school_year_month_id, start_month_id, end_month_id,
                     label, extra_type, amount, extra_date, paid, notes, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, FALSE, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateExtrasTableIfNeeded(connection);

            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, extra.getChildId());
                statement.setInt(2, extra.getStartMonthId());
                statement.setInt(3, extra.getStartMonthId());
                setNullableInt(statement, 4, extra.getEndMonthId());
                statement.setString(5, extra.getLabel());
                statement.setString(6, extra.getExtraType().name());
                statement.setBigDecimal(7, extra.getAmount());
                statement.setDate(8, Date.valueOf(LocalDate.now()));
                statement.setString(9, extra.getNotes());
                statement.setBoolean(10, extra.isActive());

                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    lastErrorMessage = "Aucun frais suppl\u00e9mentaire n'a \u00e9t\u00e9 ajout\u00e9.";
                    return false;
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        extra.setId(generatedKeys.getInt(1));
                    }
                }

                lastErrorMessage = "";
                return true;
            }
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateExtra(Extra extra) {
        String sql = """
                UPDATE extras
                SET child_id = ?,
                    school_year_month_id = ?,
                    start_month_id = ?,
                    end_month_id = ?,
                    label = ?,
                    extra_type = ?,
                    amount = ?,
                    notes = ?,
                    active = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateExtrasTableIfNeeded(connection);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, extra.getChildId());
                statement.setInt(2, extra.getStartMonthId());
                statement.setInt(3, extra.getStartMonthId());
                setNullableInt(statement, 4, extra.getEndMonthId());
                statement.setString(5, extra.getLabel());
                statement.setString(6, extra.getExtraType().name());
                statement.setBigDecimal(7, extra.getAmount());
                statement.setString(8, extra.getNotes());
                statement.setBoolean(9, extra.isActive());
                statement.setInt(10, extra.getId());

                boolean updated = statement.executeUpdate() > 0;
                lastErrorMessage = updated ? "" : "Aucun frais suppl\u00e9mentaire n'a \u00e9t\u00e9 modifi\u00e9.";
                return updated;
            }
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean setActive(int extraId, boolean active) {
        String sql = "UPDATE extras SET active = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateExtrasTableIfNeeded(connection);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBoolean(1, active);
                statement.setInt(2, extraId);

                boolean updated = statement.executeUpdate() > 0;
                lastErrorMessage = updated ? "" : "Aucun frais suppl\u00e9mentaire n'a \u00e9t\u00e9 modifi\u00e9.";
                return updated;
            }
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public List<Extra> listAllExtras() {
        List<Extra> extras = new ArrayList<>();
        String sql = """
                SELECT e.*,
                       c.first_name,
                       c.last_name,
                       sy.id AS school_year_id,
                       sy.name AS school_year_name,
                       start_month.month_name AS start_month_name,
                       end_month.month_name AS end_month_name
                FROM extras e
                JOIN children c ON c.id = e.child_id
                LEFT JOIN school_year_months start_month
                    ON start_month.id = COALESCE(e.start_month_id, e.school_year_month_id)
                LEFT JOIN school_years sy ON sy.id = start_month.school_year_id
                LEFT JOIN school_year_months end_month ON end_month.id = e.end_month_id
                ORDER BY sy.name DESC, start_month.display_order, c.first_name, e.label
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateExtrasTableIfNeeded(connection);

            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    extras.add(mapExtra(resultSet));
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return extras;
    }

    public String getLastErrorMessage() {
        if (lastErrorMessage == null || lastErrorMessage.isBlank()) {
            return "Une erreur est survenue pendant l'op\u00e9ration.";
        }

        return lastErrorMessage;
    }

    private Extra mapExtra(ResultSet resultSet) throws SQLException {
        Extra extra = new Extra();
        extra.setId(resultSet.getInt("id"));
        extra.setChildId(resultSet.getInt("child_id"));
        extra.setSchoolYearMonthId(readNullableInt(resultSet, "school_year_month_id"));
        extra.setStartMonthId(readNullableInt(resultSet, "start_month_id"));
        extra.setEndMonthId(readNullableInt(resultSet, "end_month_id"));
        extra.setLabel(resultSet.getString("label"));
        extra.setExtraType(parseExtraType(resultSet.getString("extra_type")));
        extra.setAmount(resultSet.getBigDecimal("amount"));

        if (resultSet.getDate("extra_date") != null) {
            extra.setExtraDate(resultSet.getDate("extra_date").toLocalDate());
        }

        extra.setPaid(resultSet.getBoolean("paid"));
        extra.setNotes(resultSet.getString("notes"));
        extra.setActive(resultSet.getBoolean("active"));
        extra.setChildFullName(buildFullName(resultSet.getString("first_name"), resultSet.getString("last_name")));
        extra.setSchoolYearId(readNullableInt(resultSet, "school_year_id"));
        extra.setSchoolYearName(resultSet.getString("school_year_name"));
        extra.setStartMonthName(resultSet.getString("start_month_name"));
        extra.setEndMonthName(resultSet.getString("end_month_name"));

        if (resultSet.getTimestamp("created_at") != null) {
            extra.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        }

        if (resultSet.getTimestamp("updated_at") != null) {
            extra.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        }

        return extra;
    }

    private void updateExtrasTableIfNeeded(Connection connection) throws SQLException {
        addColumnIfMissing(
                connection,
                "start_month_id",
                "ALTER TABLE extras ADD COLUMN start_month_id INT NULL AFTER school_year_month_id");

        addColumnIfMissing(
                connection,
                "end_month_id",
                "ALTER TABLE extras ADD COLUMN end_month_id INT NULL AFTER start_month_id");

        addColumnIfMissing(
                connection,
                "extra_type",
                "ALTER TABLE extras ADD COLUMN extra_type VARCHAR(20) NOT NULL DEFAULT 'PONCTUEL' AFTER label");

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE extras
                    SET start_month_id = school_year_month_id
                    WHERE start_month_id IS NULL
                      AND school_year_month_id IS NOT NULL
                    """);

            statement.executeUpdate("""
                    UPDATE extras
                    SET extra_type = 'PONCTUEL'
                    WHERE extra_type IS NULL
                       OR extra_type = ''
                    """);
        }
    }

    private void addColumnIfMissing(Connection connection, String columnName, String alterSql) throws SQLException {
        if (!columnExists(connection, columnName)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(alterSql);
            }
        }
    }

    private boolean columnExists(Connection connection, String columnName) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS column_count
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'extras'
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, columnName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("column_count") > 0;
            }
        }
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private Integer readNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);

        if (resultSet.wasNull()) {
            return null;
        }

        return value;
    }

    private ExtraType parseExtraType(String value) {
        if (value == null || value.isBlank()) {
            return ExtraType.PONCTUEL;
        }

        if ("R\u00e9current".equalsIgnoreCase(value) || "RECURRENT".equalsIgnoreCase(value)) {
            return ExtraType.RECURRENT;
        }

        return ExtraType.PONCTUEL;
    }

    private String buildFullName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }

        return firstName + " " + lastName;
    }

    private String createFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "La table des frais suppl\u00e9mentaires n'existe pas. Cliquez sur Initialiser la base.";
        }

        if (message != null && message.toLowerCase().contains("unknown column")) {
            return "La table des frais suppl\u00e9mentaires n'est pas \u00e0 jour. Cliquez sur Initialiser la base.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }
}
