package almohtadinepaydesk.controllers;

import java.io.IOException;

import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.models.UserRole;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.services.AuthService;
import almohtadinepaydesk.utils.AlertUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainLayoutController {

    private final AuthService authService = new AuthService();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    @FXML
    private Label connectedUserLabel;

    @FXML
    private Label connectedRoleLabel;

    @FXML
    private Button usersButton;

    @FXML
    private Button activityLogsButton;

    @FXML
    private StackPane contentArea;

    @FXML
    private void initialize() {
        showConnectedUser();
        loadPage("dashboard.fxml");
    }

    @FXML
    private void showDashboard() {
        loadPage("dashboard.fxml");
    }

    @FXML
    private void showChildren() {
        loadPage("children.fxml");
    }

    @FXML
    private void showSchoolYears() {
        loadPage("school-years.fxml");
    }

    @FXML
    private void showPayments() {
        loadPage("payments.fxml");
    }

    @FXML
    private void showExtras() {
        loadPage("extras.fxml");
    }

    @FXML
    private void showReceipts() {
        loadPage("receipts.fxml");
    }

    @FXML
    private void showBackups() {
        loadPage("backups.fxml");
    }

    @FXML
    private void showSettings() {
        loadPage("settings.fxml");
    }

    @FXML
    private void showUsers() {
        if (!Session.isAdmin()) {
            AlertUtil.showError("Gestion des comptes", "Acc\u00e8s r\u00e9serv\u00e9 aux administrateurs.");
            return;
        }

        loadPage("users.fxml");
    }

    @FXML
    private void showActivityLogs() {
        if (!Session.isAdmin()) {
            AlertUtil.showError("Journaux d'activit\u00e9", "Acc\u00e8s r\u00e9serv\u00e9 aux administrateurs.");
            return;
        }

        loadPage("activity-logs.fxml");
    }

    @FXML
    private void showChangePassword() {
        loadPage("change-password.fxml");
    }

    @FXML
    private void showAbout() {
        loadPage("about.fxml");
    }

    @FXML
    private void handleLogout() {
        boolean confirmed = AlertUtil.showConfirmation(
                "D\u00e9connexion",
                "Voulez-vous vraiment vous d\u00e9connecter ?");

        if (!confirmed) {
            return;
        }

        logLogout();
        authService.logout();
        openLoginScreen();
    }

    private void showConnectedUser() {
        User user = Session.getCurrentUser();

        if (user == null) {
            connectedUserLabel.setText("Utilisateur");
            connectedRoleLabel.setText("R\u00f4le :");
            usersButton.setVisible(false);
            usersButton.setManaged(false);
            activityLogsButton.setVisible(false);
            activityLogsButton.setManaged(false);
            return;
        }

        String fullName = user.getFullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = user.getUsername();
        }

        connectedUserLabel.setText(fullName);
        connectedRoleLabel.setText("R\u00f4le : " + user.getRole().getLabel());

        if (user.getRole() == UserRole.PERSONNEL) {
            usersButton.setVisible(false);
            usersButton.setManaged(false);
            activityLogsButton.setVisible(false);
            activityLogsButton.setManaged(false);
        }
    }

    private void logLogout() {
        User user = Session.getCurrentUser();

        if (user == null) {
            activityLogDao.log(null, "LOGOUT", "users", null, "D\u00e9connexion");
            return;
        }

        activityLogDao.log(user.getId(), "LOGOUT", "users", user.getId(), "D\u00e9connexion de " + user.getUsername());
    }

    private void loadPage(String fxmlFile) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/fxml/" + fxmlFile));
            contentArea.getChildren().setAll(page);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            AlertUtil.showError("Navigation", "Impossible de charger cette page.");
        }
    }

    private void openLoginScreen() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("D\u00e9connexion", "Impossible de revenir \u00e0 l'\u00e9cran de connexion.");
        }
    }
}
