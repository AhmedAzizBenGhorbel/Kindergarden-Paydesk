package almohtadinepaydesk.controllers;

import java.util.List;

import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.dao.UserDao;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.models.UserRole;
import almohtadinepaydesk.security.PasswordUtil;
import almohtadinepaydesk.security.Session;
import almohtadinepaydesk.utils.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class UserManagementController {

    private final UserDao userDao = new UserDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();
    private final ObservableList<User> users = FXCollections.observableArrayList();

    @FXML
    private BorderPane managementPane;

    @FXML
    private VBox accessDeniedPane;

    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Integer> idColumn;

    @FXML
    private TableColumn<User, String> fullNameColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, String> stateColumn;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private Label formTitleLabel;

    @FXML
    private Button updateButton;

    @FXML
    private Button deactivateButton;

    @FXML
    private Button reactivateButton;

    @FXML
    private void initialize() {
        if (!Session.isAdmin()) {
            showAccessDenied();
            return;
        }

        configureTable();
        loadUsers();
        clearForm();

        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldUser, selectedUser) -> fillForm(selectedUser));
    }

    @FXML
    private void handleAddUser() {
        if (!validateRequiredFields(true)) {
            return;
        }

        String username = usernameField.getText().trim();
        if (userDao.isUsernameTaken(username, 0)) {
            AlertUtil.showWarning("Gestion des comptes", "Ce nom d'utilisateur existe d\u00e9j\u00e0.");
            return;
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(passwordField.getText(), salt);

        User user = new User();
        user.setFullName(fullNameField.getText().trim());
        user.setUsername(username);
        user.setPasswordHash(hash);
        user.setPasswordSalt(salt);
        user.setRole(UserRole.PERSONNEL);
        user.setActive(activeCheckBox.isSelected());

        if (userDao.createUser(user)) {
            logAction("CREATE_USER", user.getId(), "Cr\u00e9ation du compte personnel: " + user.getUsername());
            AlertUtil.showInfo("Gestion des comptes", "Compte personnel ajout\u00e9 avec succ\u00e8s.");
            loadUsers();
            clearForm();
        } else {
            AlertUtil.showError("Gestion des comptes", "Impossible d'ajouter ce compte.");
        }
    }

    @FXML
    private void handleUpdateUser() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            AlertUtil.showWarning("Gestion des comptes", "Veuillez s\u00e9lectionner un compte \u00e0 modifier.");
            return;
        }

        if (!canManageSelectedUser(selectedUser)) {
            return;
        }

        if (!validateRequiredFields(false)) {
            return;
        }

        String username = usernameField.getText().trim();
        if (userDao.isUsernameTaken(username, selectedUser.getId())) {
            AlertUtil.showWarning("Gestion des comptes", "Ce nom d'utilisateur existe d\u00e9j\u00e0.");
            return;
        }

        boolean passwordChanged = hasPasswordOrConfirmInput();
        if (passwordChanged && !hasPasswordInput()) {
            AlertUtil.showWarning("Gestion des comptes", "Veuillez saisir le nouveau mot de passe.");
            return;
        }

        if (passwordChanged && !passwordsMatch()) {
            AlertUtil.showWarning("Gestion des comptes", "Les mots de passe ne sont pas identiques.");
            return;
        }

        selectedUser.setFullName(fullNameField.getText().trim());
        selectedUser.setUsername(username);
        selectedUser.setRole(UserRole.PERSONNEL);
        selectedUser.setActive(activeCheckBox.isSelected());

        boolean updated = userDao.updateUser(selectedUser);
        if (!updated) {
            AlertUtil.showError("Gestion des comptes", "Impossible de modifier ce compte.");
            return;
        }

        if (passwordChanged) {
            String salt = PasswordUtil.generateSalt();
            String hash = PasswordUtil.hashPassword(passwordField.getText(), salt);
            userDao.updatePassword(selectedUser.getId(), hash, salt);
        }

        logAction("UPDATE_USER", selectedUser.getId(), "Modification du compte personnel: " + selectedUser.getUsername());
        AlertUtil.showInfo("Gestion des comptes", "Compte personnel modifi\u00e9 avec succ\u00e8s.");
        loadUsers();
        clearForm();
    }

    @FXML
    private void handleDeactivateUser() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            AlertUtil.showWarning("Gestion des comptes", "Veuillez s\u00e9lectionner un compte \u00e0 d\u00e9sactiver.");
            return;
        }

        if (!canManageSelectedUser(selectedUser)) {
            return;
        }

        if (!selectedUser.isActive()) {
            AlertUtil.showWarning("Gestion des comptes", "Ce compte est d\u00e9j\u00e0 inactif.");
            return;
        }

        if (userDao.setActive(selectedUser.getId(), false)) {
            logAction("DEACTIVATE_USER", selectedUser.getId(),
                    "D\u00e9sactivation du compte personnel: " + selectedUser.getUsername());
            AlertUtil.showInfo("Gestion des comptes", "Compte d\u00e9sactiv\u00e9 avec succ\u00e8s.");
            loadUsers();
            clearForm();
        } else {
            AlertUtil.showError("Gestion des comptes", "Impossible de d\u00e9sactiver ce compte.");
        }
    }

    @FXML
    private void handleReactivateUser() {
        User selectedUser = usersTable.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            AlertUtil.showWarning("Gestion des comptes", "Veuillez s\u00e9lectionner un compte \u00e0 r\u00e9activer.");
            return;
        }

        if (!canManageSelectedUser(selectedUser)) {
            return;
        }

        if (selectedUser.isActive()) {
            AlertUtil.showWarning("Gestion des comptes", "Ce compte est d\u00e9j\u00e0 actif.");
            return;
        }

        if (userDao.setActive(selectedUser.getId(), true)) {
            logAction("REACTIVATE_USER", selectedUser.getId(),
                    "R\u00e9activation du compte personnel: " + selectedUser.getUsername());
            AlertUtil.showInfo("Gestion des comptes", "Compte r\u00e9activ\u00e9 avec succ\u00e8s.");
            loadUsers();
            clearForm();
        } else {
            AlertUtil.showError("Gestion des comptes", "Impossible de r\u00e9activer ce compte.");
        }
    }

    @FXML
    private void clearForm() {
        usersTable.getSelectionModel().clearSelection();
        formTitleLabel.setText("Ajouter un compte personnel");
        fullNameField.clear();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        activeCheckBox.setSelected(true);
        updateButton.setDisable(true);
        deactivateButton.setDisable(true);
        reactivateButton.setDisable(true);
    }

    private void showAccessDenied() {
        managementPane.setVisible(false);
        managementPane.setManaged(false);
        accessDeniedPane.setVisible(true);
        accessDeniedPane.setManaged(true);
    }

    private void configureTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole().getLabel()));
        stateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().isActive()) {
                return new SimpleStringProperty("Compte actif");
            }
            return new SimpleStringProperty("Compte inactif");
        });

        usersTable.setItems(users);
    }

    private void loadUsers() {
        List<User> userList = userDao.listAllUsers();
        users.setAll(userList);
    }

    private void fillForm(User user) {
        if (user == null) {
            return;
        }

        formTitleLabel.setText("Modifier");
        fullNameField.setText(user.getFullName());
        usernameField.setText(user.getUsername());
        passwordField.clear();
        confirmPasswordField.clear();
        activeCheckBox.setSelected(user.isActive());

        boolean personnelAccount = user.getRole() == UserRole.PERSONNEL;
        updateButton.setDisable(!personnelAccount);
        deactivateButton.setDisable(!personnelAccount || !user.isActive());
        reactivateButton.setDisable(!personnelAccount || user.isActive());
    }

    private boolean validateRequiredFields(boolean passwordRequired) {
        if (fullNameField.getText() == null || fullNameField.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Gestion des comptes", "Le nom complet est obligatoire.");
            return false;
        }

        if (usernameField.getText() == null || usernameField.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Gestion des comptes", "Le nom d'utilisateur est obligatoire.");
            return false;
        }

        if (passwordRequired && !hasPasswordInput()) {
            AlertUtil.showWarning("Gestion des comptes", "Le mot de passe est obligatoire.");
            return false;
        }

        if ((passwordRequired || hasPasswordOrConfirmInput()) && !passwordsMatch()) {
            AlertUtil.showWarning("Gestion des comptes", "Les mots de passe ne sont pas identiques.");
            return false;
        }

        return true;
    }

    private boolean hasPasswordInput() {
        return passwordField.getText() != null && !passwordField.getText().isBlank();
    }

    private boolean hasPasswordOrConfirmInput() {
        boolean passwordFilled = passwordField.getText() != null && !passwordField.getText().isBlank();
        boolean confirmFilled = confirmPasswordField.getText() != null && !confirmPasswordField.getText().isBlank();
        return passwordFilled || confirmFilled;
    }

    private boolean passwordsMatch() {
        return passwordField.getText().equals(confirmPasswordField.getText());
    }

    private boolean canManageSelectedUser(User selectedUser) {
        User currentUser = Session.getCurrentUser();

        if (currentUser != null && selectedUser.getId() == currentUser.getId()) {
            AlertUtil.showWarning("Gestion des comptes", "Vous ne pouvez pas d\u00e9sactiver ou modifier votre propre compte ici.");
            return false;
        }

        if (selectedUser.getRole() != UserRole.PERSONNEL) {
            AlertUtil.showWarning("Gestion des comptes", "Cette page permet seulement de g\u00e9rer les comptes Personnel.");
            return false;
        }

        return true;
    }

    private void logAction(String action, int recordId, String details) {
        User currentUser = Session.getCurrentUser();
        Integer currentUserId = null;

        if (currentUser != null) {
            currentUserId = currentUser.getId();
        }

        activityLogDao.log(currentUserId, action, "users", recordId, details);
    }
}
