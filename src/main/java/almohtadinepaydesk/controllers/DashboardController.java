package almohtadinepaydesk.controllers;

import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.dao.DashboardDao;
import almohtadinepaydesk.dao.DashboardDao.ChildSearchResult;
import almohtadinepaydesk.dao.DashboardDao.DashboardStats;
import almohtadinepaydesk.dao.DashboardDao.RecentPayment;
import almohtadinepaydesk.dao.MonthlyRecordDao;
import almohtadinepaydesk.dao.SchoolYearDao;
import almohtadinepaydesk.dao.SchoolYearMonthDao;
import almohtadinepaydesk.models.PaymentMethod;
import almohtadinepaydesk.models.SchoolYear;
import almohtadinepaydesk.models.SchoolYearMonth;
import almohtadinepaydesk.utils.CurrencyUtil;
import almohtadinepaydesk.utils.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class DashboardController {

    private static final String ALL_GROUPS = "Toutes les classes";

    private final DashboardDao dashboardDao = new DashboardDao();
    private final SchoolYearDao schoolYearDao = new SchoolYearDao();
    private final SchoolYearMonthDao schoolYearMonthDao = new SchoolYearMonthDao();
    private final MonthlyRecordDao monthlyRecordDao = new MonthlyRecordDao();
    private final ObservableList<RecentPayment> recentPayments = FXCollections.observableArrayList();
    private final ObservableList<ChildSearchResult> childSearchResults = FXCollections.observableArrayList();
    private final List<SchoolYearMonth> currentMonths = new ArrayList<>();

    private boolean loadingFilters;

    @FXML
    private ComboBox<SchoolYear> schoolYearComboBox;

    @FXML
    private ComboBox<SchoolYearMonth> monthComboBox;

    @FXML
    private ComboBox<String> classGroupComboBox;

    @FXML
    private Label activeChildrenLabel;

    @FXML
    private Label totalExpectedLabel;

    @FXML
    private Label totalPaidLabel;

    @FXML
    private Label totalRemainingLabel;

    @FXML
    private Label unpaidCountLabel;

    @FXML
    private Label partialCountLabel;

    @FXML
    private Label lateCountLabel;

    @FXML
    private TableView<RecentPayment> recentPaymentsTable;

    @FXML
    private TableColumn<RecentPayment, String> paymentDateColumn;

    @FXML
    private TableColumn<RecentPayment, String> paymentChildColumn;

    @FXML
    private TableColumn<RecentPayment, String> paymentMonthColumn;

    @FXML
    private TableColumn<RecentPayment, String> paymentAmountColumn;

    @FXML
    private TableColumn<RecentPayment, String> paymentMethodColumn;

    @FXML
    private TableColumn<RecentPayment, String> paymentReceiptColumn;

    @FXML
    private TextField childSearchField;

    @FXML
    private TableView<ChildSearchResult> childSearchTable;

    @FXML
    private TableColumn<ChildSearchResult, String> childNameColumn;

    @FXML
    private TableColumn<ChildSearchResult, String> childClassColumn;

    @FXML
    private TableColumn<ChildSearchResult, String> childParentColumn;

    @FXML
    private TableColumn<ChildSearchResult, String> childPhoneColumn;

    @FXML
    private TableColumn<ChildSearchResult, String> childFeeColumn;

    @FXML
    private void initialize() {
        configureTables();
        configureFilters();
        loadInitialFilters();
        loadDashboard();
    }

    private void configureTables() {
        paymentDateColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(DateUtil.formatDate(cellData.getValue().getPaymentDate())));
        paymentChildColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getChildFullName())));
        paymentMonthColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                formatMonthAndYear(cellData.getValue().getMonthName(), cellData.getValue().getSchoolYearName())));
        paymentAmountColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(CurrencyUtil.formatAmount(cellData.getValue().getAmount())));
        paymentMethodColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(formatPaymentMethod(cellData.getValue().getPaymentMethod())));
        paymentReceiptColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getReceiptNumber())));

        recentPaymentsTable.setItems(recentPayments);

        childNameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getFullName())));
        childClassColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getClassGroup())));
        childParentColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getParentFullName())));
        childPhoneColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getParentPhone())));
        childFeeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(CurrencyUtil.formatAmount(cellData.getValue().getMonthlyFee())));

        childSearchTable.setItems(childSearchResults);
    }

    private void configureFilters() {
        schoolYearComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handleSchoolYearChanged());
        monthComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!loadingFilters) {
                loadDashboard();
            }
        });
        classGroupComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!loadingFilters) {
                loadDashboard();
            }
        });
        childSearchField.textProperty().addListener((observable, oldValue, newValue) -> loadChildSearchResults());
    }

    private void loadInitialFilters() {
        loadingFilters = true;

        schoolYearComboBox.getItems().setAll(schoolYearDao.listAllSchoolYears());
        selectActiveSchoolYear();

        if (schoolYearComboBox.getValue() == null && !schoolYearComboBox.getItems().isEmpty()) {
            schoolYearComboBox.getSelectionModel().selectFirst();
        }

        loadMonthsForSelectedYear();
        loadClassGroups();

        loadingFilters = false;
    }

    private void handleSchoolYearChanged() {
        if (loadingFilters) {
            return;
        }

        loadingFilters = true;
        loadMonthsForSelectedYear();
        loadingFilters = false;
        loadDashboard();
    }

    private void selectActiveSchoolYear() {
        SchoolYear activeSchoolYear = schoolYearDao.findActiveSchoolYear();

        if (activeSchoolYear == null) {
            return;
        }

        for (SchoolYear schoolYear : schoolYearComboBox.getItems()) {
            if (schoolYear.getId() == activeSchoolYear.getId()) {
                schoolYearComboBox.setValue(schoolYear);
                return;
            }
        }
    }

    private void loadMonthsForSelectedYear() {
        currentMonths.clear();
        monthComboBox.getItems().clear();

        SchoolYear selectedYear = schoolYearComboBox.getValue();
        if (selectedYear == null) {
            return;
        }

        List<SchoolYearMonth> allMonths = schoolYearMonthDao.listBySchoolYearId(selectedYear.getId());

        for (SchoolYearMonth month : allMonths) {
            if (month.isActive()) {
                currentMonths.add(month);
            }
        }

        if (currentMonths.isEmpty()) {
            currentMonths.addAll(allMonths);
        }

        monthComboBox.getItems().setAll(currentMonths);

        if (!monthComboBox.getItems().isEmpty()) {
            monthComboBox.getSelectionModel().selectFirst();
        }
    }

    private void loadClassGroups() {
        classGroupComboBox.getItems().clear();
        classGroupComboBox.getItems().add(ALL_GROUPS);
        classGroupComboBox.getItems().addAll(dashboardDao.listClassGroups());
        classGroupComboBox.setValue(ALL_GROUPS);
    }

    private void loadDashboard() {
        Integer schoolYearId = getSelectedSchoolYearId();
        Integer monthId = getSelectedMonthId();
        String classGroup = getSelectedClassGroup();

        if (schoolYearId != null) {
            monthlyRecordDao.recalculateMonthlyRecordsForSchoolYear(schoolYearId);
        } else {
            monthlyRecordDao.recalculateAllMonthlyRecords();
        }

        DashboardStats stats = dashboardDao.getDashboardStats(schoolYearId, monthId, classGroup);
        showStats(stats);

        recentPayments.setAll(dashboardDao.listRecentPayments(schoolYearId, monthId, classGroup));
        loadChildSearchResults();
    }

    private void loadChildSearchResults() {
        if (childSearchField == null) {
            return;
        }

        childSearchResults.setAll(
                dashboardDao.searchActiveChildren(childSearchField.getText(), getSelectedClassGroup()));
    }

    private void showStats(DashboardStats stats) {
        activeChildrenLabel.setText(String.valueOf(stats.getActiveChildrenCount()));
        totalExpectedLabel.setText(CurrencyUtil.formatAmount(stats.getTotalExpected()));
        totalPaidLabel.setText(CurrencyUtil.formatAmount(stats.getTotalPaid()));
        totalRemainingLabel.setText(CurrencyUtil.formatAmount(stats.getTotalRemaining()));
        unpaidCountLabel.setText(String.valueOf(stats.getUnpaidCount()));
        partialCountLabel.setText(String.valueOf(stats.getPartialCount()));
        lateCountLabel.setText(String.valueOf(stats.getLateCount()));
    }

    private Integer getSelectedSchoolYearId() {
        SchoolYear selectedYear = schoolYearComboBox.getValue();

        if (selectedYear == null) {
            return null;
        }

        return selectedYear.getId();
    }

    private Integer getSelectedMonthId() {
        SchoolYearMonth selectedMonth = monthComboBox.getValue();

        if (selectedMonth == null) {
            return null;
        }

        return selectedMonth.getId();
    }

    private String getSelectedClassGroup() {
        String selectedGroup = classGroupComboBox.getValue();

        if (selectedGroup == null || selectedGroup.equals(ALL_GROUPS)) {
            return null;
        }

        return selectedGroup;
    }

    private String formatMonthAndYear(String monthName, String schoolYearName) {
        if (schoolYearName == null || schoolYearName.isBlank()) {
            return valueOrEmpty(monthName);
        }

        if (monthName == null || monthName.isBlank()) {
            return schoolYearName;
        }

        return monthName + " - " + schoolYearName;
    }

    private String formatPaymentMethod(PaymentMethod method) {
        if (method == null) {
            return "";
        }

        return method.getLabel();
    }

    private String valueOrEmpty(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }
}
