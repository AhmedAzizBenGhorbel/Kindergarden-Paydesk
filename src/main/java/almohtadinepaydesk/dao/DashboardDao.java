package almohtadinepaydesk.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.database.DatabaseConnection;
import almohtadinepaydesk.models.PaymentMethod;
import almohtadinepaydesk.models.PaymentStatus;

/**
 * Data access helper for the dashboard screen.
 * It reads summary totals, recent payments, and quick child search data from the database.
 */
public class DashboardDao {

    private String lastErrorMessage = "";

    /**
     * Loads the dashboard counters and money totals for the selected filters.
     *
     * @param schoolYearId selected school year, or null for all years
     * @param monthId selected month, or null for all months
     * @param classGroup selected class group, or null for all groups
     * @return a populated DashboardStats object, never null
     */
    public DashboardStats getDashboardStats(Integer schoolYearId, Integer monthId, String classGroup) {
        DashboardStats stats = new DashboardStats();
        stats.setActiveChildrenCount(countActiveChildren(classGroup));

        // The dashboard uses the monthly records table as the main source of truth,
        // then adds paid totals from payment entries and counts by status.
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(SUM(mr.expected_amount), 0) AS total_expected,
                       COALESCE(SUM((
                            SELECT COALESCE(SUM(pe.amount), 0)
                            FROM payment_entries pe
                            WHERE pe.monthly_record_id = mr.id
                              AND pe.active = TRUE
                       )), 0) AS total_paid,
                       SUM(CASE WHEN mr.payment_status = ? THEN 1 ELSE 0 END) AS unpaid_count,
                       SUM(CASE WHEN mr.payment_status = ? THEN 1 ELSE 0 END) AS partial_count,
                       SUM(CASE WHEN mr.payment_status = ? THEN 1 ELSE 0 END) AS late_count
                FROM monthly_records mr
                JOIN children c ON c.id = mr.child_id
                JOIN school_year_months sym ON sym.id = mr.school_year_month_id
                JOIN school_years sy ON sy.id = sym.school_year_id
                WHERE mr.active = TRUE
                """);

        parameters.add(PaymentStatus.NON_PAYE.getLabel());
        parameters.add(PaymentStatus.PARTIEL.getLabel());
        parameters.add(PaymentStatus.EN_RETARD.getLabel());

        addCommonFilters(sql, parameters, schoolYearId, monthId, classGroup);

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            fillParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    stats.setTotalExpected(cleanAmount(resultSet.getBigDecimal("total_expected")));
                    stats.setTotalPaid(cleanAmount(resultSet.getBigDecimal("total_paid")));
                    stats.setTotalRemaining(calculateRemaining(stats.getTotalExpected(), stats.getTotalPaid()));
                    stats.setUnpaidCount(resultSet.getInt("unpaid_count"));
                    stats.setPartialCount(resultSet.getInt("partial_count"));
                    stats.setLateCount(resultSet.getInt("late_count"));
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Returns the most recent payment entries matching the chosen filters.
     *
     * @param schoolYearId selected school year, or null for all years
     * @param monthId selected month, or null for all months
     * @param classGroup selected class group, or null for all groups
     * @return a list of recent payments, sorted from newest to oldest
     */
    public List<RecentPayment> listRecentPayments(Integer schoolYearId, Integer monthId, String classGroup) {
        List<RecentPayment> payments = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT pe.amount,
                       pe.payment_date,
                       pe.payment_method,
                       TRIM(CONCAT(COALESCE(c.first_name, ''), ' ', COALESCE(c.last_name, ''))) AS child_full_name,
                       sy.name AS school_year_name,
                       sym.month_name,
                       r.receipt_number
                FROM payment_entries pe
                JOIN monthly_records mr ON mr.id = pe.monthly_record_id
                JOIN children c ON c.id = mr.child_id
                JOIN school_year_months sym ON sym.id = mr.school_year_month_id
                JOIN school_years sy ON sy.id = sym.school_year_id
                LEFT JOIN receipts r ON r.payment_entry_id = pe.id
                WHERE pe.active = TRUE
                  AND mr.active = TRUE
                """);

        addCommonFilters(sql, parameters, schoolYearId, monthId, classGroup);
        sql.append(" ORDER BY pe.payment_date DESC, pe.id DESC LIMIT 8");

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            fillParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    payments.add(mapRecentPayment(resultSet));
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return payments;
    }

    /**
     * Searches active children for the quick-search box on the dashboard.
     *
     * @param searchText partial child name entered by the user
     * @param classGroup selected class group, or null for all groups
     * @return a short list of matching active children
     */
    public List<ChildSearchResult> searchActiveChildren(String searchText, String classGroup) {
        List<ChildSearchResult> children = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT id,
                       TRIM(CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, ''))) AS child_full_name,
                       class_group,
                       parent_full_name,
                       parent_phone,
                       monthly_fee
                FROM children
                WHERE active = TRUE
                """);

        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append(" AND TRIM(CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, ''))) LIKE ?");
            parameters.add("%" + searchText.trim() + "%");
        }

        if (classGroup != null && !classGroup.isBlank()) {
            sql.append(" AND class_group = ?");
            parameters.add(classGroup);
        }

        sql.append(" ORDER BY first_name LIMIT 10");

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            fillParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    children.add(mapChildSearchResult(resultSet));
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return children;
    }

    /**
     * Returns the list of class groups currently used by active children.
     *
     * @return distinct class/group names
     */
    public List<String> listClassGroups() {
        List<String> classGroups = new ArrayList<>();
        String sql = """
                SELECT DISTINCT class_group
                FROM children
                WHERE active = TRUE
                  AND class_group IS NOT NULL
                  AND class_group <> ''
                ORDER BY class_group
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                classGroups.add(resultSet.getString("class_group"));
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return classGroups;
    }

    /**
     * Returns the latest database error in a user-friendly form.
     *
     * @return a readable error message for the UI
     */
    public String getLastErrorMessage() {
        if (lastErrorMessage == null || lastErrorMessage.isBlank()) {
            return "Une erreur est survenue pendant l'op\u00e9ration.";
        }

        return lastErrorMessage;
    }

    /**
     * Counts active children, optionally limited to one class/group.
     *
     * @param classGroup selected class group, or null for all groups
     * @return number of active children
     */
    private int countActiveChildren(String classGroup) {
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS active_children_count
                FROM children
                WHERE active = TRUE
                """);

        if (classGroup != null && !classGroup.isBlank()) {
            sql.append(" AND class_group = ?");
            parameters.add(classGroup);
        }

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {

            fillParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("active_children_count");
                }
            }

            lastErrorMessage = "";
        } catch (SQLException e) {
            lastErrorMessage = createFriendlyErrorMessage(e);
            e.printStackTrace();
        }

        return 0;
    }

    /**
     * Adds the shared dashboard filters to a SQL query.
     * This keeps the different dashboard queries aligned on the same year, month, and class filters.
     */
    private void addCommonFilters(
            StringBuilder sql,
            List<Object> parameters,
            Integer schoolYearId,
            Integer monthId,
            String classGroup) {

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
    }

    /**
     * Converts one result-set row into a recent payment object for the table view.
     */
    private RecentPayment mapRecentPayment(ResultSet resultSet) throws SQLException {
        RecentPayment payment = new RecentPayment();
        payment.setChildFullName(resultSet.getString("child_full_name"));
        payment.setSchoolYearName(resultSet.getString("school_year_name"));
        payment.setMonthName(resultSet.getString("month_name"));
        payment.setAmount(cleanAmount(resultSet.getBigDecimal("amount")));

        if (resultSet.getDate("payment_date") != null) {
            payment.setPaymentDate(resultSet.getDate("payment_date").toLocalDate());
        }

        payment.setPaymentMethod(PaymentMethod.fromLabelOrName(resultSet.getString("payment_method")));
        payment.setReceiptNumber(resultSet.getString("receipt_number"));
        return payment;
    }

    /**
     * Converts one result-set row into a quick-search child result.
     */
    private ChildSearchResult mapChildSearchResult(ResultSet resultSet) throws SQLException {
        ChildSearchResult child = new ChildSearchResult();
        child.setId(resultSet.getInt("id"));
        child.setFullName(resultSet.getString("child_full_name"));
        child.setClassGroup(resultSet.getString("class_group"));
        child.setParentFullName(resultSet.getString("parent_full_name"));
        child.setParentPhone(resultSet.getString("parent_phone"));
        child.setMonthlyFee(cleanAmount(resultSet.getBigDecimal("monthly_fee")));
        return child;
    }

    /**
     * Normalizes null monetary values to zero before calculations.
     */
    private BigDecimal cleanAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }

        return amount;
    }

    /**
     * Prevents the remaining amount from going below zero.
     */
    private BigDecimal calculateRemaining(BigDecimal expected, BigDecimal paid) {
        BigDecimal remaining = cleanAmount(expected).subtract(cleanAmount(paid));

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return remaining;
    }

    /**
     * Binds a parameter list into a prepared statement in order.
     */
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

    /**
     * Turns raw SQL errors into a simpler message the UI can show.
     */
    private String createFriendlyErrorMessage(SQLException e) {
        String message = e.getMessage();

        if (message != null && message.toLowerCase().contains("doesn't exist")) {
            return "Les tables du tableau de bord n'existent pas. Cliquez sur Initialiser la base.";
        }

        return "Erreur base de donn\u00e9es: " + message;
    }

    /**
     * Small data holder for dashboard counters and totals.
     */
    public static class DashboardStats {
        private int activeChildrenCount;
        private BigDecimal totalExpected = BigDecimal.ZERO;
        private BigDecimal totalPaid = BigDecimal.ZERO;
        private BigDecimal totalRemaining = BigDecimal.ZERO;
        private int unpaidCount;
        private int partialCount;
        private int lateCount;

        public int getActiveChildrenCount() {
            return activeChildrenCount;
        }

        public void setActiveChildrenCount(int activeChildrenCount) {
            this.activeChildrenCount = activeChildrenCount;
        }

        public BigDecimal getTotalExpected() {
            return totalExpected;
        }

        public void setTotalExpected(BigDecimal totalExpected) {
            this.totalExpected = totalExpected;
        }

        public BigDecimal getTotalPaid() {
            return totalPaid;
        }

        public void setTotalPaid(BigDecimal totalPaid) {
            this.totalPaid = totalPaid;
        }

        public BigDecimal getTotalRemaining() {
            return totalRemaining;
        }

        public void setTotalRemaining(BigDecimal totalRemaining) {
            this.totalRemaining = totalRemaining;
        }

        public int getUnpaidCount() {
            return unpaidCount;
        }

        public void setUnpaidCount(int unpaidCount) {
            this.unpaidCount = unpaidCount;
        }

        public int getPartialCount() {
            return partialCount;
        }

        public void setPartialCount(int partialCount) {
            this.partialCount = partialCount;
        }

        public int getLateCount() {
            return lateCount;
        }

        public void setLateCount(int lateCount) {
            this.lateCount = lateCount;
        }
    }

    /**
     * Small data holder for the recent payments table.
     */
    public static class RecentPayment {
        private String childFullName;
        private String schoolYearName;
        private String monthName;
        private BigDecimal amount;
        private java.time.LocalDate paymentDate;
        private PaymentMethod paymentMethod;
        private String receiptNumber;

        public String getChildFullName() {
            return childFullName;
        }

        public void setChildFullName(String childFullName) {
            this.childFullName = childFullName;
        }

        public String getSchoolYearName() {
            return schoolYearName;
        }

        public void setSchoolYearName(String schoolYearName) {
            this.schoolYearName = schoolYearName;
        }

        public String getMonthName() {
            return monthName;
        }

        public void setMonthName(String monthName) {
            this.monthName = monthName;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public java.time.LocalDate getPaymentDate() {
            return paymentDate;
        }

        public void setPaymentDate(java.time.LocalDate paymentDate) {
            this.paymentDate = paymentDate;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getReceiptNumber() {
            return receiptNumber;
        }

        public void setReceiptNumber(String receiptNumber) {
            this.receiptNumber = receiptNumber;
        }
    }

    /**
     * Small data holder for the dashboard child quick-search table.
     */
    public static class ChildSearchResult {
        private int id;
        private String fullName;
        private String classGroup;
        private String parentFullName;
        private String parentPhone;
        private BigDecimal monthlyFee;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getClassGroup() {
            return classGroup;
        }

        public void setClassGroup(String classGroup) {
            this.classGroup = classGroup;
        }

        public String getParentFullName() {
            return parentFullName;
        }

        public void setParentFullName(String parentFullName) {
            this.parentFullName = parentFullName;
        }

        public String getParentPhone() {
            return parentPhone;
        }

        public void setParentPhone(String parentPhone) {
            this.parentPhone = parentPhone;
        }

        public BigDecimal getMonthlyFee() {
            return monthlyFee;
        }

        public void setMonthlyFee(BigDecimal monthlyFee) {
            this.monthlyFee = monthlyFee;
        }
    }
}
