package almohtadinepaydesk.models;

import java.time.LocalDateTime;

/**
 * Represents one activity log row.
 * Activity logs are used to keep a basic audit trail of important app actions.
 */
public class ActivityLog {

    private int id;
    private Integer userId;
    private String action;
    private String tableName;
    private Integer recordId;
    private String details;
    private String userFullName;
    private LocalDateTime activityDate;

    public ActivityLog() {
    }

    /**
     * Creates a new activity log entry with the current time.
     *
     * @param action short action code such as LOGIN_SUCCESS or UPDATE_CHILD
     */
    public ActivityLog(String action) {
        this.action = action;
        this.activityDate = LocalDateTime.now();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public LocalDateTime getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDateTime activityDate) {
        this.activityDate = activityDate;
    }
}
