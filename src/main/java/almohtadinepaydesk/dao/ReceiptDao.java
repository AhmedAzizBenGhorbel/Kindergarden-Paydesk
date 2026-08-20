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
import almohtadinepaydesk.models.PaymentMethod;
import almohtadinepaydesk.models.Receipt;

public class ReceiptDao {

    private static final String INSERT_SQL = """
            INSERT INTO receipts
                (receipt_number, child_id, payment_entry_id, receipt_date, total_amount, payer_name, notes, active)
            VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)
            """;

    private String lastErrorMessage = "";

    public boolean createReceipt(Receipt receipt) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return createReceipt(connection, receipt);
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean createReceipt(Connection connection, Receipt receipt) {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, receipt.getReceiptNumber());
            statement.setInt(2, receipt.getChildId());
            setNullableInt(statement, 3, receipt.getPaymentEntryId());
            statement.setDate(4, Date.valueOf(receipt.getReceiptDate()));
            statement.setBigDecimal(5, receipt.getTotalAmount());
            statement.setString(6, receipt.getPayerName());
            statement.setString(7, receipt.getNotes());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                lastErrorMessage = "Aucun re\u00e7u n'a \u00e9t\u00e9 enregistr\u00e9.";
                return false;
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    receipt.setId(generatedKeys.getInt(1));
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

    public String findLastReceiptNumberForYear(int year) {
        String prefix = "ADP-" + year + "-";
        String sql = """
                SELECT receipt_number
                FROM receipts
                WHERE receipt_number LIKE ?
                ORDER BY receipt_number DESC
                LIMIT 1
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, prefix + "%");

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    lastErrorMessage = "";
                    return resultSet.getString("receipt_number");
                }
            }
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return null;
    }

    public List<Receipt> listReceipts(
            String searchText,
            Integer schoolYearId,
            Integer monthId,
            PaymentMethod paymentMethod) {

        List<Receipt> receipts = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
                SELECT r.*,
                       TRIM(CONCAT(COALESCE(c.first_name, ''), ' ', COALESCE(c.last_name, ''))) AS child_full_name,
                       COALESCE(c.parent_full_name, r.payer_name) AS parent_name,
                       COALESCE(pe.payment_date, r.receipt_date) AS payment_date,
                       pe.payment_method,
                       sy.name AS school_year_name,
                       sym.month_name,
                       CASE
                           WHEN u.full_name IS NOT NULL AND u.full_name <> '' THEN u.full_name
                           ELSE u.username
                       END AS created_by_name,
                       COALESCE(r.notes, pe.notes) AS receipt_note
                FROM receipts r
                JOIN children c ON c.id = r.child_id
                LEFT JOIN payment_entries pe ON pe.id = r.payment_entry_id
                LEFT JOIN monthly_records mr ON mr.id = pe.monthly_record_id
                LEFT JOIN school_year_months sym ON sym.id = mr.school_year_month_id
                LEFT JOIN school_years sy ON sy.id = sym.school_year_id
                LEFT JOIN users u ON u.id = pe.created_by_user_id
                WHERE r.active = TRUE
                """);

        if (searchText != null && !searchText.trim().isEmpty()) {
            String searchPattern = "%" + searchText.trim() + "%";
            sql.append("""
                     AND (
                            r.receipt_number LIKE ?
                            OR TRIM(CONCAT(COALESCE(c.first_name, ''), ' ', COALESCE(c.last_name, ''))) LIKE ?
                         )
                    """);
            parameters.add(searchPattern);
            parameters.add(searchPattern);
        }

        if (schoolYearId != null) {
            sql.append(" AND sy.id = ?");
            parameters.add(schoolYearId);
        }

        if (monthId != null) {
            sql.append(" AND sym.id = ?");
            parameters.add(monthId);
        }

        if (paymentMethod != null) {
            sql.append(" AND (pe.payment_method = ? OR pe.payment_method = ?)");
            parameters.add(paymentMethod.name());
            parameters.add(paymentMethod.getLabel());
        }

        sql.append(" ORDER BY COALESCE(pe.payment_date, r.receipt_date) DESC, r.id DESC");

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            fillParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    receipts.add(mapReceipt(resultSet));
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return receipts;
    }

    public String getLastErrorMessage() {
        if (lastErrorMessage == null || lastErrorMessage.isBlank()) {
            return "Une erreur est survenue pendant l'op\u00e9ration.";
        }

        return lastErrorMessage;
    }

    private Receipt mapReceipt(ResultSet resultSet) throws SQLException {
        Receipt receipt = new Receipt();
        receipt.setId(resultSet.getInt("id"));
        receipt.setReceiptNumber(resultSet.getString("receipt_number"));
        receipt.setChildId(resultSet.getInt("child_id"));
        receipt.setPaymentEntryId(readNullableInt(resultSet, "payment_entry_id"));

        if (resultSet.getDate("payment_date") != null) {
            receipt.setReceiptDate(resultSet.getDate("payment_date").toLocalDate());
        }

        receipt.setTotalAmount(resultSet.getBigDecimal("total_amount"));
        receipt.setPayerName(resultSet.getString("payer_name"));
        receipt.setNotes(resultSet.getString("receipt_note"));
        receipt.setActive(resultSet.getBoolean("active"));
        receipt.setChildFullName(resultSet.getString("child_full_name"));
        receipt.setParentFullName(resultSet.getString("parent_name"));
        String paymentMethodText = resultSet.getString("payment_method");
        if (paymentMethodText != null && !paymentMethodText.isBlank()) {
            receipt.setPaymentMethod(PaymentMethod.fromLabelOrName(paymentMethodText));
        }
        receipt.setSchoolYearName(resultSet.getString("school_year_name"));
        receipt.setMonthName(resultSet.getString("month_name"));
        receipt.setCreatedByFullName(resultSet.getString("created_by_name"));

        if (resultSet.getTimestamp("created_at") != null) {
            receipt.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        }

        if (resultSet.getTimestamp("updated_at") != null) {
            receipt.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        }

        return receipt;
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

    private void fillParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);

            if (value instanceof Integer integerValue) {
                statement.setInt(i + 1, integerValue);
            } else {
                statement.setString(i + 1, String.valueOf(value));
            }
        }
    }

    private String createFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("duplicate")) {
            return "Ce num\u00e9ro de re\u00e7u existe d\u00e9j\u00e0.";
        }

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "La table des re\u00e7us n'existe pas. Cliquez sur Initialiser la base.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }
}
