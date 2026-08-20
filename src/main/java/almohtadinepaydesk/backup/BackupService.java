package almohtadinepaydesk.backup;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import almohtadinepaydesk.config.AppConfig;
import almohtadinepaydesk.database.DatabaseConfig;

public class BackupService {

    private static final DateTimeFormatter FILE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm");

    public BackupResult runBackup(String backupFolderPath) {
        if (backupFolderPath == null || backupFolderPath.trim().isEmpty()) {
            return BackupResult.failure("", "", "Veuillez choisir un dossier de sauvegarde.");
        }

        File backupFolder = new File(backupFolderPath.trim());
        if (!backupFolder.exists() || !backupFolder.isDirectory()) {
            return BackupResult.failure("", backupFolderPath, "Le dossier de sauvegarde est introuvable.");
        }

        String fileName = AppConfig.DEFAULT_BACKUP_FILE_PREFIX
                + "_"
                + LocalDateTime.now().format(FILE_DATE_FORMATTER)
                + ".sql";

        File outputFile = new File(backupFolder, fileName);
        List<String> command = buildCommand(outputFile);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            String output = readProcessOutput(process);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return BackupResult.success(fileName, backupFolder.getAbsolutePath(), "Sauvegarde termin\u00e9e avec succ\u00e8s.");
            }

            return BackupResult.failure(fileName, backupFolder.getAbsolutePath(), cleanMessage(output));
        } catch (IOException e) {
            if (isMysqldumpMissing(e)) {
                return BackupResult.failure(
                        fileName,
                        backupFolder.getAbsolutePath(),
                        "mysqldump introuvable. V\u00e9rifiez l'installation de MySQL ou le PATH.");
            }

            return BackupResult.failure(fileName, backupFolder.getAbsolutePath(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return BackupResult.failure(fileName, backupFolder.getAbsolutePath(), "Sauvegarde interrompue.");
        }
    }

    private List<String> buildCommand(File outputFile) {
        List<String> command = new ArrayList<>();
        command.add(findMysqldumpCommand());
        command.add("--host=localhost");
        command.add("--port=3306");
        command.add("--user=" + DatabaseConfig.USER);

        if (DatabaseConfig.PASSWORD != null && !DatabaseConfig.PASSWORD.isBlank()) {
            command.add("--password=" + DatabaseConfig.PASSWORD);
        }

        command.add("--default-character-set=utf8mb4");
        command.add("--result-file=" + outputFile.getAbsolutePath());
        command.add(AppConfig.DATABASE_NAME);
        return command;
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

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    output.append(line).append(System.lineSeparator());
                }
            }
        }

        return output.toString().trim();
    }

    private String cleanMessage(String output) {
        if (output == null || output.isBlank()) {
            return "La sauvegarde a \u00e9chou\u00e9.";
        }

        return output;
    }

    private boolean isMysqldumpMissing(IOException e) {
        String message = e.getMessage();

        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("mysqldump")
                && (lowerMessage.contains("createprocess error=2")
                        || lowerMessage.contains("cannot find")
                        || lowerMessage.contains("le fichier sp\u00e9cifi\u00e9 est introuvable"));
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
