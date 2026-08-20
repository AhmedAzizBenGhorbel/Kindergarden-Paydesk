package almohtadinepaydesk.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents stored receipt data for a payment.
 * The app keeps receipt information in the database, but it does not generate PDFs in V1.
 */
public class Receipt {

    private int id;
    private String receiptNumber;
    private int childId;
    private Integer paymentEntryId;
    private LocalDate receiptDate;
    private BigDecimal totalAmount;
    private String payerName;
    private String notes;
    private boolean active;
    private String childFullName;
    private String parentFullName;
    private PaymentMethod paymentMethod;
    private String schoolYearName;
    private String monthName;
    private String createdByFullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Receipt() {
    }

    /**
     * Creates a receipt skeleton for a payment transaction.
     *
     * @param receiptNumber generated receipt number
     * @param childId child identifier
     * @param receiptDate payment date
     * @param totalAmount receipt total amount
     */
    public Receipt(String receiptNumber, int childId, LocalDate receiptDate, BigDecimal totalAmount) {
        this.receiptNumber = receiptNumber;
        this.childId = childId;
        this.receiptDate = receiptDate;
        this.totalAmount = totalAmount;
        this.active = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public int getChildId() {
        return childId;
    }

    public void setChildId(int childId) {
        this.childId = childId;
    }

    public Integer getPaymentEntryId() {
        return paymentEntryId;
    }

    public void setPaymentEntryId(Integer paymentEntryId) {
        this.paymentEntryId = paymentEntryId;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public String getCreatedByFullName() {
        return createdByFullName;
    }

    public void setCreatedByFullName(String createdByFullName) {
        this.createdByFullName = createdByFullName;
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
