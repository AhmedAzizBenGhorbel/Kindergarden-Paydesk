package almohtadinepaydesk.controllers;

import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.dao.ReceiptDao;
import almohtadinepaydesk.dao.SchoolYearDao;
import almohtadinepaydesk.dao.SchoolYearMonthDao;
import almohtadinepaydesk.models.PaymentMethod;
import almohtadinepaydesk.models.Receipt;
import almohtadinepaydesk.models.SchoolYear;
import almohtadinepaydesk.models.SchoolYearMonth;
import almohtadinepaydesk.utils.CurrencyUtil;
import almohtadinepaydesk.utils.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class ReceiptController {

    private static final String ALL_YEARS = "Toutes les ann\u00e9es";
    private static final String ALL_MONTHS = "Tous les mois";
    private static final String ALL_METHODS = "Tous les modes";

    private final ReceiptDao receiptDao = new ReceiptDao();
    private final SchoolYearDao schoolYearDao = new SchoolYearDao();
    private final SchoolYearMonthDao schoolYearMonthDao = new SchoolYearMonthDao();
    private final ObservableList<Receipt> receipts = FXCollections.observableArrayList();
    private final List<SchoolYearMonth> currentMonths = new ArrayList<>();

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<SchoolYear> schoolYearComboBox;

    @FXML
    private ComboBox<SchoolYearMonth> monthComboBox;

    @FXML
    private ComboBox<String> paymentMethodComboBox;

    @FXML
    private TableView<Receipt> receiptsTable;

    @FXML
    private TableColumn<Receipt, String> receiptNumberColumn;

    @FXML
    private TableColumn<Receipt, String> childColumn;

    @FXML
    private TableColumn<Receipt, String> parentColumn;

    @FXML
    private TableColumn<Receipt, String> amountColumn;

    @FXML
    private TableColumn<Receipt, String> paymentDateColumn;

    @FXML
    private TableColumn<Receipt, String> paymentMethodColumn;

    @FXML
    private TableColumn<Receipt, String> schoolYearColumn;

    @FXML
    private TableColumn<Receipt, String> monthColumn;

    @FXML
    private TableColumn<Receipt, String> createdByColumn;

    @FXML
    private TableColumn<Receipt, String> noteColumn;

    @FXML
    private void initialize() {
        configureTable();
        configureFilters();
        loadFilters();
        loadReceipts();
    }

    @FXML
    private void handleRefresh() {
        loadFilters();
        loadReceipts();
    }

    private void configureTable() {
        receiptNumberColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getReceiptNumber())));
        childColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getChildFullName())));
        parentColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getParentFullName())));
        amountColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(CurrencyUtil.formatAmount(cellData.getValue().getTotalAmount())));
        paymentDateColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(DateUtil.formatDate(cellData.getValue().getReceiptDate())));
        paymentMethodColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(formatPaymentMethod(cellData.getValue().getPaymentMethod())));
        schoolYearColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getSchoolYearName())));
        monthColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getMonthName())));
        createdByColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getCreatedByFullName())));
        noteColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getNotes())));

        receiptsTable.setItems(receipts);
    }

    private void configureFilters() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> loadReceipts());
        schoolYearComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            loadMonthsForSelectedYear();
            loadReceipts();
        });
        monthComboBox.valueProperty().addListener((observable, oldValue, newValue) -> loadReceipts());
        paymentMethodComboBox.valueProperty().addListener((observable, oldValue, newValue) -> loadReceipts());
    }

    private void loadFilters() {
        int selectedYearId = getSelectedSchoolYearIdOrZero();
        int selectedMonthId = getSelectedMonthIdOrZero();
        String selectedMethod = paymentMethodComboBox.getValue();

        schoolYearComboBox.getItems().clear();
        schoolYearComboBox.getItems().add(createAllSchoolYearsItem());
        schoolYearComboBox.getItems().addAll(schoolYearDao.listAllSchoolYears());
        selectSchoolYearById(selectedYearId);

        if (schoolYearComboBox.getValue() == null) {
            schoolYearComboBox.getSelectionModel().selectFirst();
        }

        loadMonthsForSelectedYear();
        selectMonthById(selectedMonthId);

        paymentMethodComboBox.getItems().clear();
        paymentMethodComboBox.getItems().add(ALL_METHODS);
        for (PaymentMethod method : PaymentMethod.values()) {
            paymentMethodComboBox.getItems().add(method.getLabel());
        }

        if (selectedMethod != null && paymentMethodComboBox.getItems().contains(selectedMethod)) {
            paymentMethodComboBox.setValue(selectedMethod);
        } else {
            paymentMethodComboBox.setValue(ALL_METHODS);
        }
    }

    private void loadMonthsForSelectedYear() {
        int selectedMonthId = getSelectedMonthIdOrZero();
        currentMonths.clear();
        monthComboBox.getItems().clear();
        monthComboBox.getItems().add(createAllMonthsItem());

        Integer schoolYearId = getSelectedSchoolYearId();
        if (schoolYearId != null) {
            currentMonths.addAll(schoolYearMonthDao.listBySchoolYearId(schoolYearId));
            monthComboBox.getItems().addAll(currentMonths);
        }

        selectMonthById(selectedMonthId);

        if (monthComboBox.getValue() == null) {
            monthComboBox.getSelectionModel().selectFirst();
        }
    }

    private void loadReceipts() {
        String searchText = searchField.getText();
        Integer schoolYearId = getSelectedSchoolYearId();
        Integer monthId = getSelectedMonthId();
        PaymentMethod paymentMethod = getSelectedPaymentMethod();

        receipts.setAll(receiptDao.listReceipts(searchText, schoolYearId, monthId, paymentMethod));
    }

    private SchoolYear createAllSchoolYearsItem() {
        SchoolYear schoolYear = new SchoolYear();
        schoolYear.setId(0);
        schoolYear.setName(ALL_YEARS);
        return schoolYear;
    }

    private SchoolYearMonth createAllMonthsItem() {
        SchoolYearMonth month = new SchoolYearMonth();
        month.setId(0);
        month.setMonthName(ALL_MONTHS);
        return month;
    }

    private Integer getSelectedSchoolYearId() {
        SchoolYear selectedYear = schoolYearComboBox.getValue();

        if (selectedYear == null || selectedYear.getId() == 0) {
            return null;
        }

        return selectedYear.getId();
    }

    private int getSelectedSchoolYearIdOrZero() {
        SchoolYear selectedYear = schoolYearComboBox.getValue();

        if (selectedYear == null) {
            return 0;
        }

        return selectedYear.getId();
    }

    private Integer getSelectedMonthId() {
        SchoolYearMonth selectedMonth = monthComboBox.getValue();

        if (selectedMonth == null || selectedMonth.getId() == 0) {
            return null;
        }

        return selectedMonth.getId();
    }

    private int getSelectedMonthIdOrZero() {
        SchoolYearMonth selectedMonth = monthComboBox.getValue();

        if (selectedMonth == null) {
            return 0;
        }

        return selectedMonth.getId();
    }

    private PaymentMethod getSelectedPaymentMethod() {
        String selectedMethod = paymentMethodComboBox.getValue();

        if (selectedMethod == null || selectedMethod.equals(ALL_METHODS)) {
            return null;
        }

        return PaymentMethod.fromLabelOrName(selectedMethod);
    }

    private void selectSchoolYearById(int schoolYearId) {
        for (SchoolYear schoolYear : schoolYearComboBox.getItems()) {
            if (schoolYear.getId() == schoolYearId) {
                schoolYearComboBox.setValue(schoolYear);
                return;
            }
        }
    }

    private void selectMonthById(int monthId) {
        for (SchoolYearMonth month : monthComboBox.getItems()) {
            if (month.getId() == monthId) {
                monthComboBox.setValue(month);
                return;
            }
        }
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
