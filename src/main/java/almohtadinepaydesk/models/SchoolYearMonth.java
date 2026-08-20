package almohtadinepaydesk.models;

import java.time.LocalDateTime;

/**
 * Represents one month that belongs to a school year.
 * The app uses this to control which months are active for payments.
 */
public class SchoolYearMonth {

    private int id;
    private int schoolYearId;
    private String monthName;
    private int monthNumber;
    private int displayOrder;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SchoolYearMonth() {
    }

    /**
     * Creates a month row and marks it active by default.
     *
     * @param schoolYearId owning school year
     * @param monthName month label
     * @param monthNumber numeric month value
     * @param displayOrder ordering used in the UI and queries
     */
    public SchoolYearMonth(int schoolYearId, String monthName, int monthNumber, int displayOrder) {
        this.schoolYearId = schoolYearId;
        this.monthName = monthName;
        this.monthNumber = monthNumber;
        this.displayOrder = displayOrder;
        this.active = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSchoolYearId() {
        return schoolYearId;
    }

    public void setSchoolYearId(int schoolYearId) {
        this.schoolYearId = schoolYearId;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
    }

    public int getMonthNumber() {
        return monthNumber;
    }

    public void setMonthNumber(int monthNumber) {
        this.monthNumber = monthNumber;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return monthName;
    }
}
