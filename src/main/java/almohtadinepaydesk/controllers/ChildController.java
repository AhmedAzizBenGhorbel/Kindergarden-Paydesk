package almohtadinepaydesk.controllers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.dao.ChildDao;
import almohtadinepaydesk.models.Child;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.utils.AlertUtil;
import almohtadinepaydesk.utils.CurrencyUtil;
import almohtadinepaydesk.utils.DateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class ChildController {

    private static final String ALL_GROUPS = "Toutes les classes";
    private static final String ALL_STATUS = "Tous";
    private static final String ACTIVE_STATUS = "Actifs";
    private static final String INACTIVE_STATUS = "Inactifs";

    private final ChildDao childDao = new ChildDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final ObservableList<Child> allChildren = FXCollections.observableArrayList();
    private final ObservableList<Child> filteredChildren = FXCollections.observableArrayList();

    @FXML
    private TableView<Child> childrenTable;

    @FXML
    private TableColumn<Child, Integer> idColumn;

    @FXML
    private TableColumn<Child, String> fullNameColumn;

    @FXML
    private TableColumn<Child, String> classGroupColumn;

    @FXML
    private TableColumn<Child, String> parentNameColumn;

    @FXML
    private TableColumn<Child, String> parentPhoneColumn;

    @FXML
    private TableColumn<Child, String> registrationDateColumn;

    @FXML
    private TableColumn<Child, String> monthlyFeeColumn;

    @FXML
    private TableColumn<Child, String> statusColumn;

    @FXML
    private TableColumn<Child, String> noteColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> classFilterComboBox;

    @FXML
    private ComboBox<String> statusFilterComboBox;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField classGroupField;

    @FXML
    private TextField parentNameField;

    @FXML
    private TextField parentPhoneField;

    @FXML
    private DatePicker registrationDatePicker;

    @FXML
    private TextField monthlyFeeField;

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
        configureFilters();
        loadChildren();
        resetForm();

        childrenTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldChild, selectedChild) -> fillForm(selectedChild));
    }

    @FXML
    private void handleAddChild() {
        Child child = readChildFromForm();

        if (child == null) {
            return;
        }

        child.setActive(true);

        if (childDao.createChild(child)) {
            logAction("CREATE_CHILD", child.getId(), "Ajout de l'enfant: " + child.getFullName());
            AlertUtil.showInfo("Enfants", "Enfant ajout\u00e9 avec succ\u00e8s.");
            loadChildren();
            resetForm();
        } else {
            AlertUtil.showError("Enfants", childDao.getLastErrorMessage());
        }
    }

    @FXML
    private void handleUpdateChild() {
        Child selectedChild = childrenTable.getSelectionModel().getSelectedItem();

        if (selectedChild == null) {
            AlertUtil.showWarning("Enfants", "Veuillez s\u00e9lectionner un enfant \u00e0 modifier.");
            return;
        }

        Child formChild = readChildFromForm();
        if (formChild == null) {
            return;
        }

        selectedChild.setFullName(formChild.getFullName());
        selectedChild.setClassGroup(formChild.getClassGroup());
        selectedChild.setParentFullName(formChild.getParentFullName());
        selectedChild.setParentPhone(formChild.getParentPhone());
        selectedChild.setRegistrationDate(formChild.getRegistrationDate());
        selectedChild.setMonthlyFee(formChild.getMonthlyFee());
        selectedChild.setNotes(formChild.getNotes());

        if (childDao.updateChild(selectedChild)) {
            logAction("UPDATE_CHILD", selectedChild.getId(), "Modification de l'enfant: " + selectedChild.getFullName());
            AlertUtil.showInfo("Enfants", "Enfant modifi\u00e9 avec succ\u00e8s.");
            loadChildren();
            resetForm();
        } else {
            AlertUtil.showError("Enfants", childDao.getLastErrorMessage());
        }
    }

    @FXML
    private void handleDeactivateChild() {
        Child selectedChild = childrenTable.getSelectionModel().getSelectedItem();

        if (selectedChild == null) {
            AlertUtil.showWarning("Enfants", "Veuillez s\u00e9lectionner un enfant \u00e0 d\u00e9sactiver.");
            return;
        }

        if (!selectedChild.isActive()) {
            AlertUtil.showWarning("Enfants", "Cet enfant est d\u00e9j\u00e0 inactif.");
            return;
        }

        if (childDao.setActive(selectedChild.getId(), false)) {
            logAction("DEACTIVATE_CHILD", selectedChild.getId(),
                    "D\u00e9sactivation de l'enfant: " + selectedChild.getFullName());
            AlertUtil.showInfo("Enfants", "Enfant d\u00e9sactiv\u00e9 avec succ\u00e8s.");
            loadChildren();
            resetForm();
        } else {
            AlertUtil.showError("Enfants", childDao.getLastErrorMessage());
        }
    }

    @FXML
    private void handleReactivateChild() {
        Child selectedChild = childrenTable.getSelectionModel().getSelectedItem();

        if (selectedChild == null) {
            AlertUtil.showWarning("Enfants", "Veuillez s\u00e9lectionner un enfant \u00e0 r\u00e9activer.");
            return;
        }

        if (selectedChild.isActive()) {
            AlertUtil.showWarning("Enfants", "Cet enfant est d\u00e9j\u00e0 actif.");
            return;
        }

        if (childDao.setActive(selectedChild.getId(), true)) {
            logAction("REACTIVATE_CHILD", selectedChild.getId(),
                    "R\u00e9activation de l'enfant: " + selectedChild.getFullName());
            AlertUtil.showInfo("Enfants", "Enfant r\u00e9activ\u00e9 avec succ\u00e8s.");
            loadChildren();
            resetForm();
        } else {
            AlertUtil.showError("Enfants", childDao.getLastErrorMessage());
        }
    }

    @FXML
    private void resetForm() {
        childrenTable.getSelectionModel().clearSelection();
        fullNameField.clear();
        classGroupField.clear();
        parentNameField.clear();
        parentPhoneField.clear();
        registrationDatePicker.setValue(LocalDate.now());
        monthlyFeeField.clear();
        noteArea.clear();
        updateButton.setDisable(true);
        deactivateButton.setDisable(true);
        reactivateButton.setDisable(true);
    }

    private void configureTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        fullNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getFullName())));
        classGroupColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getClassGroup())));
        parentNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getParentFullName())));
        parentPhoneColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getParentPhone())));
        registrationDateColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(DateUtil.formatDate(cellData.getValue().getRegistrationDate())));
        monthlyFeeColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(CurrencyUtil.formatAmount(cellData.getValue().getMonthlyFee())));
        statusColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().isActive()) {
                return new SimpleStringProperty("Actif");
            }
            return new SimpleStringProperty("Inactif");
        });
        noteColumn.setCellValueFactory(cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getNotes())));

        childrenTable.setItems(filteredChildren);
    }

    private void configureFilters() {
        statusFilterComboBox.getItems().setAll(ALL_STATUS, ACTIVE_STATUS, INACTIVE_STATUS);
        statusFilterComboBox.setValue(ALL_STATUS);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        classFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void loadChildren() {
        String selectedGroup = classFilterComboBox.getValue();
        allChildren.setAll(childDao.listAllChildren());
        reloadClassFilter(selectedGroup);
        applyFilters();
    }

    private void reloadClassFilter(String selectedGroup) {
        List<String> groups = new ArrayList<>();
        groups.add(ALL_GROUPS);

        for (Child child : allChildren) {
            String group = child.getClassGroup();
            if (group != null && !group.isBlank() && !groups.contains(group)) {
                groups.add(group);
            }
        }

        classFilterComboBox.getItems().setAll(groups);

        if (selectedGroup != null && groups.contains(selectedGroup)) {
            classFilterComboBox.setValue(selectedGroup);
        } else {
            classFilterComboBox.setValue(ALL_GROUPS);
        }
    }

    private void applyFilters() {
        filteredChildren.clear();

        String searchText = searchField.getText();
        String selectedGroup = classFilterComboBox.getValue();
        String selectedStatus = statusFilterComboBox.getValue();

        for (Child child : allChildren) {
            if (!matchesSearch(child, searchText)) {
                continue;
            }

            if (!matchesGroup(child, selectedGroup)) {
                continue;
            }

            if (!matchesStatus(child, selectedStatus)) {
                continue;
            }

            filteredChildren.add(child);
        }
    }

    private boolean matchesSearch(Child child, String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return true;
        }

        String fullName = valueOrEmpty(child.getFullName()).toLowerCase();
        return fullName.contains(searchText.trim().toLowerCase());
    }

    private boolean matchesGroup(Child child, String selectedGroup) {
        if (selectedGroup == null || selectedGroup.equals(ALL_GROUPS)) {
            return true;
        }

        return selectedGroup.equals(child.getClassGroup());
    }

    private boolean matchesStatus(Child child, String selectedStatus) {
        if (selectedStatus == null || selectedStatus.equals(ALL_STATUS)) {
            return true;
        }

        if (selectedStatus.equals(ACTIVE_STATUS)) {
            return child.isActive();
        }

        return !child.isActive();
    }

    private void fillForm(Child child) {
        if (child == null) {
            return;
        }

        fullNameField.setText(valueOrEmpty(child.getFullName()));
        classGroupField.setText(valueOrEmpty(child.getClassGroup()));
        parentNameField.setText(valueOrEmpty(child.getParentFullName()));
        parentPhoneField.setText(valueOrEmpty(child.getParentPhone()));
        registrationDatePicker.setValue(child.getRegistrationDate());

        if (child.getMonthlyFee() == null) {
            monthlyFeeField.clear();
        } else {
            monthlyFeeField.setText(child.getMonthlyFee().toPlainString());
        }

        noteArea.setText(valueOrEmpty(child.getNotes()));
        updateButton.setDisable(false);
        deactivateButton.setDisable(!child.isActive());
        reactivateButton.setDisable(child.isActive());
    }

    private Child readChildFromForm() {
        if (!validateForm()) {
            return null;
        }

        Child child = new Child();
        child.setFullName(fullNameField.getText().trim());
        child.setClassGroup(emptyToNull(classGroupField.getText()));
        child.setParentFullName(emptyToNull(parentNameField.getText()));
        child.setParentPhone(emptyToNull(parentPhoneField.getText()));
        child.setRegistrationDate(registrationDatePicker.getValue());
        child.setMonthlyFee(parseMonthlyFee());
        child.setNotes(emptyToNull(noteArea.getText()));
        return child;
    }

    private boolean validateForm() {
        if (fullNameField.getText() == null || fullNameField.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Enfants", "Le nom complet est obligatoire.");
            return false;
        }

        if (registrationDatePicker.getValue() == null) {
            AlertUtil.showWarning("Enfants", "La date d'inscription est obligatoire.");
            return false;
        }

        BigDecimal monthlyFee = parseMonthlyFee();
        if (monthlyFee == null || monthlyFee.compareTo(BigDecimal.ZERO) <= 0) {
            AlertUtil.showWarning("Enfants", "Les frais mensuels doivent \u00eatre un montant positif.");
            return false;
        }

        String phone = parentPhoneField.getText();
        if (phone != null && !phone.isBlank() && !phone.matches("[0-9+()\\s-]{6,30}")) {
            AlertUtil.showWarning("Enfants", "Le num\u00e9ro de t\u00e9l\u00e9phone n'est pas valide.");
            return false;
        }

        return true;
    }

    private BigDecimal parseMonthlyFee() {
        String text = monthlyFeeField.getText();

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

        activityLogDao.log(currentUserId, action, "children", recordId, details);
    }
}
