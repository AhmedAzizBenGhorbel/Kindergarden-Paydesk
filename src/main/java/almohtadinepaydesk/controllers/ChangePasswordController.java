package almohtadinepaydesk.controllers;

import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.services.AuthService;
import almohtadinepaydesk.utils.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;

public class ChangePasswordController {

    private final AuthService authService = new AuthService();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    @FXML
    private PasswordField oldPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private void handleChangePassword() {
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (!validateForm(oldPassword, newPassword, confirmPassword)) {
            return;
        }

        if (!authService.changePassword(oldPassword, newPassword)) {
            AlertUtil.showError("Changer mot de passe", authService.getLastErrorMessage());
            return;
        }

        logPasswordChange();
        clearForm();
        AlertUtil.showInfo("Changer mot de passe", "Mot de passe chang\u00e9 avec succ\u00e8s.");
    }

    private boolean validateForm(String oldPassword, String newPassword, String confirmPassword) {
        if (isBlank(oldPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            AlertUtil.showWarning("Changer mot de passe", "Tous les champs sont obligatoires.");
            return false;
        }

        if (newPassword.length() < 6) {
            AlertUtil.showWarning("Changer mot de passe", "Le nouveau mot de passe doit contenir au moins 6 caract\u00e8res.");
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            AlertUtil.showWarning("Changer mot de passe", "Le nouveau mot de passe et la confirmation ne correspondent pas.");
            return false;
        }

        return true;
    }

    private void clearForm() {
        oldPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();
    }

    private void logPasswordChange() {
        User currentUser = Session.getCurrentUser();
        Integer currentUserId = null;

        if (currentUser != null) {
            currentUserId = currentUser.getId();
        }

        activityLogDao.log(currentUserId, "CHANGE_PASSWORD", "users", currentUserId, "Changement du mot de passe");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
