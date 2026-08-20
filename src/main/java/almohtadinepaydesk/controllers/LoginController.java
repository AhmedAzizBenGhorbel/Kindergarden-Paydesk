package almohtadinepaydesk.controllers;

import almohtadinepaydesk.services.AuthService;
import almohtadinepaydesk.utils.AlertUtil;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    private final AuthService authService = new AuthService();

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            AlertUtil.showWarning("Connexion", "Veuillez remplir le nom d'utilisateur et le mot de passe.");
            return;
        }

        boolean loggedIn = authService.login(username.trim(), password);

        if (loggedIn) {
            openMainScreen();
        } else {
            AlertUtil.showError("Connexion", authService.getLastErrorMessage());
        }
    }

    private void openMainScreen() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/main-layout.fxml"));
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Connexion", "Impossible d'ouvrir l'\u00e9cran principal.");
        }
    }
}
