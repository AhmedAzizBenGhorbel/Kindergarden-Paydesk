package almohtadinepaydesk.backup;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.config.AppConfig;
import almohtadinepaydesk.database.DatabaseConfig;

public class BackupService {

    private DatabaseConfig.Settings settings;
    private final long timeoutSeconds;
    public BackupService() { this(null, 120); }
    public BackupService(DatabaseConfig.Settings settings, long timeoutSeconds) {
        if (timeoutSeconds < 1) throw new IllegalArgumentException("Timeout must be positive.");
        this.settings = settings;
        this.timeoutSeconds = timeoutSeconds;
    }

    public BackupResult runBackup(String backupFolderPath) {
        if (backupFolderPath == null || backupFolderPath.isBlank()) {
            return BackupResult.failure("", "", "Veuillez choisir un dossier de sauvegarde.");
        }
        java.nio.file.Path folder;
        try { folder = java.nio.file.Path.of(backupFolderPath.trim()); }
        catch (java.nio.file.InvalidPathException e) {
            return BackupResult.failure("", "", "Le chemin du dossier de sauvegarde est invalide.");
        }
        if (!java.nio.file.Files.isDirectory(folder)) {
            return BackupResult.failure("", backupFolderPath, "Le dossier de sauvegarde est introuvable.");
        }
        java.nio.file.Path output = null;
        java.nio.file.Path options = null;
        java.nio.file.Path errors = null;
        Process process = null;
        boolean success = false;
        try {
            if (settings == null) settings = DatabaseConfig.getSettings();
            output = java.nio.file.Files.createTempFile(folder, AppConfig.DEFAULT_BACKUP_FILE_PREFIX + "_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")) + "_", ".sql");
            options = java.nio.file.Files.createTempFile("paydesk-mysql-", ".cnf");
            restrictToOwner(options);
            String clientOptions = "[client]\nuser=" + quoteOption(settings.user())
                    + "\npassword=" + quoteOption(settings.password()) + "\n";
            java.nio.file.Files.writeString(options, clientOptions, StandardCharsets.UTF_8);
            errors = java.nio.file.Files.createTempFile("paydesk-dump-", ".log");
            List<String> command = buildCommand(output.toFile(), options);
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            builder.redirectError(errors.toFile());
            process = startProcess(builder);
            if (!process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
                return BackupResult.failure(output.getFileName().toString(), folder.toAbsolutePath().toString(),
                        "La sauvegarde a dépassé le délai de " + timeoutSeconds + " secondes.");
            }
            if (process.exitValue() != 0) {
                String detail;
                try (java.io.InputStream stream = java.nio.file.Files.newInputStream(errors)) {
                    detail = new String(stream.readNBytes(4096), StandardCharsets.UTF_8);
                }
                if (!settings.password().isEmpty()) detail = detail.replace(settings.password(), "[redacted]");
                return BackupResult.failure(output.getFileName().toString(), folder.toAbsolutePath().toString(),
                        "mysqldump a échoué (code " + process.exitValue() + "). " + cleanMessage(detail));
            }
            if (java.nio.file.Files.size(output) == 0) {
                return BackupResult.failure(output.getFileName().toString(), folder.toAbsolutePath().toString(),
                        "mysqldump n'a produit aucun fichier SQL utilisable.");
            }
            success = true;
            return BackupResult.success(output.getFileName().toString(), folder.toAbsolutePath().toString(),
                    "Sauvegarde terminée avec succès.");
        } catch (java.sql.SQLException e) {
            return BackupResult.failure("", folder.toAbsolutePath().toString(),
                    almohtadinepaydesk.database.DatabaseDiagnostics.userMessage(e));
        } catch (IOException | java.nio.file.InvalidPathException e) {
            return BackupResult.failure("", folder.toAbsolutePath().toString(),
                    "Impossible de créer la sauvegarde. Vérifiez mysqldump, le PATH et les droits du dossier.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return BackupResult.failure("", folder.toAbsolutePath().toString(), "Sauvegarde interrompue.");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                try { process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            deleteTemporary(options);
            deleteTemporary(errors);
            if (!success) deleteTemporary(output);
        }
    }

    protected Process startProcess(ProcessBuilder builder) throws IOException { return builder.start(); }

    private List<String> buildCommand(File outputFile, java.nio.file.Path options) {
        List<String> command = new ArrayList<>();
        command.add(settings.mysqldump().isBlank() ? findMysqldumpCommand() : settings.mysqldump());
        // MySQL requires defaults-file as the first option. The secret never enters argv.
        command.add("--defaults-file=" + options.toAbsolutePath());
        command.add("--host=" + settings.host());
        command.add("--port=" + settings.port());
        command.add("--protocol=TCP");
        command.add("--single-transaction");
        command.add("--default-character-set=utf8mb4");
        command.add("--result-file=" + outputFile.getAbsolutePath());
        command.add(settings.database());
        return command;
    }

    private static String quoteOption(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
    private static void restrictToOwner(java.nio.file.Path file) throws IOException {
        var acl = java.nio.file.Files.getFileAttributeView(file, java.nio.file.attribute.AclFileAttributeView.class);
        if (acl != null) {
            var entry = java.nio.file.attribute.AclEntry.newBuilder()
                    .setType(java.nio.file.attribute.AclEntryType.ALLOW).setPrincipal(acl.getOwner())
                    .setPermissions(java.util.EnumSet.allOf(java.nio.file.attribute.AclEntryPermission.class)).build();
            acl.setAcl(List.of(entry));
        } else {
            java.nio.file.Files.setPosixFilePermissions(file,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        }
    }
    private static void deleteTemporary(java.nio.file.Path file) {
        if (file == null) return;
        try { java.nio.file.Files.deleteIfExists(file); }
        catch (IOException e) {
            java.util.logging.Logger.getLogger(BackupService.class.getName())
                    .warning("Backup temporary file cleanup failed; check local temporary directory permissions.");
        }
    }

    private String findMysqldumpCommand() {
        List<String> commonPaths = new ArrayList<>();
        commonPaths.add("C:\\xampp\\mysql\\bin\\mysqldump.exe");
        commonPaths.add("C:\\laragon\\bin\\mysql\\mysql-8.0\\bin\\mysqldump.exe");
        commonPaths.add("C:\\Program Files\\MySQL\\MySQL Server 9.5\\bin\\mysqldump.exe");
        commonPaths.add("C:\\Program Files\\MySQL\\MySQL Server 9.4\\bin\\mysqldump.exe");
        commonPaths.add("C:\\Program Files\\MySQL\\MySQL Server 9.3\\bin\\mysqldump.exe");
        commonPaths.add("C:\\Program Files\\MySQL\\MySQL Server 9.2\\bin\\mysqldump.exe");
        commonPaths.add("C:\\Program Files\\MySQL\\MySQL Server 9.1\\bin\\mysqldump.exe");
        commonPaths.add("C:\\Program Files\\MySQL\\MySQL Server 9.0\\bin\\mysqldump.exe");
        commonPaths.add("C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysqldump.exe");
        commonPaths.add("C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe");

        for (String path : commonPaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return file.getAbsolutePath();
            }
        }

        String wampPath = findMysqldumpInFolder("C:\\wamp64\\bin\\mysql");
        if (wampPath != null) {
            return wampPath;
        }

        String laragonPath = findMysqldumpInFolder("C:\\laragon\\bin\\mysql");
        if (laragonPath != null) {
            return laragonPath;
        }

        return "mysqldump";
    }

    private String findMysqldumpInFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] subFolders = folder.listFiles(File::isDirectory);

        if (subFolders == null) {
            return null;
        }

        for (File subFolder : subFolders) {
            File mysqldumpFile = new File(subFolder, "bin\\mysqldump.exe");
            if (mysqldumpFile.exists() && mysqldumpFile.isFile()) {
                return mysqldumpFile.getAbsolutePath();
            }
        }

        return null;
    }

    private String cleanMessage(String output) {
        if (output == null || output.isBlank()) {
            return "La sauvegarde a \u00e9chou\u00e9.";
        }

        return output;
    }

    public static class BackupResult {
        private final boolean success;
        private final String fileName;
        private final String folderPath;
        private final String message;

        private BackupResult(boolean success, String fileName, String folderPath, String message) {
            this.success = success;
            this.fileName = fileName;
            this.folderPath = folderPath;
            this.message = message;
        }

        public static BackupResult success(String fileName, String folderPath, String message) {
            return new BackupResult(true, fileName, folderPath, message);
        }

        public static BackupResult failure(String fileName, String folderPath, String message) {
            return new BackupResult(false, fileName, folderPath, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFolderPath() {
            return folderPath;
        }

        public String getMessage() {
            return message;
        }
    }
}
