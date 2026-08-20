package almohtadinepaydesk.controllers;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.dao.SchoolYearDao;
import almohtadinepaydesk.dao.SchoolYearMonthDao;
import almohtadinepaydesk.models.SchoolYear;
import almohtadinepaydesk.models.SchoolYearMonth;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.utils.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class SchoolYearController {

    private final SchoolYearDao schoolYearDao = new SchoolYearDao();
    private final SchoolYearMonthDao schoolYearMonthDao = new SchoolYearMonthDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final ObservableList<SchoolYear> schoolYears = FXCollections.observableArrayList();
    private final Map<CheckBox, SchoolYearMonth> monthCheckBoxes = new LinkedHashMap<>();

    @FXML
    private TableView<SchoolYear> schoolYearsTable;

    @FXML
    private TableColumn<SchoolYear, Integer> idColumn;

    @FXML
    private TableColumn<SchoolYear, String> nameColumn;

    @FXML
    private TableColumn<SchoolYear, String> activeColumn;

    @FXML
    private TextField schoolYearField;

    @FXML
    private Label selectedYearLabel;

    @FXML
    private VBox monthsBox;

    @FXML
    private Button setActiveButton;

    @FXML
    private Button saveMonthsButton;

    @FXML
    private void initialize() {
        configureTable();
        schoolYearsTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldYear, selectedYear) -> loadMonthsForSelectedYear(selectedYear));
        loadSchoolYears();
    }

    @FXML
    private void handleAddSchoolYear() {
        String schoolYearName = schoolYearField.getText();

        if (!isValidSchoolYearName(schoolYearName)) {
            AlertUtil.showWarning("Ann\u00e9es scolaires", "Format invalide. Exemple correct: 2025/2026.");
            return;
        }

        schoolYearName = schoolYearName.trim();

        if (schoolYearDao.existsByName(schoolYearName)) {
            AlertUtil.showWarning("Ann\u00e9es scolaires", "Cette ann\u00e9e scolaire existe d\u00e9j\u00e0.");
            return;
        }

        int startYear = Integer.parseInt(schoolYearName.substring(0, 4));
        int endYear = Integer.parseInt(schoolYearName.substring(5, 9));

        SchoolYear schoolYear = new SchoolYear();
        schoolYear.setName(schoolYearName);
        schoolYear.setStartDate(LocalDate.of(startYear, 9, 1));
        schoolYear.setEndDate(LocalDate.of(endYear, 6, 30));
        schoolYear.setActive(false);

        if (schoolYearDao.createSchoolYear(schoolYear)) {
            schoolYearMonthDao.createDefaultMonthsIfMissing(schoolYear.getId());
            logAction("CREATE_SCHOOL_YEAR", schoolYear.getId(),
                    "Ajout de l'ann\u00e9e scolaire: " + schoolYear.getName());
            AlertUtil.showInfo("Ann\u00e9es scolaires", "Ann\u00e9e scolaire ajout\u00e9e avec succ\u00e8s.");
            schoolYearField.clear();
            loadSchoolYears();
            selectSchoolYearById(schoolYear.getId());
        } else {
            AlertUtil.showError("Ann\u00e9es scolaires", schoolYearDao.getLastErrorMessage());
        }
    }

    @FXML
    private void handleSetActiveSchoolYear() {
        SchoolYear selectedYear = schoolYearsTable.getSelectionModel().getSelectedItem();

        if (selectedYear == null) {
            AlertUtil.showWarning("Ann\u00e9es scolaires", "Veuillez s\u00e9lectionner une ann\u00e9e scolaire.");
            return;
        }

        if (schoolYearDao.setActiveSchoolYear(selectedYear.getId())) {
            logAction("SET_ACTIVE_SCHOOL_YEAR", selectedYear.getId(),
                    "Ann\u00e9e scolaire active: " + selectedYear.getName());
            AlertUtil.showInfo("Ann\u00e9es scolaires", "Ann\u00e9e scolaire d\u00e9finie comme active.");
            loadSchoolYears();
            selectSchoolYearById(selectedYear.getId());
        } else {
            AlertUtil.showError("Ann\u00e9es scolaires", schoolYearDao.getLastErrorMessage());
        }
    }

    @FXML
    private void handleSaveActiveMonths() {
        SchoolYear selectedYear = schoolYearsTable.getSelectionModel().getSelectedItem();

        if (selectedYear == null) {
            AlertUtil.showWarning("Ann\u00e9es scolaires", "Veuillez s\u00e9lectionner une ann\u00e9e scolaire.");
            return;
        }

        int activeCount = 0;
        for (Map.Entry<CheckBox, SchoolYearMonth> entry : monthCheckBoxes.entrySet()) {
            boolean active = entry.getKey().isSelected();
            entry.getValue().setActive(active);

            if (active) {
                activeCount++;
            }
        }

        if (activeCount == 0) {
            AlertUtil.showWarning("Ann\u00e9es scolaires", "Au moins un mois actif est obligatoire.");
            return;
        }

        if (schoolYearMonthDao.updateActiveMonths(List.copyOf(monthCheckBoxes.values()))) {
            logAction("UPDATE_ACTIVE_MONTHS", selectedYear.getId(),
                    "Modification des mois actifs pour: " + selectedYear.getName());
            AlertUtil.showInfo("Ann\u00e9es scolaires", "Mois actifs enregistr\u00e9s avec succ\u00e8s.");
            loadMonthsForSelectedYear(selectedYear);
        } else {
            AlertUtil.showError("Ann\u00e9es scolaires", schoolYearMonthDao.getLastErrorMessage());
        }
    }

    private void configureTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        activeColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().isActive()) {
                return new SimpleStringProperty("Oui");
            }

            return new SimpleStringProperty("Non");
        });

        schoolYearsTable.setItems(schoolYears);
    }

    private void loadSchoolYears() {
        schoolYears.setAll(schoolYearDao.listAllSchoolYears());

        if (schoolYears.isEmpty()) {
            loadMonthsForSelectedYear(null);
        } else if (schoolYearsTable.getSelectionModel().getSelectedItem() == null) {
            schoolYearsTable.getSelectionModel().selectFirst();
        }
    }

    private void selectSchoolYearById(int schoolYearId) {
        for (SchoolYear schoolYear : schoolYears) {
            if (schoolYear.getId() == schoolYearId) {
                schoolYearsTable.getSelectionModel().select(schoolYear);
                return;
            }
        }
    }

    private void loadMonthsForSelectedYear(SchoolYear schoolYear) {
        monthsBox.getChildren().clear();
        monthCheckBoxes.clear();

        if (schoolYear == null) {
            selectedYearLabel.setText("Aucune ann\u00e9e scolaire s\u00e9lectionn\u00e9e");
            setActiveButton.setDisable(true);
            saveMonthsButton.setDisable(true);
            return;
        }

        selectedYearLabel.setText("Mois actifs - " + schoolYear.getName());
        setActiveButton.setDisable(false);
        saveMonthsButton.setDisable(false);

        List<SchoolYearMonth> months = schoolYearMonthDao.listBySchoolYearId(schoolYear.getId());

        for (SchoolYearMonth month : months) {
            CheckBox checkBox = new CheckBox(month.getMonthName());
            checkBox.setSelected(month.isActive());
            checkBox.getStyleClass().add("month-check-box");

            monthCheckBoxes.put(checkBox, month);
            monthsBox.getChildren().add(checkBox);
        }
    }

    private boolean isValidSchoolYearName(String value) {
        if (value == null || !value.trim().matches("\\d{4}/\\d{4}")) {
            return false;
        }

        int startYear = Integer.parseInt(value.trim().substring(0, 4));
        int endYear = Integer.parseInt(value.trim().substring(5, 9));
        return endYear == startYear + 1;
    }

    private void logAction(String action, int recordId, String details) {
        User currentUser = Session.getCurrentUser();
        Integer currentUserId = null;

        if (currentUser != null) {
            currentUserId = currentUser.getId();
        }

        activityLogDao.log(currentUserId, action, "school_years", recordId, details);
    }
}
