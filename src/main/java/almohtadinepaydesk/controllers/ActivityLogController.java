package almohtadinepaydesk.controllers;

import java.time.format.DateTimeFormatter;

import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.models.ActivityLog;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.utils.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class ActivityLogController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final ObservableList<ActivityLog> activityLogs = FXCollections.observableArrayList();

    @FXML
    private TableView<ActivityLog> activityLogsTable;

    @FXML
    private TableColumn<ActivityLog, String> dateColumn;

    @FXML
    private TableColumn<ActivityLog, String> userColumn;

    @FXML
    private TableColumn<ActivityLog, String> actionColumn;

    @FXML
    private TableColumn<ActivityLog, String> detailsColumn;

    @FXML
    private void initialize() {
        configureTable();

        if (!Session.isAdmin()) {
            AlertUtil.showError("Journaux d'activit\u00e9", "Acc\u00e8s r\u00e9serv\u00e9 aux administrateurs.");
            return;
        }

        loadActivityLogs();
    }

    @FXML
    private void handleRefresh() {
        if (!Session.isAdmin()) {
            AlertUtil.showError("Journaux d'activit\u00e9", "Acc\u00e8s r\u00e9serv\u00e9 aux administrateurs.");
            return;
        }

        loadActivityLogs();
    }

    private void configureTable() {
        dateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(formatDate(cellData.getValue())));
        userColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getUserFullName())));
        actionColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(formatAction(cellData.getValue().getAction())));
        detailsColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(valueOrEmpty(cellData.getValue().getDetails())));

        activityLogsTable.setItems(activityLogs);
    }

    private void loadActivityLogs() {
        activityLogs.setAll(activityLogDao.listRecentLogs());
    }

    private String formatDate(ActivityLog log) {
        if (log.getActivityDate() == null) {
            return "";
        }

        return log.getActivityDate().format(DATE_TIME_FORMATTER);
    }

    private String formatAction(String action) {
        if (action == null) {
            return "";
        }

        return switch (action) {
            case "LOGIN_SUCCESS" -> "Connexion r\u00e9ussie";
            case "LOGIN_FAILURE" -> "\u00c9chec connexion";
            case "LOGOUT" -> "D\u00e9connexion";
            case "CHANGE_PASSWORD" -> "Changement mot de passe";
            case "CREATE_CHILD" -> "Ajout enfant";
            case "UPDATE_CHILD" -> "Modification enfant";
            case "DEACTIVATE_CHILD" -> "D\u00e9sactivation enfant";
            case "REACTIVATE_CHILD" -> "R\u00e9activation enfant";
            case "CREATE_USER" -> "Ajout compte";
            case "UPDATE_USER" -> "Modification compte";
            case "DEACTIVATE_USER" -> "D\u00e9sactivation compte";
            case "REACTIVATE_USER" -> "R\u00e9activation compte";
            case "CREATE_EXTRA" -> "Ajout frais";
            case "UPDATE_EXTRA" -> "Modification frais";
            case "DEACTIVATE_EXTRA" -> "D\u00e9sactivation frais";
            case "REACTIVATE_EXTRA" -> "R\u00e9activation frais";
            case "CREATE_PAYMENT_ENTRY" -> "Ajout paiement";
            case "CREATE_RECEIPT" -> "Cr\u00e9ation donn\u00e9es re\u00e7u";
            case "GENERATE_MONTHLY_RECORDS" -> "G\u00e9n\u00e9ration paiements mensuels";
            case "RECALCULATE_PAYMENT_STATUSES" -> "Recalcul statuts";
            case "RUN_BACKUP" -> "Sauvegarde";
            case "UPDATE_SETTINGS" -> "Modification param\u00e8tres";
            default -> action;
        };
    }

    private String valueOrEmpty(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }
}
