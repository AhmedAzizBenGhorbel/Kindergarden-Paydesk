package almohtadinepaydesk.services;

import java.sql.SQLException;
import almohtadinepaydesk.database.DatabaseDiagnostics;
import almohtadinepaydesk.dao.ActivityLogDao;
import almohtadinepaydesk.dao.UserDao;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.security.PasswordUtil;
import almohtadinepaydesk.security.Session;

public class AuthService {

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    public AuthService() { this(new UserDao(), new ActivityLogDao()); }
    public AuthService(UserDao userDao, ActivityLogDao activityLogDao) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
    }
    private String lastErrorMessage = "";

    public boolean login(String username, String password) {
        lastErrorMessage = "";
        User user;
        try {
            user = userDao.findByUsername(username);
        } catch (SQLException e) {
            lastErrorMessage = DatabaseDiagnostics.userMessage(e);
            DatabaseDiagnostics.report("login user lookup", e);
            return false;
        }

        if (user == null) {
            lastErrorMessage = "Nom d'utilisateur ou mot de passe incorrect.";
            activityLogDao.log(null, "LOGIN_FAILURE", "users", null, "Nom d'utilisateur introuvable: " + username);
            return false;
        }

        if (!user.isActive()) {
            lastErrorMessage = "Ce compte est inactif.";
            activityLogDao.log(user.getId(), "LOGIN_FAILURE", "users", user.getId(), "Compte inactif: " + username);
            return false;
        }

        if (isOldPlainPassword(user, password)) {
            upgradePlainPassword(user, password);
            Session.login(user);
            activityLogDao.log(user.getId(), "LOGIN_SUCCESS", "users", user.getId(), "Connexion r\u00e9ussie: " + username);
            return true;
        }

        boolean validPassword = PasswordUtil.verifyPassword(
                password,
                user.getPasswordSalt(),
                user.getPasswordHash());

        if (!validPassword) {
            lastErrorMessage = "Nom d'utilisateur ou mot de passe incorrect.";
            activityLogDao.log(user.getId(), "LOGIN_FAILURE", "users", user.getId(), "Mot de passe incorrect: " + username);
            return false;
        }

        Session.login(user);
        activityLogDao.log(user.getId(), "LOGIN_SUCCESS", "users", user.getId(), "Connexion r\u00e9ussie: " + username);
        return true;
    }

    public boolean changePassword(String oldPassword, String newPassword) {
        User currentUser = Session.getCurrentUser();

        if (currentUser == null) {
            lastErrorMessage = "Aucun utilisateur connect\u00e9.";
            return false;
        }

        User user = userDao.findById(currentUser.getId());
        if (user == null) {
            lastErrorMessage = "Utilisateur introuvable.";
            return false;
        }

        boolean oldPasswordValid = PasswordUtil.verifyPassword(
                oldPassword,
                user.getPasswordSalt(),
                user.getPasswordHash());

        if (!oldPasswordValid && !isOldPlainPassword(user, oldPassword)) {
            lastErrorMessage = "Ancien mot de passe incorrect.";
            return false;
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(newPassword, salt);
        boolean updated = userDao.updatePassword(user.getId(), hash, salt);

        if (!updated) {
            lastErrorMessage = "Impossible de changer le mot de passe.";
            return false;
        }

        currentUser.setPasswordHash(hash);
        currentUser.setPasswordSalt(salt);
        return true;
    }

    public void logout() {
        Session.logout();
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    private boolean isOldPlainPassword(User user, String password) {
        String salt = user.getPasswordSalt();
        return (salt == null || salt.isBlank()) && password.equals(user.getPasswordHash());
    }

    private void upgradePlainPassword(User user, String password) {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        if (userDao.updatePassword(user.getId(), hash, salt)) {
            user.setPasswordHash(hash);
            user.setPasswordSalt(salt);
        }
    }
}
