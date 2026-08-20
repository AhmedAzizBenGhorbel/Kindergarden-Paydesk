package almohtadinepaydesk.models;

import java.time.LocalDateTime;

/**
 * Represents one backup attempt stored in the database.
 * The app uses this to keep a simple history of success and failure events.
 */
public class BackupLog {

    private int id;
    private String fileName;
    private String folderPath;
    private LocalDateTime backupDate;
    private String status;
    private String message;
    private Integer createdByUserId;

    public BackupLog() {
    }

    /**
     * Creates a log entry with the current timestamp.
     *
     * @param fileName name of the backup file
     * @param status backup status such as SUCCESS or FAILED
     */
    public BackupLog(String fileName, String status) {
        this.fileName = fileName;
        this.status = status;
        this.backupDate = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public LocalDateTime getBackupDate() {
        return backupDate;
    }

    public void setBackupDate(LocalDateTime backupDate) {
        this.backupDate = backupDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Integer createdByUserId) {
        this.createdByUserId = createdByUserId;
    }
}
