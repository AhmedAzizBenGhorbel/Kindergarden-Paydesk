package almohtadinepaydesk;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import almohtadinepaydesk.backup.BackupService;
import almohtadinepaydesk.database.DatabaseConfig;

class BackupServiceTest {
    @TempDir Path directory;
    private static class StubProcess extends Process {
        final int exit; boolean alive; boolean destroyed;
        StubProcess(int exit,boolean alive) { this.exit=exit; this.alive=alive; }
        public OutputStream getOutputStream(){return OutputStream.nullOutputStream();}
        public InputStream getInputStream(){return InputStream.nullInputStream();}
        public InputStream getErrorStream(){return InputStream.nullInputStream();}
        public int waitFor(){alive=false; return exit;}
        public boolean waitFor(long timeout,TimeUnit unit){return !alive;}
        public int exitValue(){return exit;}
        public void destroy(){alive=false; destroyed=true;}
        public Process destroyForcibly(){destroy(); return this;}
        public boolean isAlive(){return alive;}
    }
    private static class StubBackup extends BackupService {
        final StubProcess process; final boolean writeSql; List<String> command; Path options; Path errors;
        StubBackup(int exit,boolean timeout,boolean writeSql) {
            super(new DatabaseConfig.Settings("lab-host","3308","lab_db","lab_user","sensitive-secret","dump-test"),1);
            process=new StubProcess(exit,timeout); this.writeSql=writeSql;
        }
        @Override protected Process startProcess(ProcessBuilder builder) throws IOException {
            command=List.copyOf(builder.command());
            options=Path.of(command.get(1).substring("--defaults-file=".length()));
            assertTrue(Files.readString(options).contains("password=\"sensitive-secret\""));
            errors=builder.redirectError().file().toPath();
            Files.writeString(errors,"synthetic dump error sensitive-secret");
            if(writeSql) {
                String output=command.stream().filter(s->s.startsWith("--result-file=")).findFirst().orElseThrow();
                Files.writeString(Path.of(output.substring("--result-file=".length())),"-- synthetic SQL\nCREATE TABLE lab (id INT);\n");
            }
            return process;
        }
        void assertCleaned(){assertFalse(Files.exists(options)); assertFalse(Files.exists(errors));}
    }
    @Test void successfulDumpsUseSharedSettingsAndUniqueFiles() throws Exception {
        StubBackup backup=new StubBackup(0,false,true);
        var first=backup.runBackup(directory.toString()); var second=backup.runBackup(directory.toString());
        assertTrue(first.isSuccess()); assertTrue(second.isSuccess()); assertNotEquals(first.getFileName(),second.getFileName());
        assertTrue(Files.size(directory.resolve(first.getFileName()))>0);
        assertTrue(backup.command.contains("--host=lab-host")); assertTrue(backup.command.contains("--port=3308"));
        assertEquals("lab_db",backup.command.getLast()); assertTrue(backup.command.contains("--single-transaction"));
        assertFalse(String.join(" ",backup.command).contains("sensitive-secret")); backup.assertCleaned();
    }
    @Test void failedDumpRemovesPartialFileAndRedactsPassword() {
        StubBackup backup=new StubBackup(2,false,true); var result=backup.runBackup(directory.toString());
        assertFalse(result.isSuccess()); assertTrue(result.getMessage().contains("code 2"));
        assertFalse(result.getMessage().contains("sensitive-secret")); assertFalse(Files.exists(directory.resolve(result.getFileName()))); backup.assertCleaned();
    }
    @Test void timeoutDestroysProcessAndRemovesPartialFile() {
        StubBackup backup=new StubBackup(0,true,true); var result=backup.runBackup(directory.toString());
        assertFalse(result.isSuccess()); assertTrue(result.getMessage().contains("délai")); assertTrue(backup.process.destroyed);
        assertFalse(Files.exists(directory.resolve(result.getFileName()))); backup.assertCleaned();
    }
    @Test void zeroExitWithoutSqlIsFailure() {
        StubBackup backup=new StubBackup(0,false,false); var result=backup.runBackup(directory.toString());
        assertFalse(result.isSuccess()); assertFalse(Files.exists(directory.resolve(result.getFileName()))); backup.assertCleaned();
    }
    @Test void invalidPathIsReportedAsFailure() {
        assertFalse(new StubBackup(0,false,true).runBackup("bad\u0000path").isSuccess());
    }
    @Test void defaultBackupReloadsConfigurationBetweenAttempts() throws Exception {
        String javaCommand = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder = new ProcessBuilder(javaCommand, "-cp", System.getProperty("java.class.path"),
                ConfigurationReloadProbe.class.getName()).directory(directory.toFile()).redirectErrorStream(true);
        builder.environment().keySet().removeIf(key -> key.startsWith("PAYDESK_DB_"));
        Process process = builder.start();
        try {
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Configuration probe timed out");
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
    public static class ConfigurationReloadProbe {
        public static void main(String[] args) throws Exception {
            String[] expectedHost = {"first-lab-host"};
            BackupService backup = new BackupService() {
                @Override protected Process startProcess(ProcessBuilder builder) throws IOException {
                    if (!builder.command().contains("--host=" + expectedHost[0])) {
                        throw new AssertionError("Backup reused stale database configuration");
                    }
                    throw new IOException("Synthetic executable failure");
                }
            };
            for (String host : new String[]{"first-lab-host", "second-lab-host"}) {
                expectedHost[0] = host;
                Files.writeString(Path.of("paydesk.properties"), "db.host=" + host + "\n");
                if (backup.runBackup(".").isSuccess()) throw new AssertionError("Synthetic backup succeeded");
            }
        }
    }
    @Test void missingExecutableIsReproducibleWithoutDatabase() throws Exception {
        var config=new DatabaseConfig.Settings("localhost","3306","synthetic_lab","synthetic","","no-such-paydesk-dump-command-4821");
        var result=new BackupService(config,1).runBackup(directory.toString());
        assertFalse(result.isSuccess()); assertTrue(result.getMessage().contains("mysqldump"));
        try(var files=Files.list(directory)){assertEquals(0,files.count());}
    }
}
