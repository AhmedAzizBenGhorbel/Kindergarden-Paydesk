package almohtadinepaydesk.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents the monthly payment row for one child and one active month.
 * This is the central record that tracks expected, paid, remaining, and status values.
 */
public class MonthlyRecord {

    private int id;
    private int childId;
    private int schoolYearMonthId;
    private BigDecimal expectedAmount;
    private BigDecimal totalPaid;
    private BigDecimal baseMonthlyFee;
    private BigDecimal totalExtras;
    private BigDecimal totalExpected;
    private BigDecimal remainingAmount;
    private BigDecimal advanceAmount;
    private PaymentStatus paymentStatus;
    private LocalDate dueDate;
    private String notes;
    private boolean active;
    private String childFullName;
    private String parentFullName;
    private String classGroup;
    private String schoolYearName;
    private String monthName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MonthlyRecord() {
    }

    /**
     * Creates a record with the starting expected amount and default payment state.
     *
     * @param childId child identifier
     * @param schoolYearMonthId month identifier
     * @param expectedAmount starting expected amount
     */
    public MonthlyRecord(int childId, int schoolYearMonthId, BigDecimal expectedAmount) {
        this.childId = childId;
        this.schoolYearMonthId = schoolYearMonthId;
        this.expectedAmount = expectedAmount;
        this.totalPaid = BigDecimal.ZERO;
        this.paymentStatus = PaymentStatus.NON_PAYE;
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

    public int getSchoolYearMonthId() {
        return schoolYearMonthId;
    }

    public void setSchoolYearMonthId(int schoolYearMonthId) {
        this.schoolYearMonthId = schoolYearMonthId;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }

    public BigDecimal getBaseMonthlyFee() {
        return baseMonthlyFee;
    }

    public void setBaseMonthlyFee(BigDecimal baseMonthlyFee) {
        this.baseMonthlyFee = baseMonthlyFee;
    }

    public BigDecimal getTotalExtras() {
        return totalExtras;
    }

    public void setTotalExtras(BigDecimal totalExtras) {
        this.totalExtras = totalExtras;
    }

    public BigDecimal getTotalExpected() {
        return totalExpected;
    }

    public void setTotalExpected(BigDecimal totalExpected) {
        this.totalExpected = totalExpected;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public BigDecimal getAdvanceAmount() {
        return advanceAmount;
    }

    public void setAdvanceAmount(BigDecimal advanceAmount) {
        this.advanceAmount = advanceAmount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
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

    public String getParentFullName() {
        return parentFullName;
    }

    public void setParentFullName(String parentFullName) {
        this.parentFullName = parentFullName;
    }

    public String getClassGroup() {
        return classGroup;
    }

    public void setClassGroup(String classGroup) {
        this.classGroup = classGroup;
    }

    public String getSchoolYearName() {
        return schoolYearName;
    }

    public void setSchoolYearName(String schoolYearName) {
        this.schoolYearName = schoolYearName;
    }

    public String getMonthName() {
        return monthName;
    }

    public void setMonthName(String monthName) {
        this.monthName = monthName;
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
