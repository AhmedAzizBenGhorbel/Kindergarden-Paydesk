package almohtadinepaydesk.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.database.DatabaseConnection;
import almohtadinepaydesk.models.MonthlyRecord;
import almohtadinepaydesk.models.PaymentStatus;
import almohtadinepaydesk.models.SchoolYear;
import almohtadinepaydesk.models.SchoolYearMonth;
import almohtadinepaydesk.services.PaymentCalculationService;
import almohtadinepaydesk.services.PaymentStatusService;

public class MonthlyRecordDao {

    private final PaymentCalculationService calculationService = new PaymentCalculationService();
    private final PaymentStatusService statusService = new PaymentStatusService();
    private String lastErrorMessage = "";

    public int generateMonthlyRecordsForActiveSchoolYear() {
        SchoolYear activeSchoolYear = new SchoolYearDao().findActiveSchoolYear();

        if (activeSchoolYear == null) {
            lastErrorMessage = "Aucune ann\u00e9e scolaire active trouv\u00e9e.";
            return -1;
        }

        String activeMonthsSql = """
                SELECT *
                FROM school_year_months
                WHERE school_year_id = ?
                  AND active = TRUE
                ORDER BY display_order
                """;

        String activeChildrenSql = """
                SELECT id, monthly_fee
                FROM children
                WHERE active = TRUE
                ORDER BY first_name
                """;

        String insertSql = """
                INSERT IGNORE INTO monthly_records
                    (child_id, school_year_month_id, expected_amount, total_paid, payment_status, due_date, notes, active)
                VALUES (?, ?, ?, ?, ?, ?, NULL, TRUE)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateChildrenTableIfNeeded(connection);

            int deadlineDay = getPaymentDeadlineDay(connection);
            List<SchoolYearMonth> activeMonths = loadActiveMonths(connection, activeMonthsSql, activeSchoolYear.getId());

            if (activeMonths.isEmpty()) {
                lastErrorMessage = "Aucun mois actif trouv\u00e9 pour l'ann\u00e9e scolaire active.";
                return -1;
            }

            int createdCount = 0;

            try (PreparedStatement childrenStatement = connection.prepareStatement(activeChildrenSql);
                    ResultSet childrenResultSet = childrenStatement.executeQuery();
                    PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {

                while (childrenResultSet.next()) {
                    int childId = childrenResultSet.getInt("id");
                    BigDecimal baseMonthlyFee = calculationService.cleanAmount(childrenResultSet.getBigDecimal("monthly_fee"));

                    for (SchoolYearMonth month : activeMonths) {
                        BigDecimal totalExtras = getTotalExtras(connection, childId, month.getId());
                        BigDecimal totalExpected = calculationService.calculateTotalExpected(baseMonthlyFee, totalExtras);
                        BigDecimal totalPaid = BigDecimal.ZERO;
                        LocalDate deadlineDate = calculateDeadlineDate(activeSchoolYear, month, deadlineDay);
                        PaymentStatus status = statusService.calculateStatus(totalPaid, totalExpected, deadlineDate);

                        insertStatement.setInt(1, childId);
                        insertStatement.setInt(2, month.getId());
                        insertStatement.setBigDecimal(3, totalExpected);
                        insertStatement.setBigDecimal(4, totalPaid);
                        insertStatement.setString(5, status.getLabel());
                        insertStatement.setDate(6, Date.valueOf(deadlineDate));
                        createdCount += insertStatement.executeUpdate();
                    }
                }
            }

            recalculateMonthlyRecordsForSchoolYear(activeSchoolYear.getId());
            lastErrorMessage = "";
            return createdCount;
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return -1;
        }
    }

    public int recalculateAllMonthlyRecords() {
        return recalculateMonthlyRecordsForSchoolYear(null);
    }

    public int recalculateMonthlyRecordsForSchoolYear(Integer schoolYearId) {
        StringBuilder selectSql = new StringBuilder("""
                SELECT mr.id,
                       mr.child_id,
                       mr.school_year_month_id,
                       mr.due_date,
                       c.monthly_fee,
                       sy.start_date,
                       sy.end_date,
                       sym.month_number,
                       sym.display_order
                FROM monthly_records mr
                JOIN children c ON c.id = mr.child_id
                JOIN school_year_months sym ON sym.id = mr.school_year_month_id
                JOIN school_years sy ON sy.id = sym.school_year_id
                WHERE mr.active = TRUE
                """);

        if (schoolYearId != null) {
            selectSql.append(" AND sy.id = ?");
        }

        String updateSql = """
                UPDATE monthly_records
                SET expected_amount = ?,
                    total_paid = ?,
                    payment_status = ?,
                    due_date = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement selectStatement = connection.prepareStatement(selectSql.toString());
                PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {

            updateChildrenTableIfNeeded(connection);

            if (schoolYearId != null) {
                selectStatement.setInt(1, schoolYearId);
            }

            int deadlineDay = getPaymentDeadlineDay(connection);
            int updatedCount = 0;

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                while (resultSet.next()) {
                    int recordId = resultSet.getInt("id");
                    int childId = resultSet.getInt("child_id");
                    int monthId = resultSet.getInt("school_year_month_id");

                    BigDecimal baseMonthlyFee = calculationService.cleanAmount(resultSet.getBigDecimal("monthly_fee"));
                    BigDecimal totalExtras = getTotalExtras(connection, childId, monthId);
                    BigDecimal totalExpected = calculationService.calculateTotalExpected(baseMonthlyFee, totalExtras);
                    BigDecimal totalPaid = getTotalPaid(connection, recordId);

                    SchoolYear schoolYear = new SchoolYear();
                    schoolYear.setStartDate(resultSet.getDate("start_date").toLocalDate());
                    schoolYear.setEndDate(resultSet.getDate("end_date").toLocalDate());

                    SchoolYearMonth month = new SchoolYearMonth();
                    month.setMonthNumber(resultSet.getInt("month_number"));
                    month.setDisplayOrder(resultSet.getInt("display_order"));

                    LocalDate deadlineDate = calculateDeadlineDate(schoolYear, month, deadlineDay);
                    PaymentStatus status = statusService.calculateStatus(totalPaid, totalExpected, deadlineDate);

                    updateStatement.setBigDecimal(1, totalExpected);
                    updateStatement.setBigDecimal(2, totalPaid);
                    updateStatement.setString(3, status.getLabel());
                    updateStatement.setDate(4, Date.valueOf(deadlineDate));
                    updateStatement.setInt(5, recordId);
                    updatedCount += updateStatement.executeUpdate();
                }
            }

            lastErrorMessage = "";
            return updatedCount;
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
            return -1;
        }
    }

    public List<MonthlyRecord> listMonthlyRecords(
            Integer schoolYearId,
            Integer monthId,
            String classGroup,
            PaymentStatus status) {

        List<MonthlyRecord> records = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT mr.*,
                       c.first_name,
                       c.last_name,
                       c.parent_full_name,
                       c.class_group,
                       c.monthly_fee,
                       sy.name AS school_year_name,
                       sym.month_name,
                       COALESCE((
                            SELECT SUM(e.amount)
                            FROM extras e
                            LEFT JOIN school_year_months extra_start_month
                                ON extra_start_month.id = COALESCE(e.start_month_id, e.school_year_month_id)
                            LEFT JOIN school_year_months extra_end_month
                                ON extra_end_month.id = e.end_month_id
                            WHERE e.child_id = mr.child_id
                              AND e.active = TRUE
                              AND (
                                    (
                                        COALESCE(e.extra_type, 'PONCTUEL') = 'PONCTUEL'
                                        AND COALESCE(e.start_month_id, e.school_year_month_id) = mr.school_year_month_id
                                    )
                                    OR
                                    (
                                        COALESCE(e.extra_type, 'PONCTUEL') = 'RECURRENT'
                                        AND extra_start_month.school_year_id = sym.school_year_id
                                        AND extra_start_month.display_order <= sym.display_order
                                        AND (
                                            e.end_month_id IS NULL
                                            OR extra_end_month.display_order >= sym.display_order
                                        )
                                    )
                              )
                       ), 0) AS total_extras,
                       COALESCE((
                            SELECT SUM(pe.amount)
                            FROM payment_entries pe
                            WHERE pe.monthly_record_id = mr.id
                              AND pe.active = TRUE
                       ), 0) AS calculated_paid
                FROM monthly_records mr
                JOIN children c ON c.id = mr.child_id
                JOIN school_year_months sym ON sym.id = mr.school_year_month_id
                JOIN school_years sy ON sy.id = sym.school_year_id
                WHERE mr.active = TRUE
                """);

        List<Object> parameters = new ArrayList<>();

        if (schoolYearId != null) {
            sql.append(" AND sy.id = ?");
            parameters.add(schoolYearId);
        }

        if (monthId != null) {
            sql.append(" AND sym.id = ?");
            parameters.add(monthId);
        }

        if (classGroup != null && !classGroup.isBlank()) {
            sql.append(" AND c.class_group = ?");
            parameters.add(classGroup);
        }

        if (status != null) {
            sql.append(" AND mr.payment_status = ?");
            parameters.add(status.getLabel());
        }

        sql.append(" ORDER BY sy.name DESC, sym.display_order, c.first_name");

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateChildrenTableIfNeeded(connection);
            updateExtrasTableIfNeeded(connection);

            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                fillParameters(statement, parameters);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        records.add(mapMonthlyRecord(resultSet));
                    }
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return records;
    }

    public MonthlyRecord findDetailedById(int monthlyRecordId) {
        String sql = """
                SELECT mr.*,
                       c.first_name,
                       c.last_name,
                       c.parent_full_name,
                       c.class_group,
                       c.monthly_fee,
                       sy.name AS school_year_name,
                       sym.month_name,
                       COALESCE((
                            SELECT SUM(e.amount)
                            FROM extras e
                            LEFT JOIN school_year_months extra_start_month
                                ON extra_start_month.id = COALESCE(e.start_month_id, e.school_year_month_id)
                            LEFT JOIN school_year_months extra_end_month
                                ON extra_end_month.id = e.end_month_id
                            WHERE e.child_id = mr.child_id
                              AND e.active = TRUE
                              AND (
                                    (
                                        COALESCE(e.extra_type, 'PONCTUEL') = 'PONCTUEL'
                                        AND COALESCE(e.start_month_id, e.school_year_month_id) = mr.school_year_month_id
                                    )
                                    OR
                                    (
                                        COALESCE(e.extra_type, 'PONCTUEL') = 'RECURRENT'
                                        AND extra_start_month.school_year_id = sym.school_year_id
                                        AND extra_start_month.display_order <= sym.display_order
                                        AND (
                                            e.end_month_id IS NULL
                                            OR extra_end_month.display_order >= sym.display_order
                                        )
                                    )
                              )
                       ), 0) AS total_extras,
                       COALESCE((
                            SELECT SUM(pe.amount)
                            FROM payment_entries pe
                            WHERE pe.monthly_record_id = mr.id
                              AND pe.active = TRUE
                       ), 0) AS calculated_paid
                FROM monthly_records mr
                JOIN children c ON c.id = mr.child_id
                JOIN school_year_months sym ON sym.id = mr.school_year_month_id
                JOIN school_years sy ON sy.id = sym.school_year_id
                WHERE mr.id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateChildrenTableIfNeeded(connection);
            updateExtrasTableIfNeeded(connection);

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, monthlyRecordId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        lastErrorMessage = "";
                        return mapMonthlyRecord(resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return null;
    }

    public List<String> listClassGroups() {
        List<String> classGroups = new ArrayList<>();
        String sql = """
                SELECT DISTINCT class_group
                FROM children
                WHERE class_group IS NOT NULL
                  AND class_group <> ''
                ORDER BY class_group
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            updateChildrenTableIfNeeded(connection);

            try (PreparedStatement statement = connection.prepareStatement(sql);
                    ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    classGroups.add(resultSet.getString("class_group"));
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return classGroups;
    }

    public String getLastErrorMessage() {
        if (lastErrorMessage == null || lastErrorMessage.isBlank()) {
            return "Une erreur est survenue pendant l'op\u00e9ration.";
        }

        return lastErrorMessage;
    }

    private void updateExtrasTableIfNeeded(Connection connection) throws SQLException {
        addExtraColumnIfMissing(
                connection,
                "start_month_id",
                "ALTER TABLE extras ADD COLUMN start_month_id INT NULL AFTER school_year_month_id");

        addExtraColumnIfMissing(
                connection,
                "end_month_id",
                "ALTER TABLE extras ADD COLUMN end_month_id INT NULL AFTER start_month_id");

        addExtraColumnIfMissing(
                connection,
                "extra_type",
                "ALTER TABLE extras ADD COLUMN extra_type VARCHAR(20) NOT NULL DEFAULT 'PONCTUEL' AFTER label");

        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE extras
                SET start_month_id = school_year_month_id
                WHERE start_month_id IS NULL
                  AND school_year_month_id IS NOT NULL
                """)) {
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE extras
                SET extra_type = 'PONCTUEL'
                WHERE extra_type IS NULL
                   OR extra_type = ''
                """)) {
            statement.executeUpdate();
        }
    }

    private void addExtraColumnIfMissing(Connection connection, String columnName, String alterSql) throws SQLException {
        if (!extraColumnExists(connection, columnName)) {
            try (PreparedStatement statement = connection.prepareStatement(alterSql)) {
                statement.execute();
            }
        }
    }

    private boolean extraColumnExists(Connection connection, String columnName) throws SQLException {
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

    private void updateChildrenTableIfNeeded(Connection connection) throws SQLException {
        addChildColumnIfMissing(
                connection,
                "class_group",
                "ALTER TABLE children ADD COLUMN class_group VARCHAR(80) NULL AFTER last_name");

        addChildColumnIfMissing(
                connection,
                "registration_date",
                "ALTER TABLE children ADD COLUMN registration_date DATE NULL AFTER parent_phone");

        addChildColumnIfMissing(
                connection,
                "monthly_fee",
                "ALTER TABLE children ADD COLUMN monthly_fee DECIMAL(10,3) NOT NULL DEFAULT 0.000 AFTER registration_date");
    }

    private void addChildColumnIfMissing(Connection connection, String columnName, String alterSql) throws SQLException {
        if (!childColumnExists(connection, columnName)) {
            try (PreparedStatement statement = connection.prepareStatement(alterSql)) {
                statement.execute();
            }
        }
    }

    private boolean childColumnExists(Connection connection, String columnName) throws SQLException {
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

    private List<SchoolYearMonth> loadActiveMonths(Connection connection, String sql, int schoolYearId)
            throws SQLException {
        List<SchoolYearMonth> months = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, schoolYearId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    SchoolYearMonth month = new SchoolYearMonth();
                    month.setId(resultSet.getInt("id"));
                    month.setSchoolYearId(resultSet.getInt("school_year_id"));
                    month.setMonthName(resultSet.getString("month_name"));
                    month.setMonthNumber(resultSet.getInt("month_number"));
                    month.setDisplayOrder(resultSet.getInt("display_order"));
                    month.setActive(resultSet.getBoolean("active"));
                    months.add(month);
                }
            }
        }

        return months;
    }

    private MonthlyRecord mapMonthlyRecord(ResultSet resultSet) throws SQLException {
        MonthlyRecord record = new MonthlyRecord();
        record.setId(resultSet.getInt("id"));
        record.setChildId(resultSet.getInt("child_id"));
        record.setSchoolYearMonthId(resultSet.getInt("school_year_month_id"));
        record.setExpectedAmount(calculationService.cleanAmount(resultSet.getBigDecimal("expected_amount")));
        record.setTotalPaid(calculationService.cleanAmount(resultSet.getBigDecimal("calculated_paid")));
        record.setPaymentStatus(PaymentStatus.fromLabel(resultSet.getString("payment_status")));
        if (resultSet.getDate("due_date") != null) {
            record.setDueDate(resultSet.getDate("due_date").toLocalDate());
        }
        record.setNotes(resultSet.getString("notes"));
        record.setActive(resultSet.getBoolean("active"));

        String firstName = resultSet.getString("first_name");
        String lastName = resultSet.getString("last_name");
        record.setChildFullName(buildFullName(firstName, lastName));
        record.setParentFullName(resultSet.getString("parent_full_name"));
        record.setClassGroup(resultSet.getString("class_group"));
        record.setSchoolYearName(resultSet.getString("school_year_name"));
        record.setMonthName(resultSet.getString("month_name"));

        BigDecimal baseMonthlyFee = calculationService.cleanAmount(resultSet.getBigDecimal("monthly_fee"));
        BigDecimal totalExtras = calculationService.cleanAmount(resultSet.getBigDecimal("total_extras"));
        BigDecimal totalExpected = calculationService.calculateTotalExpected(baseMonthlyFee, totalExtras);

        record.setBaseMonthlyFee(baseMonthlyFee);
        record.setTotalExtras(totalExtras);
        record.setTotalExpected(totalExpected);
        record.setRemainingAmount(calculationService.calculateRemainingAmount(totalExpected, record.getTotalPaid()));
        record.setAdvanceAmount(calculationService.calculateAdvanceAmount(totalExpected, record.getTotalPaid()));

        if (resultSet.getTimestamp("created_at") != null) {
            record.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        }

        if (resultSet.getTimestamp("updated_at") != null) {
            record.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        }

        return record;
    }

    private String buildFullName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }

        return firstName + " " + lastName;
    }

    private BigDecimal getTotalExtras(Connection connection, int childId, int monthId) throws SQLException {
        updateExtrasTableIfNeeded(connection);

        String sql = """
                SELECT COALESCE(SUM(amount), 0) AS total_extras
                FROM extras e
                JOIN school_year_months target_month ON target_month.id = ?
                LEFT JOIN school_year_months extra_start_month
                    ON extra_start_month.id = COALESCE(e.start_month_id, e.school_year_month_id)
                LEFT JOIN school_year_months extra_end_month
                    ON extra_end_month.id = e.end_month_id
                WHERE e.child_id = ?
                  AND e.active = TRUE
                  AND (
                        (
                            COALESCE(e.extra_type, 'PONCTUEL') = 'PONCTUEL'
                            AND COALESCE(e.start_month_id, e.school_year_month_id) = target_month.id
                        )
                        OR
                        (
                            COALESCE(e.extra_type, 'PONCTUEL') = 'RECURRENT'
                            AND extra_start_month.school_year_id = target_month.school_year_id
                            AND extra_start_month.display_order <= target_month.display_order
                            AND (
                                e.end_month_id IS NULL
                                OR extra_end_month.display_order >= target_month.display_order
                            )
                        )
                  )
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, monthId);
            statement.setInt(2, childId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return calculationService.cleanAmount(resultSet.getBigDecimal("total_extras"));
                }
            }
        }

        return calculationService.cleanAmount(BigDecimal.ZERO);
    }

    private BigDecimal getTotalPaid(Connection connection, int monthlyRecordId) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) AS total_paid
                FROM payment_entries
                WHERE monthly_record_id = ?
                  AND active = TRUE
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, monthlyRecordId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return calculationService.cleanAmount(resultSet.getBigDecimal("total_paid"));
                }
            }
        }

        return calculationService.cleanAmount(BigDecimal.ZERO);
    }

    private int getPaymentDeadlineDay(Connection connection) throws SQLException {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = 'payment_deadline_day'";

        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                try {
                    return Integer.parseInt(resultSet.getString("setting_value"));
                } catch (NumberFormatException e) {
                    return 10;
                }
            }
        }

        return 10;
    }

    private LocalDate calculateDeadlineDate(SchoolYear schoolYear, SchoolYearMonth month, int deadlineDay) {
        int year = schoolYear.getEndDate().getYear();

        if (month.getMonthNumber() >= schoolYear.getStartDate().getMonthValue()) {
            year = schoolYear.getStartDate().getYear();
        }

        YearMonth yearMonth = YearMonth.of(year, month.getMonthNumber());
        int safeDay = Math.min(deadlineDay, yearMonth.lengthOfMonth());
        return LocalDate.of(year, month.getMonthNumber(), safeDay);
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

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "Les tables de paiement n'existent pas. Cliquez sur Initialiser la base.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }
}
