package almohtadinepaydesk.controllers;

import java.io.IOException;

import almohtadinepaydesk.models.User;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.services.AuthService;
import almohtadinepaydesk.utils.AlertUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MainController {

    private final AuthService authService = new AuthService();

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private void initialize() {
        User user = Session.getCurrentUser();

        if (user == null) {
            welcomeLabel.setText("Bienvenue");
            roleLabel.setText("R\u00f4le :");
            return;
        }

        String displayName = user.getFullName();
        if (displayName == null || displayName.isBlank()) {
            displayName = user.getUsername();
        }

        welcomeLabel.setText("Bienvenue, " + displayName);
        roleLabel.setText("R\u00f4le : " + user.getRole().getLabel());
    }

    @FXML
    private void handleLogout() {
        authService.logout();
        openLoginScreen();
    }

    private void openLoginScreen() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("D\u00e9connexion", "Impossible de revenir \u00e0 l'\u00e9cran de connexion.");
        }
    }
}
