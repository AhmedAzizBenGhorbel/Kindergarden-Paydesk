package almohtadinepaydesk.utils;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Central helper for JavaFX alert dialogs.
 * Using one utility keeps messages and dialog behavior consistent across the app.
 */
public class AlertUtil {

    /**
     * Shows an information dialog.
     *
     * @param title dialog title
     * @param message message shown to the user
     */
    public static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    /**
     * Shows a warning dialog.
     *
     * @param title dialog title
     * @param message message shown to the user
     */
    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    /**
     * Shows an error dialog.
     *
     * @param title dialog title
     * @param message message shown to the user
     */
    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    /**
     * Shows a confirmation dialog and returns the user's choice.
     *
     * @param title dialog title
     * @param message message shown to the user
     * @return true when the user confirms, false otherwise
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Shared internal method for simple alert dialogs.
     */
    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Utility class, so it should not be instantiated.
     */
    private AlertUtil() {
    }
}
