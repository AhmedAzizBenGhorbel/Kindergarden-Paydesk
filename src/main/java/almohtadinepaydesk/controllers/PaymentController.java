package almohtadinepaydesk.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.dao.MonthlyRecordDao;
import almohtadinepaydesk.dao.SchoolYearDao;
import almohtadinepaydesk.dao.SchoolYearMonthDao;
import almohtadinepaydesk.models.MonthlyRecord;
import almohtadinepaydesk.models.PaymentStatus;
import almohtadinepaydesk.models.SchoolYear;
import almohtadinepaydesk.models.SchoolYearMonth;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.utils.AlertUtil;
import almohtadinepaydesk.utils.CurrencyUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PaymentController {

    private static final String ALL_MONTHS = "Tous les mois";
    private static final String ALL_GROUPS = "Toutes les classes";
    private static final String ALL_STATUSES = "Tous les statuts";

    private final MonthlyRecordDao monthlyRecordDao = new MonthlyRecordDao();
    private final SchoolYearDao schoolYearDao = new SchoolYearDao();
    private final SchoolYearMonthDao schoolYearMonthDao = new SchoolYearMonthDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final ObservableList<MonthlyRecord> monthlyRecords = FXCollections.observableArrayList();
    private final List<SchoolYearMonth> currentMonths = new ArrayList<>();

    @FXML
    private ComboBox<SchoolYear> schoolYearComboBox;

    @FXML
    private ComboBox<String> monthComboBox;

    @FXML
    private ComboBox<String> classGroupComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TableView<MonthlyRecord> paymentsTable;

    @FXML
    private TableColumn<MonthlyRecord, String> childColumn;

    @FXML
    private TableColumn<MonthlyRecord, String> classColumn;

    @FXML
    private TableColumn<MonthlyRecord, String> schoolYearColumn;

    @FXML
    private TableColumn<MonthlyRecord, String> monthColumn;

    @FXML
    private TableColumn<MonthlyRecord, String> expectedColumn;

    @FXML
    private TableColumn<MonthlyRecord, String> paidColumn;

    @FXML
    private TableColumn<MonthlyRecord, String> remainingColumn;

    @FXML
    private TableColumn<MonthlyRecord, String> advanceColumn;

    @FXML
    private TableColumn<MonthlyRecord, String> statusColumn;

    @FXML
    private TableColumn<MonthlyRecord, Void> actionsColumn;

    @FXML
    private void initialize() {
        configureTable();
        configureFilters();
        loadFilters();
        loadMonthlyRecords();
    }

    @FXML
    private void handleGenerateMonthlyRecords() {
        int createdCount = monthlyRecordDao.generateMonthlyRecordsForActiveSchoolYear();

        if (createdCount < 0) {
            AlertUtil.showError("Paiements", monthlyRecordDao.getLastErrorMessage());
            return;
        }

        logAction("GENERATE_MONTHLY_RECORDS", "G\u00e9n\u00e9ration des paiements mensuels");
        AlertUtil.showInfo("Paiements", "G\u00e9n\u00e9ration termin\u00e9e. Nouveaux enregistrements: " + createdCount + ".");
        selectActiveSchoolYear();
        loadFilters();
        loadMonthlyRecords();
    }

    @FXML
    private void handleRecalculateStatuses() {
        int updatedCount = monthlyRecordDao.recalculateAllMonthlyRecords();

        if (updatedCount < 0) {
            AlertUtil.showError("Paiements", monthlyRecordDao.getLastErrorMessage());
            return;
        }

        logAction("RECALCULATE_PAYMENT_STATUSES", "Recalcul des statuts de paiement");
        AlertUtil.showInfo("Paiements", "Statuts recalcul\u00e9s. Enregistrements mis \u00e0 jour: " + updatedCount + ".");
        loadMonthlyRecords();
    }

    private void configureTable() {
        childColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getChildFullName())));
        classColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getClassGroup())));
        schoolYearColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getSchoolYearName())));
        monthColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getMonthName())));
        expectedColumn.setCellValueFactory(cellData -> new SimpleStringProperty(CurrencyUtil.formatAmount(cellData.getValue().getTotalExpected())));
        paidColumn.setCellValueFactory(cellData -> new SimpleStringProperty(CurrencyUtil.formatAmount(cellData.getValue().getTotalPaid())));
        remainingColumn.setCellValueFactory(cellData -> new SimpleStringProperty(CurrencyUtil.formatAmount(cellData.getValue().getRemainingAmount())));
        advanceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(formatAdvance(cellData.getValue().getAdvanceAmount())));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPaymentStatus().getLabel()));
        configureActionsColumn();

        paymentsTable.setItems(monthlyRecords);
    }

    private void configureActionsColumn() {
        actionsColumn.setCellFactory(column -> new TableCell<>() {

            private final Button detailsButton = new Button("D\u00e9tails");

            {
                detailsButton.getStyleClass().add("small-action-button");
                detailsButton.setOnAction(event -> {
                    MonthlyRecord record = getTableView().getItems().get(getIndex());
                    openPaymentDetail(record);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(detailsButton);
                }
            }
        });
    }

    private void configureFilters() {
        schoolYearComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            loadMonthsForSelectedYear();
            loadMonthlyRecords();
        });
        monthComboBox.valueProperty().addListener((observable, oldValue, newValue) -> loadMonthlyRecords());
        classGroupComboBox.valueProperty().addListener((observable, oldValue, newValue) -> loadMonthlyRecords());
        statusComboBox.valueProperty().addListener((observable, oldValue, newValue) -> loadMonthlyRecords());
    }

    private void loadFilters() {
        SchoolYear selectedYear = schoolYearComboBox.getValue();
        schoolYearComboBox.getItems().setAll(schoolYearDao.listAllSchoolYears());

        if (selectedYear != null) {
            selectSchoolYearById(selectedYear.getId());
        }

        if (schoolYearComboBox.getValue() == null) {
            selectActiveSchoolYear();
        }

        if (schoolYearComboBox.getValue() == null && !schoolYearComboBox.getItems().isEmpty()) {
            schoolYearComboBox.getSelectionModel().selectFirst();
        }

        loadMonthsForSelectedYear();
        loadClassGroups();
        loadStatuses();
    }

    private void selectActiveSchoolYear() {
        SchoolYear activeSchoolYear = schoolYearDao.findActiveSchoolYear();

        if (activeSchoolYear != null) {
            selectSchoolYearById(activeSchoolYear.getId());
        }
    }

    private void selectSchoolYearById(int schoolYearId) {
        for (SchoolYear schoolYear : schoolYearComboBox.getItems()) {
            if (schoolYear.getId() == schoolYearId) {
                schoolYearComboBox.setValue(schoolYear);
                return;
            }
        }
    }

    private void loadMonthsForSelectedYear() {
        currentMonths.clear();
        monthComboBox.getItems().clear();
        monthComboBox.getItems().add(ALL_MONTHS);

        SchoolYear selectedYear = schoolYearComboBox.getValue();
        if (selectedYear != null) {
            currentMonths.addAll(schoolYearMonthDao.listBySchoolYearId(selectedYear.getId()));

            for (SchoolYearMonth month : currentMonths) {
                monthComboBox.getItems().add(month.getMonthName());
            }
        }

        if (monthComboBox.getValue() == null || !monthComboBox.getItems().contains(monthComboBox.getValue())) {
            monthComboBox.setValue(ALL_MONTHS);
        }
    }

    private void loadClassGroups() {
        String selectedGroup = classGroupComboBox.getValue();
        classGroupComboBox.getItems().clear();
        classGroupComboBox.getItems().add(ALL_GROUPS);
        classGroupComboBox.getItems().addAll(monthlyRecordDao.listClassGroups());

        if (selectedGroup != null && classGroupComboBox.getItems().contains(selectedGroup)) {
            classGroupComboBox.setValue(selectedGroup);
        } else {
            classGroupComboBox.setValue(ALL_GROUPS);
        }
    }

    private void loadStatuses() {
        String selectedStatus = statusComboBox.getValue();
        statusComboBox.getItems().clear();
        statusComboBox.getItems().add(ALL_STATUSES);

        for (PaymentStatus status : PaymentStatus.values()) {
            statusComboBox.getItems().add(status.getLabel());
        }

        if (selectedStatus != null && statusComboBox.getItems().contains(selectedStatus)) {
            statusComboBox.setValue(selectedStatus);
        } else {
            statusComboBox.setValue(ALL_STATUSES);
        }
    }

    private void loadMonthlyRecords() {
        Integer schoolYearId = getSelectedSchoolYearId();
        Integer monthId = getSelectedMonthId();
        String classGroup = getSelectedClassGroup();
        PaymentStatus status = getSelectedStatus();

        monthlyRecords.setAll(monthlyRecordDao.listMonthlyRecords(schoolYearId, monthId, classGroup, status));
    }

    private Integer getSelectedSchoolYearId() {
        SchoolYear selectedYear = schoolYearComboBox.getValue();

        if (selectedYear == null) {
            return null;
        }

        return selectedYear.getId();
    }

    private Integer getSelectedMonthId() {
        String selectedMonth = monthComboBox.getValue();

        if (selectedMonth == null || selectedMonth.equals(ALL_MONTHS)) {
            return null;
        }

        for (SchoolYearMonth month : currentMonths) {
            if (month.getMonthName().equals(selectedMonth)) {
                return month.getId();
            }
        }

        return null;
    }

    private String getSelectedClassGroup() {
        String selectedGroup = classGroupComboBox.getValue();

        if (selectedGroup == null || selectedGroup.equals(ALL_GROUPS)) {
            return null;
        }

        return selectedGroup;
    }

    private PaymentStatus getSelectedStatus() {
        String selectedStatus = statusComboBox.getValue();

        if (selectedStatus == null || selectedStatus.equals(ALL_STATUSES)) {
            return null;
        }

        return PaymentStatus.fromLabel(selectedStatus);
    }

    private String formatAdvance(BigDecimal advanceAmount) {
        if (advanceAmount != null && advanceAmount.compareTo(BigDecimal.ZERO) > 0) {
            return "Avance: " + CurrencyUtil.formatAmount(advanceAmount);
        }

        return CurrencyUtil.formatAmount(BigDecimal.ZERO);
    }

    private String valueOrEmpty(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    private void openPaymentDetail(MonthlyRecord record) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment-detail.fxml"));
            Parent root = loader.load();

            PaymentDetailController controller = loader.getController();
            controller.setMonthlyRecordId(record.getId());

            Scene scene = new Scene(root, 850, 620);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

            Stage detailStage = new Stage();
            detailStage.setTitle("D\u00e9tail du paiement");
            detailStage.initModality(Modality.WINDOW_MODAL);
            detailStage.initOwner(paymentsTable.getScene().getWindow());
            detailStage.setScene(scene);
            detailStage.showAndWait();

            loadMonthlyRecords();
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Paiements", "Impossible d'ouvrir le d\u00e9tail du paiement.");
        }
    }

    private void logAction(String action, String details) {
        User currentUser = Session.getCurrentUser();
        Integer currentUserId = null;

        if (currentUser != null) {
            currentUserId = currentUser.getId();
        }

        activityLogDao.log(currentUserId, action, "monthly_records", null, details);
    }
}
