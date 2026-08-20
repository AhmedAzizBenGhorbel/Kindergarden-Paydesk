package almohtadinepaydesk.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents an extra fee attached to a child.
 * Extras can be one-time or recurring and are added into monthly totals.
 */
public class Extra {

    private int id;
    private int childId;
    private Integer schoolYearMonthId;
    private Integer startMonthId;
    private Integer endMonthId;
    private String label;
    private ExtraType extraType;
    private BigDecimal amount;
    private LocalDate extraDate;
    private boolean paid;
    private String notes;
    private boolean active;
    private String childFullName;
    private Integer schoolYearId;
    private String schoolYearName;
    private String startMonthName;
    private String endMonthName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Extra() {
    }

    /**
     * Creates a basic one-time extra.
     *
     * @param childId child identifier
     * @param label extra label
     * @param amount extra amount
     * @param extraDate date of the extra
     */
    public Extra(int childId, String label, BigDecimal amount, LocalDate extraDate) {
        this.childId = childId;
        this.label = label;
        this.amount = amount;
        this.extraDate = extraDate;
        this.extraType = ExtraType.PONCTUEL;
        this.paid = false;
        this.active = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getChildId() {
        return childId;
    }

    public void setChildId(int childId) {
        this.childId = childId;
    }

    public Integer getSchoolYearMonthId() {
        return schoolYearMonthId;
    }

    public void setSchoolYearMonthId(Integer schoolYearMonthId) {
        this.schoolYearMonthId = schoolYearMonthId;
    }

    public Integer getStartMonthId() {
        return startMonthId;
    }

    public void setStartMonthId(Integer startMonthId) {
        this.startMonthId = startMonthId;
    }

    public Integer getEndMonthId() {
        return endMonthId;
    }

    public void setEndMonthId(Integer endMonthId) {
        this.endMonthId = endMonthId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ExtraType getExtraType() {
        return extraType;
    }

    public void setExtraType(ExtraType extraType) {
        this.extraType = extraType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExtraDate() {
        return extraDate;
    }

    public void setExtraDate(LocalDate extraDate) {
        this.extraDate = extraDate;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getChildFullName() {
        return childFullName;
    }

    public void setChildFullName(String childFullName) {
        this.childFullName = childFullName;
    }

    public Integer getSchoolYearId() {
        return schoolYearId;
    }

    public void setSchoolYearId(Integer schoolYearId) {
        this.schoolYearId = schoolYearId;
    }

    public String getSchoolYearName() {
        return schoolYearName;
    }

    public void setSchoolYearName(String schoolYearName) {
        this.schoolYearName = schoolYearName;
    }

    public String getStartMonthName() {
        return startMonthName;
    }

    public void setStartMonthName(String startMonthName) {
        this.startMonthName = startMonthName;
    }

    public String getEndMonthName() {
        return endMonthName;
    }

    public void setEndMonthName(String endMonthName) {
        this.endMonthName = endMonthName;
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
}
