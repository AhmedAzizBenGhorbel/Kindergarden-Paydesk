package almohtadinepaydesk.controllers;

import java.io.File;
import java.time.format.DateTimeFormatter;

import almohtadinepaydesk.backup.BackupService;
import almohtadinepaydesk.backup.BackupService.BackupResult;
import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.dao.BackupLogDao;
import almohtadinepaydesk.dao.SettingDao;
import almohtadinepaydesk.models.BackupLog;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.utils.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class BackupController {

    private static final String KEY_BACKUP_FOLDER = "backup_folder";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SettingDao settingDao = new SettingDao();
    private final BackupLogDao backupLogDao = new BackupLogDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final BackupService backupService = new BackupService();
    private final ObservableList<BackupLog> backupLogs = FXCollections.observableArrayList();

    @FXML
    private TextField backupFolderField;

    @FXML
    private TableView<BackupLog> backupHistoryTable;

    @FXML
    private TableColumn<BackupLog, String> dateColumn;

    @FXML
    private TableColumn<BackupLog, String> fileNameColumn;

    @FXML
    private TableColumn<BackupLog, String> folderColumn;

    @FXML
    private TableColumn<BackupLog, String> statusColumn;

    @FXML
    private TableColumn<BackupLog, String> messageColumn;

    @FXML
    private void initialize() {
        configureTable();
        loadBackupFolder();
        loadBackupHistory();
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
            settingDao.saveSetting(KEY_BACKUP_FOLDER, selectedFolder.getAbsolutePath(), "Dossier de sauvegarde");
        }
    }

    @FXML
    private void handleRunBackup() {
        BackupResult result = backupService.runBackup(backupFolderField.getText());
        BackupLog backupLog = createBackupLog(result);

        if (!backupLogDao.createBackupLog(backupLog)) {
            AlertUtil.showError("Sauvegardes", backupLogDao.getLastErrorMessage());
        }

        logBackupAction(result);
        loadBackupHistory();

        if (result.isSuccess()) {
            AlertUtil.showInfo("Sauvegardes", result.getMessage());
        } else {
            AlertUtil.showError("Sauvegardes", result.getMessage());
        }
    }

    private void configureTable() {
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(formatDate(cellData.getValue())));
        fileNameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getFileName())));
        folderColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getFolderPath())));
        statusColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(formatStatus(cellData.getValue().getStatus())));
        messageColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getMessage())));

        backupHistoryTable.setItems(backupLogs);
    }

    private void loadBackupFolder() {
        backupFolderField.setText(settingDao.findValueByKey(KEY_BACKUP_FOLDER, ""));
    }

    private void loadBackupHistory() {
        backupLogs.setAll(backupLogDao.listBackupLogs());
    }

    private BackupLog createBackupLog(BackupResult result) {
        BackupLog backupLog = new BackupLog();
        backupLog.setFileName(result.getFileName());
        backupLog.setFolderPath(result.getFolderPath());
        backupLog.setStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
        backupLog.setMessage(result.getMessage());
        backupLog.setCreatedByUserId(getCurrentUserId());
        return backupLog;
    }

    private void logBackupAction(BackupResult result) {
        String details = result.getMessage();

        if (result.getFileName() != null && !result.getFileName().isBlank()) {
            details = result.getFileName() + " - " + result.getMessage();
        }

        activityLogDao.log(getCurrentUserId(), "RUN_BACKUP", "backup_logs", null, details);
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

    private Integer getCurrentUserId() {
        User currentUser = Session.getCurrentUser();

        if (currentUser == null) {
            return null;
        }

        return currentUser.getId();
    }

    private String formatDate(BackupLog backupLog) {
        if (backupLog.getBackupDate() == null) {
            return "";
        }

        return backupLog.getBackupDate().format(DATE_TIME_FORMATTER);
    }

    private String formatStatus(String status) {
        if ("SUCCESS".equals(status)) {
            return "Succ\u00e8s";
        }

        if ("FAILED".equals(status)) {
            return "\u00c9chec";
        }

        return valueOrEmpty(status);
    }

    private String valueOrEmpty(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }
}
