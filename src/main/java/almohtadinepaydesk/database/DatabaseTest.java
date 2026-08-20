package almohtadinepaydesk.database;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseTest {

    public static boolean testConnection() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private DatabaseTest() {
    }
}
