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
import almohtadinepaydesk.models.Child;

public class ChildDao {

    private String lastErrorMessage = "";

    public boolean createChild(Child child) {
        String sql = """
                INSERT INTO children
                    (first_name, last_name, class_group, parent_full_name, parent_phone,
                     registration_date, monthly_fee, notes, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateChildrenTableIfNeeded(connection);

            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(1, child.getFullName());
                statement.setString(2, "");
                statement.setString(3, child.getClassGroup());
                statement.setString(4, child.getParentFullName());
                statement.setString(5, child.getParentPhone());
                statement.setDate(6, Date.valueOf(child.getRegistrationDate()));
                statement.setBigDecimal(7, child.getMonthlyFee());
                statement.setString(8, child.getNotes());
                statement.setBoolean(9, child.isActive());

                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    lastErrorMessage = "Aucune ligne n'a \u00e9t\u00e9 ajout\u00e9e.";
                    return false;
                }

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        child.setId(generatedKeys.getInt(1));
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

    public boolean updateChild(Child child) {
        String sql = """
                UPDATE children
                SET first_name = ?,
                    last_name = ?,
                    class_group = ?,
                    parent_full_name = ?,
                    parent_phone = ?,
                    registration_date = ?,
                    monthly_fee = ?,
                    notes = ?,
                    active = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateChildrenTableIfNeeded(connection);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, child.getFullName());
                statement.setString(2, "");
                statement.setString(3, child.getClassGroup());
                statement.setString(4, child.getParentFullName());
                statement.setString(5, child.getParentPhone());
                statement.setDate(6, Date.valueOf(child.getRegistrationDate()));
                statement.setBigDecimal(7, child.getMonthlyFee());
                statement.setString(8, child.getNotes());
                statement.setBoolean(9, child.isActive());
                statement.setInt(10, child.getId());

                boolean updated = statement.executeUpdate() > 0;
                lastErrorMessage = updated ? "" : "Aucune ligne n'a \u00e9t\u00e9 modifi\u00e9e.";
                return updated;
            }
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean setActive(int childId, boolean active) {
        String sql = "UPDATE children SET active = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBoolean(1, active);
            statement.setInt(2, childId);
            boolean updated = statement.executeUpdate() > 0;
            lastErrorMessage = updated ? "" : "Aucune ligne n'a \u00e9t\u00e9 modifi\u00e9e.";
            return updated;
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public List<Child> listAllChildren() {
        List<Child> children = new ArrayList<>();
        String sql = "SELECT * FROM children ORDER BY first_name";

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateChildrenTableIfNeeded(connection);

            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    children.add(mapChild(resultSet));
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return children;
    }

    public String getLastErrorMessage() {
        if (lastErrorMessage == null || lastErrorMessage.isBlank()) {
            return "Une erreur est survenue pendant l'op\u00e9ration.";
        }

        return lastErrorMessage;
    }

    private void updateChildrenTableIfNeeded(Connection connection) throws SQLException {
        addColumnIfMissing(
                connection,
                "class_group",
                "ALTER TABLE children ADD COLUMN class_group VARCHAR(80) NULL AFTER last_name");

        addColumnIfMissing(
                connection,
                "registration_date",
                "ALTER TABLE children ADD COLUMN registration_date DATE NULL AFTER parent_phone");

        addColumnIfMissing(
                connection,
                "monthly_fee",
                "ALTER TABLE children ADD COLUMN monthly_fee DECIMAL(10,3) NOT NULL DEFAULT 0.000 AFTER registration_date");
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
                  AND TABLE_NAME = 'children'
                  AND COLUMN_NAME = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, columnName);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt("column_count") > 0;
            }
        }
    }

    private String createFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("unknown column")) {
            return "La table des enfants n'est pas \u00e0 jour. Cliquez sur Initialiser la base, puis r\u00e9essayez.";
        }

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "La table des enfants n'existe pas. Cliquez sur Initialiser la base, puis r\u00e9essayez.";
        }

        if (message != null && message.toLowerCase().contains("access denied")) {
            return "Connexion MySQL refus\u00e9e. V\u00e9rifiez l'utilisateur et le mot de passe.";
        }

        if (message != null && message.toLowerCase().contains("communications link failure")) {
            return "MySQL ne r\u00e9pond pas. V\u00e9rifiez que le serveur MySQL est d\u00e9marr\u00e9.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }

    private Child mapChild(ResultSet resultSet) throws SQLException {
        Child child = new Child();
        child.setId(resultSet.getInt("id"));
        child.setFirstName(resultSet.getString("first_name"));
        child.setLastName(resultSet.getString("last_name"));
        child.setClassGroup(resultSet.getString("class_group"));
        child.setParentFullName(resultSet.getString("parent_full_name"));
        child.setParentPhone(resultSet.getString("parent_phone"));

        if (resultSet.getDate("birth_date") != null) {
            child.setBirthDate(resultSet.getDate("birth_date").toLocalDate());
        }

        if (resultSet.getDate("registration_date") != null) {
            child.setRegistrationDate(resultSet.getDate("registration_date").toLocalDate());
        }

        child.setMonthlyFee(resultSet.getBigDecimal("monthly_fee"));
        child.setNotes(resultSet.getString("notes"));
        child.setActive(resultSet.getBoolean("active"));

        if (resultSet.getTimestamp("created_at") != null) {
            child.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        }

        if (resultSet.getTimestamp("updated_at") != null) {
            child.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        }

        return child;
    }
}
