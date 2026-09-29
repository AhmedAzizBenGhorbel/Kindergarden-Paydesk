package almohtadinepaydesk;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import almohtadinepaydesk.database.*;
import almohtadinepaydesk.dao.*;
import almohtadinepaydesk.models.User;
import almohtadinepaydesk.services.AuthService;

class ConfigurationAndLoginTest {
    @TempDir Path directory;
    @Test void defaultsAndEnvironmentPrecedence() throws Exception {
        Path file = directory.resolve("local.properties");
        assertEquals("localhost",DatabaseConfig.load(file,Map.of()).host());
        Files.writeString(file,"db.host=lab-host\ndb.port=3308\ndb.database=lab_db\ndb.user=lab\ndb.password=secret\n");
        var settings = DatabaseConfig.load(file,Map.of("PAYDESK_DB_HOST","override","PAYDESK_DB_PASSWORD",""));
        assertEquals("override",settings.host()); assertEquals("3308",settings.port()); assertEquals("",settings.password());
        assertTrue(settings.jdbcUrl(true).contains("override:3308/lab_db")); assertFalse(settings.toString().contains("secret"));
    }
    @Test void rejectsSqlAndUrlInjection() {
        Path absent = directory.resolve("absent");
        assertThrows(IllegalArgumentException.class,()->DatabaseConfig.load(absent,Map.of("PAYDESK_DB_DATABASE","db`; DROP TABLE users")));
        assertThrows(IllegalArgumentException.class,()->DatabaseConfig.load(absent,Map.of("PAYDESK_DB_PORT","65536")));
    }
    private AuthService auth(UserDao dao) {
        return new AuthService(dao,new ActivityLogDao() {
            @Override public boolean log(Integer userId,String action,String entity,Integer entityId,String details) { return true; }
        });
    }
    @Test void databaseFailureIsDistinctFromUnknownUser() {
        AuthService unavailable = auth(new UserDao() { @Override public User findByUsername(String name) throws SQLException { throw new SQLException("password=secret","08001",0); } });
        assertFalse(unavailable.login("synthetic","wrong"));
        assertTrue(unavailable.getLastErrorMessage().contains("indisponible")); assertFalse(unavailable.getLastErrorMessage().contains("secret"));
        AuthService missing = auth(new UserDao() { @Override public User findByUsername(String name) { return null; } });
        assertFalse(missing.login("synthetic","wrong")); assertTrue(missing.getLastErrorMessage().contains("incorrect"));
    }
    @Test void invalidLocalConfigCanBeRetriedWithoutStaticInitializationFailure() throws Exception {
        Files.writeString(directory.resolve("paydesk.properties"), "db.port=invalid\ndb.password=never-log-this\n");
        String javaCommand = Path.of(System.getProperty("java.home"),"bin","java").toString();
        ProcessBuilder builder = new ProcessBuilder(javaCommand,"-cp",System.getProperty("java.class.path"),
                InvalidConfigurationProbe.class.getName()).directory(directory.toFile()).redirectErrorStream(true);
        builder.environment().keySet().removeIf(key -> key.startsWith("PAYDESK_DB_"));
        Process process = builder.start();
        assertTrue(process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS));
        String output = new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0,process.exitValue(),output); assertFalse(output.contains("never-log-this"));
    }
    public static class InvalidConfigurationProbe {
        public static void main(String[] args) {
            AuthService service = new AuthService();
            for (int attempt=0; attempt<2; attempt++) {
                if (service.login("synthetic","wrong") || !service.getLastErrorMessage().contains("Configuration locale invalide")) {
                    throw new AssertionError("Invalid config was not reported safely");
                }
            }
        }
    }
    @Test void classifiesSchemaAndAccessErrorsWithoutDriverMessages() {
        assertTrue(DatabaseDiagnostics.userMessage(new SQLException("secret","42S02",1146)).contains("Schéma"));
        assertTrue(DatabaseDiagnostics.userMessage(new SQLException("secret","28000",1045)).contains("refusé"));
    }
}
