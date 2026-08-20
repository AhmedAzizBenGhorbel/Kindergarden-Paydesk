package almohtadinepaydesk.controllers;

import java.io.File;

import almohtadinepaydesk.config.AppConfig;
import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.dao.SchoolYearDao;
import almohtadinepaydesk.dao.SettingDao;
import almohtadinepaydesk.models.SchoolYear;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.utils.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class SettingsController {

    private static final String KEY_GARDERIE_NAME = "garderie_name";
    private static final String KEY_PAYMENT_DEADLINE_DAY = "payment_deadline_day";
    private static final String KEY_ACTIVE_SCHOOL_YEAR_ID = "active_school_year_id";
    private static final String KEY_BACKUP_FOLDER = "backup_folder";
    private static final String KEY_LOGO_PATH = "logo_path";
    private static final String KEY_CONTACT_INFORMATION = "contact_information";

    private final SettingDao settingDao = new SettingDao();
    private final SchoolYearDao schoolYearDao = new SchoolYearDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    @FXML
    private TextField garderieNameField;

    @FXML
    private ComboBox<SchoolYear> activeSchoolYearComboBox;

    @FXML
    private TextField paymentDeadlineDayField;

    @FXML
    private TextField backupFolderField;

    @FXML
    private TextField logoPathField;

    @FXML
    private TextArea contactInformationArea;

    @FXML
    private void initialize() {
        loadSchoolYears();
        loadSettings();
    }

    @FXML
    private void handleChooseBackupFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choisir un dossier");

        File currentFolder = getCurrentBackupFolder();
        if (currentFolder != null) {
            directoryChooser.setInitialDirectory(currentFolder);
        }

        Stage stage = (Stage) backupFolderField.getScene().getWindow();
        File selectedFolder = directoryChooser.showDialog(stage);

        if (selectedFolder != null) {
            backupFolderField.setText(selectedFolder.getAbsolutePath());
        }
    }

    @FXML
    private void handleSaveSettings() {
        if (!validateSettings()) {
            return;
        }

        boolean confirmed = AlertUtil.showConfirmation(
                "Param\u00e8tres",
                "Voulez-vous enregistrer les param\u00e8tres ?");

        if (!confirmed) {
            return;
        }

        SchoolYear selectedSchoolYear = activeSchoolYearComboBox.getValue();

        if (!schoolYearDao.setActiveSchoolYear(selectedSchoolYear.getId())) {
            AlertUtil.showError("Param\u00e8tres", schoolYearDao.getLastErrorMessage());
            return;
        }

        if (!saveAllSettings(selectedSchoolYear)) {
            return;
        }

        logSettingsChange();
        AlertUtil.showInfo("Param\u00e8tres", "Param\u00e8tres enregistr\u00e9s avec succ\u00e8s.");
        loadSchoolYears();
        selectSchoolYearById(selectedSchoolYear.getId());
    }

    private void loadSchoolYears() {
        activeSchoolYearComboBox.getItems().setAll(schoolYearDao.listAllSchoolYears());

        SchoolYear activeSchoolYear = schoolYearDao.findActiveSchoolYear();
        if (activeSchoolYear != null) {
            selectSchoolYearById(activeSchoolYear.getId());
        } else if (!activeSchoolYearComboBox.getItems().isEmpty()) {
            activeSchoolYearComboBox.getSelectionModel().selectFirst();
        }
    }

    private void loadSettings() {
        garderieNameField.setText(settingDao.findValueByKey(KEY_GARDERIE_NAME, AppConfig.ORGANIZATION_NAME));
        paymentDeadlineDayField.setText(settingDao.findValueByKey(KEY_PAYMENT_DEADLINE_DAY, "10"));
        backupFolderField.setText(settingDao.findValueByKey(KEY_BACKUP_FOLDER, ""));
        logoPathField.setText(settingDao.findValueByKey(KEY_LOGO_PATH, ""));
        contactInformationArea.setText(settingDao.findValueByKey(KEY_CONTACT_INFORMATION, ""));
    }

    private boolean saveAllSettings(SchoolYear selectedSchoolYear) {
        return saveSetting(KEY_GARDERIE_NAME, garderieNameField.getText().trim(), "Nom de la garderie")
                && saveSetting(KEY_PAYMENT_DEADLINE_DAY, paymentDeadlineDayField.getText().trim(), "Jour limite de paiement")
                && saveSetting(KEY_ACTIVE_SCHOOL_YEAR_ID, String.valueOf(selectedSchoolYear.getId()), "Ann\u00e9e scolaire active")
                && saveSetting(KEY_BACKUP_FOLDER, emptyToBlank(backupFolderField.getText()), "Dossier de sauvegarde")
                && saveSetting(KEY_LOGO_PATH, emptyToBlank(logoPathField.getText()), "Chemin du logo")
                && saveSetting(KEY_CONTACT_INFORMATION, emptyToBlank(contactInformationArea.getText()), "Informations de contact");
    }

    private boolean saveSetting(String key, String value, String description) {
        if (!settingDao.saveSetting(key, value, description)) {
            AlertUtil.showError("Param\u00e8tres", settingDao.getLastErrorMessage());
            return false;
        }

        return true;
    }

    private boolean validateSettings() {
        if (garderieNameField.getText() == null || garderieNameField.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Param\u00e8tres", "Le nom de la garderie est obligatoire.");
            return false;
        }

        if (activeSchoolYearComboBox.getValue() == null) {
            AlertUtil.showWarning("Param\u00e8tres", "Veuillez choisir une ann\u00e9e scolaire active.");
            return false;
        }

        int paymentDeadlineDay;
        try {
            paymentDeadlineDay = Integer.parseInt(paymentDeadlineDayField.getText().trim());
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Param\u00e8tres", "Le jour limite de paiement doit \u00eatre un nombre entre 1 et 28.");
            return false;
        }

        if (paymentDeadlineDay < 1 || paymentDeadlineDay > 28) {
            AlertUtil.showWarning("Param\u00e8tres", "Le jour limite de paiement doit \u00eatre entre 1 et 28.");
            return false;
        }

        return true;
    }

    private File getCurrentBackupFolder() {
        String path = backupFolderField.getText();

        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        File folder = new File(path.trim());
        if (folder.exists() && folder.isDirectory()) {
            return folder;
        }

        return null;
    }

    private void selectSchoolYearById(int schoolYearId) {
        for (SchoolYear schoolYear : activeSchoolYearComboBox.getItems()) {
            if (schoolYear.getId() == schoolYearId) {
                activeSchoolYearComboBox.setValue(schoolYear);
                return;
            }
        }
    }

    private String emptyToBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        return value.trim();
    }

    private void logSettingsChange() {
        User currentUser = Session.getCurrentUser();
        Integer currentUserId = null;

        if (currentUser != null) {
            currentUserId = currentUser.getId();
        }

        activityLogDao.log(currentUserId, "UPDATE_SETTINGS", "settings", null, "Modification des param\u00e8tres");
    }
}
