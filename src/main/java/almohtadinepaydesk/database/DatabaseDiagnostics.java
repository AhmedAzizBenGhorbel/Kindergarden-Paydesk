package almohtadinepaydesk.database;

import java.sql.SQLException;
import java.util.logging.Logger;

/** Diagnostic metadata excludes SQL text, driver messages and credentials. */
public final class DatabaseDiagnostics {
    private static final Logger LOGGER = Logger.getLogger(DatabaseDiagnostics.class.getName());
    public static String userMessage(SQLException e) {
        String state = e.getSQLState();
        if ("PDCFG".equals(state)) return "Configuration locale invalide. Vérifiez paydesk.properties et PAYDESK_DB_*.";
        if (e.getErrorCode() == 1049 || e.getErrorCode() == 1146 || (state != null && state.startsWith("42"))) {
            return "Schéma de base de données absent ou incompatible. Vérifiez ou initialisez la base.";
        }
        if (state != null && state.startsWith("28")) {
            return "Accès à la base de données refusé. Vérifiez la configuration locale.";
        }
        return "Base de données indisponible. Vérifiez le serveur et la configuration locale.";
    }
    public static void report(String operation, Exception e) {
        if (e instanceof SQLException sql) {
            String state = sql.getSQLState();
            LOGGER.warning(operation + ": SQLState=" + (state != null && state.matches("[A-Z0-9]{5}") ? state : "unknown")
                    + ", vendorCode=" + sql.getErrorCode());
        } else {
            LOGGER.warning(operation + ": " + e.getClass().getSimpleName());
        }
    }
    private DatabaseDiagnostics() { }
}
