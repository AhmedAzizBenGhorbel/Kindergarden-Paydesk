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
import almohtadinepaydesk.models.PaymentEntry;
import almohtadinepaydesk.models.PaymentMethod;

public class PaymentEntryDao {

    private static final String INSERT_SQL = """
            INSERT INTO payment_entries
                (monthly_record_id, extra_id, amount, payment_date, payment_method, notes, created_by_user_id, active)
            VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)
            """;

    private String lastErrorMessage = "";

    public boolean createPaymentEntry(PaymentEntry paymentEntry) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return createPaymentEntry(connection, paymentEntry);
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean createPaymentEntry(Connection connection, PaymentEntry paymentEntry) {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setNullableInt(statement, 1, paymentEntry.getMonthlyRecordId());
            setNullableInt(statement, 2, paymentEntry.getExtraId());
            statement.setBigDecimal(3, paymentEntry.getAmount());
            statement.setDate(4, Date.valueOf(paymentEntry.getPaymentDate()));
            statement.setString(5, paymentEntry.getPaymentMethod().name());
            statement.setString(6, paymentEntry.getNotes());
            setNullableInt(statement, 7, paymentEntry.getCreatedByUserId());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                lastErrorMessage = "Aucun paiement n'a \u00e9t\u00e9 ajout\u00e9.";
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    paymentEntry.setId(generatedKeys.getInt(1));
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

    public List<PaymentEntry> listByMonthlyRecordId(int monthlyRecordId) {
        List<PaymentEntry> paymentEntries = new ArrayList<>();
        String sql = """
                SELECT pe.*,
                       r.receipt_number
                FROM payment_entries pe
                LEFT JOIN receipts r ON r.payment_entry_id = pe.id
                WHERE pe.monthly_record_id = ?
                  AND pe.active = TRUE
                ORDER BY pe.payment_date, pe.id
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, monthlyRecordId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    paymentEntries.add(mapPaymentEntry(resultSet));
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return paymentEntries;
    }

    public String getLastErrorMessage() {
        if (lastErrorMessage == null || lastErrorMessage.isBlank()) {
            return "Une erreur est survenue pendant l'op\u00e9ration.";
        }

        return lastErrorMessage;
    }

    private PaymentEntry mapPaymentEntry(ResultSet resultSet) throws SQLException {
        PaymentEntry paymentEntry = new PaymentEntry();
        paymentEntry.setId(resultSet.getInt("id"));
        paymentEntry.setMonthlyRecordId(readNullableInt(resultSet, "monthly_record_id"));
        paymentEntry.setExtraId(readNullableInt(resultSet, "extra_id"));
        paymentEntry.setAmount(resultSet.getBigDecimal("amount"));

        if (resultSet.getDate("payment_date") != null) {
            paymentEntry.setPaymentDate(resultSet.getDate("payment_date").toLocalDate());
        }

        paymentEntry.setPaymentMethod(PaymentMethod.fromLabelOrName(resultSet.getString("payment_method")));
        paymentEntry.setNotes(resultSet.getString("notes"));
        paymentEntry.setCreatedByUserId(readNullableInt(resultSet, "created_by_user_id"));
        paymentEntry.setActive(resultSet.getBoolean("active"));
        paymentEntry.setReceiptNumber(resultSet.getString("receipt_number"));

        if (resultSet.getTimestamp("created_at") != null) {
            paymentEntry.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        }

        if (resultSet.getTimestamp("updated_at") != null) {
            paymentEntry.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        }

        return paymentEntry;
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

    private String createFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "La table des paiements n'existe pas. Cliquez sur Initialiser la base.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }
}
