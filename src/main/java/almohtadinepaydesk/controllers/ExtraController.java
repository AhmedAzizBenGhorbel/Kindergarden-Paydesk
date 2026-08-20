package almohtadinepaydesk.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.dao.ChildDao;
import almohtadinepaydesk.dao.ExtraDao;
import almohtadinepaydesk.dao.MonthlyRecordDao;
import almohtadinepaydesk.dao.SchoolYearDao;
import almohtadinepaydesk.dao.SchoolYearMonthDao;
import almohtadinepaydesk.models.Child;
import almohtadinepaydesk.models.Extra;
import almohtadinepaydesk.models.ExtraType;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ExtraController {

    private static final String NO_END_MONTH = "Aucun";

    private final ExtraDao extraDao = new ExtraDao();
    private final ChildDao childDao = new ChildDao();
    private final SchoolYearDao schoolYearDao = new SchoolYearDao();
    private final SchoolYearMonthDao schoolYearMonthDao = new SchoolYearMonthDao();
    private final MonthlyRecordDao monthlyRecordDao = new MonthlyRecordDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final ObservableList<Extra> extras = FXCollections.observableArrayList();
    private final List<SchoolYearMonth> currentMonths = new ArrayList<>();

    @FXML
    private TableView<Extra> extrasTable;

    @FXML
    private TableColumn<Extra, String> childColumn;

    @FXML
    private TableColumn<Extra, String> labelColumn;

    @FXML
    private TableColumn<Extra, String> amountColumn;

    @FXML
    private TableColumn<Extra, String> typeColumn;

    @FXML
    private TableColumn<Extra, String> schoolYearColumn;

    @FXML
    private TableColumn<Extra, String> startMonthColumn;

    @FXML
    private TableColumn<Extra, String> endMonthColumn;

    @FXML
    private TableColumn<Extra, String> stateColumn;

    @FXML
    private TableColumn<Extra, String> noteColumn;

    @FXML
    private ComboBox<Child> childComboBox;

    @FXML
    private ComboBox<String> labelComboBox;

    @FXML
    private TextField amountField;

    @FXML
    private ComboBox<ExtraType> typeComboBox;

    @FXML
    private ComboBox<SchoolYear> schoolYearComboBox;

    @FXML
    private ComboBox<String> startMonthComboBox;

    @FXML
    private ComboBox<String> endMonthComboBox;

    @FXML
    private TextArea noteArea;

    @FXML
    private Button updateButton;

    @FXML
    private Button deactivateButton;

    @FXML
    private Button reactivateButton;

    @FXML
    private void initialize() {
        configureTable();
        configureForm();
        loadFormLists();
        loadExtras();
        resetForm();

        extrasTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldExtra, selectedExtra) -> fillForm(selectedExtra));
    }

    @FXML
    private void handleAddExtra() {
        Extra extra = readExtraFromForm();

        if (extra == null) {
            return;
        }

        extra.setActive(true);

        if (extraDao.createExtra(extra)) {
            monthlyRecordDao.recalculateAllMonthlyRecords();
            logAction("CREATE_EXTRA", extra.getId(), "Ajout du frais: " + extra.getLabel());
            AlertUtil.showInfo("Frais suppl\u00e9mentaires", "Frais suppl\u00e9mentaire ajout\u00e9 avec succ\u00e8s.");
            loadExtras();
            resetForm();
        } else {
            AlertUtil.showError("Frais suppl\u00e9mentaires", extraDao.getLastErrorMessage());
        }
    }

    @FXML
    private void handleUpdateExtra() {
        Extra selectedExtra = extrasTable.getSelectionModel().getSelectedItem();

        if (selectedExtra == null) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Veuillez s\u00e9lectionner un frais \u00e0 modifier.");
            return;
        }

        Extra formExtra = readExtraFromForm();
        if (formExtra == null) {
            return;
        }

        selectedExtra.setChildId(formExtra.getChildId());
        selectedExtra.setSchoolYearMonthId(formExtra.getSchoolYearMonthId());
        selectedExtra.setStartMonthId(formExtra.getStartMonthId());
        selectedExtra.setEndMonthId(formExtra.getEndMonthId());
        selectedExtra.setLabel(formExtra.getLabel());
        selectedExtra.setExtraType(formExtra.getExtraType());
        selectedExtra.setAmount(formExtra.getAmount());
        selectedExtra.setNotes(formExtra.getNotes());

        if (extraDao.updateExtra(selectedExtra)) {
            monthlyRecordDao.recalculateAllMonthlyRecords();
            logAction("UPDATE_EXTRA", selectedExtra.getId(), "Modification du frais: " + selectedExtra.getLabel());
            AlertUtil.showInfo("Frais suppl\u00e9mentaires", "Frais suppl\u00e9mentaire modifi\u00e9 avec succ\u00e8s.");
            loadExtras();
            resetForm();
        } else {
            AlertUtil.showError("Frais suppl\u00e9mentaires", extraDao.getLastErrorMessage());
        }
    }

    @FXML
    private void handleDeactivateExtra() {
        Extra selectedExtra = extrasTable.getSelectionModel().getSelectedItem();

        if (selectedExtra == null) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Veuillez s\u00e9lectionner un frais \u00e0 d\u00e9sactiver.");
            return;
        }

        if (!selectedExtra.isActive()) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Ce frais est d\u00e9j\u00e0 inactif.");
            return;
        }

        if (extraDao.setActive(selectedExtra.getId(), false)) {
            monthlyRecordDao.recalculateAllMonthlyRecords();
            logAction("DEACTIVATE_EXTRA", selectedExtra.getId(), "D\u00e9sactivation du frais: " + selectedExtra.getLabel());
            AlertUtil.showInfo("Frais suppl\u00e9mentaires", "Frais suppl\u00e9mentaire d\u00e9sactiv\u00e9 avec succ\u00e8s.");
            loadExtras();
            resetForm();
        } else {
            AlertUtil.showError("Frais suppl\u00e9mentaires", extraDao.getLastErrorMessage());
        }
    }

    @FXML
    private void handleReactivateExtra() {
        Extra selectedExtra = extrasTable.getSelectionModel().getSelectedItem();

        if (selectedExtra == null) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Veuillez s\u00e9lectionner un frais \u00e0 r\u00e9activer.");
            return;
        }

        if (selectedExtra.isActive()) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Ce frais est d\u00e9j\u00e0 actif.");
            return;
        }

        if (extraDao.setActive(selectedExtra.getId(), true)) {
            monthlyRecordDao.recalculateAllMonthlyRecords();
            logAction("REACTIVATE_EXTRA", selectedExtra.getId(), "R\u00e9activation du frais: " + selectedExtra.getLabel());
            AlertUtil.showInfo("Frais suppl\u00e9mentaires", "Frais suppl\u00e9mentaire r\u00e9activ\u00e9 avec succ\u00e8s.");
            loadExtras();
            resetForm();
        } else {
            AlertUtil.showError("Frais suppl\u00e9mentaires", extraDao.getLastErrorMessage());
        }
    }

    @FXML
    private void resetForm() {
        extrasTable.getSelectionModel().clearSelection();
        childComboBox.getSelectionModel().clearSelection();
        labelComboBox.getSelectionModel().clearSelection();
        labelComboBox.getEditor().clear();
        amountField.clear();
        typeComboBox.setValue(ExtraType.PONCTUEL);
        selectActiveSchoolYear();
        startMonthComboBox.getSelectionModel().clearSelection();
        endMonthComboBox.setValue(NO_END_MONTH);
        noteArea.clear();
        updateButton.setDisable(true);
        deactivateButton.setDisable(true);
        reactivateButton.setDisable(true);
    }

    private void configureTable() {
        childColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getChildFullName())));
        labelColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getLabel())));
        amountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(CurrencyUtil.formatAmount(cellData.getValue().getAmount())));
        typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getExtraType().getLabel()));
        schoolYearColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getSchoolYearName())));
        startMonthColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getStartMonthName())));
        endMonthColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getEndMonthName())));
        stateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().isActive()) {
                return new SimpleStringProperty("Actif");
            }
            return new SimpleStringProperty("Inactif");
        });
        noteColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getNotes())));

        extrasTable.setItems(extras);
    }

    private void configureForm() {
        labelComboBox.setEditable(true);
        labelComboBox.getItems().setAll("Transport", "Cantine", "Frais d\u2019inscription", "Sortie", "Tenue", "Club", "Autre");
        typeComboBox.getItems().setAll(ExtraType.values());
        typeComboBox.setValue(ExtraType.PONCTUEL);
        schoolYearComboBox.valueProperty().addListener((observable, oldYear, newYear) -> loadMonthsForSelectedYear());
    }

    private void loadFormLists() {
        childComboBox.getItems().clear();
        for (Child child : childDao.listAllChildren()) {
            if (child.isActive()) {
                childComboBox.getItems().add(child);
            }
        }

        schoolYearComboBox.getItems().setAll(schoolYearDao.listAllSchoolYears());
        selectActiveSchoolYear();
        loadMonthsForSelectedYear();
    }

    private void loadExtras() {
        extras.setAll(extraDao.listAllExtras());
    }

    private void selectActiveSchoolYear() {
        SchoolYear activeYear = schoolYearDao.findActiveSchoolYear();

        if (activeYear != null) {
            selectSchoolYearById(activeYear.getId());
        } else if (!schoolYearComboBox.getItems().isEmpty()) {
            schoolYearComboBox.getSelectionModel().selectFirst();
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
        startMonthComboBox.getItems().clear();
        endMonthComboBox.getItems().clear();
        endMonthComboBox.getItems().add(NO_END_MONTH);

        SchoolYear selectedYear = schoolYearComboBox.getValue();
        if (selectedYear != null) {
            currentMonths.addAll(schoolYearMonthDao.listBySchoolYearId(selectedYear.getId()));

            for (SchoolYearMonth month : currentMonths) {
                startMonthComboBox.getItems().add(month.getMonthName());
                endMonthComboBox.getItems().add(month.getMonthName());
            }
        }

        endMonthComboBox.setValue(NO_END_MONTH);
    }

    private void fillForm(Extra extra) {
        if (extra == null) {
            return;
        }

        selectChildById(extra.getChildId());

        if (extra.getSchoolYearId() != null) {
            selectSchoolYearById(extra.getSchoolYearId());
        }

        selectMonthById(startMonthComboBox, extra.getStartMonthId());

        if (extra.getEndMonthId() == null) {
            endMonthComboBox.setValue(NO_END_MONTH);
        } else {
            selectMonthById(endMonthComboBox, extra.getEndMonthId());
        }

        labelComboBox.getEditor().setText(extra.getLabel());
        amountField.setText(extra.getAmount().toPlainString());
        typeComboBox.setValue(extra.getExtraType());
        noteArea.setText(valueOrEmpty(extra.getNotes()));
        updateButton.setDisable(false);
        deactivateButton.setDisable(!extra.isActive());
        reactivateButton.setDisable(extra.isActive());
    }

    private void selectChildById(int childId) {
        for (Child child : childComboBox.getItems()) {
            if (child.getId() == childId) {
                childComboBox.setValue(child);
                return;
            }
        }
    }

    private void selectMonthById(ComboBox<String> comboBox, Integer monthId) {
        SchoolYearMonth month = findMonthById(monthId);

        if (month != null) {
            comboBox.setValue(month.getMonthName());
        }
    }

    private Extra readExtraFromForm() {
        if (!validateForm()) {
            return null;
        }

        SchoolYearMonth startMonth = findMonthByName(startMonthComboBox.getValue());
        SchoolYearMonth endMonth = findMonthByName(endMonthComboBox.getValue());

        Extra extra = new Extra();
        extra.setChildId(childComboBox.getValue().getId());
        extra.setSchoolYearMonthId(startMonth.getId());
        extra.setStartMonthId(startMonth.getId());

        if (typeComboBox.getValue() == ExtraType.RECURRENT && endMonth != null) {
            extra.setEndMonthId(endMonth.getId());
        }

        extra.setLabel(labelComboBox.getEditor().getText().trim());
        extra.setExtraType(typeComboBox.getValue());
        extra.setAmount(parseAmount());
        extra.setNotes(emptyToNull(noteArea.getText()));
        return extra;
    }

    private boolean validateForm() {
        if (childComboBox.getValue() == null) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Veuillez s\u00e9lectionner un enfant.");
            return false;
        }

        if (schoolYearComboBox.getValue() == null) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Veuillez s\u00e9lectionner une ann\u00e9e scolaire.");
            return false;
        }

        String label = labelComboBox.getEditor().getText();
        if (label == null || label.trim().isEmpty()) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Le libell\u00e9 est obligatoire.");
            return false;
        }

        if (typeComboBox.getValue() == null) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Le type est obligatoire.");
            return false;
        }

        SchoolYearMonth startMonth = findMonthByName(startMonthComboBox.getValue());
        if (startMonth == null) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Le mois d\u00e9but est obligatoire.");
            return false;
        }

        SchoolYearMonth endMonth = findMonthByName(endMonthComboBox.getValue());
        if (typeComboBox.getValue() == ExtraType.RECURRENT && endMonth != null
                && endMonth.getDisplayOrder() < startMonth.getDisplayOrder()) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Le mois fin doit \u00eatre apr\u00e8s ou \u00e9gal au mois d\u00e9but.");
            return false;
        }

        BigDecimal amount = parseAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            AlertUtil.showWarning("Frais suppl\u00e9mentaires", "Le montant doit \u00eatre positif.");
            return false;
        }

        return true;
    }

    private SchoolYearMonth findMonthByName(String monthName) {
        if (monthName == null || monthName.equals(NO_END_MONTH)) {
            return null;
        }

        for (SchoolYearMonth month : currentMonths) {
            if (month.getMonthName().equals(monthName)) {
                return month;
            }
        }

        return null;
    }

    private SchoolYearMonth findMonthById(Integer monthId) {
        if (monthId == null) {
            return null;
        }

        for (SchoolYearMonth month : currentMonths) {
            if (month.getId() == monthId) {
                return month;
            }
        }

        return null;
    }

    private BigDecimal parseAmount() {
        String text = amountField.getText();

        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(text.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }

    private String valueOrEmpty(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    private void logAction(String action, int recordId, String details) {
        User currentUser = Session.getCurrentUser();
        Integer currentUserId = null;

        if (currentUser != null) {
            currentUserId = currentUser.getId();
        }

        activityLogDao.log(currentUserId, action, "extras", recordId, details);
    }
}
