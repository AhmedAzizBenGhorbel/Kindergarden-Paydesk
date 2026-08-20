package almohtadinepaydesk.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents one child registered in the kindergarten.
 * The app keeps children active/inactive instead of deleting them permanently.
 */
public class Child {

    private int id;
    private String firstName;
    private String lastName;
    private String classGroup;
    private LocalDate birthDate;
    private String parentFullName;
    private String parentPhone;
    private LocalDate registrationDate;
    private BigDecimal monthlyFee;
    private String notes;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Child() {
    }

    /**
     * Creates a child using only the names.
     *
     * @param firstName child's first name
     * @param lastName child's last name
     */
    public Child(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = true;
    }

    /**
     * Creates a compact child object for cases where the UI already has the main values.
     *
     * @param fullName combined child name
     * @param registrationDate registration date
     * @param monthlyFee monthly fee
     */
    public Child(String fullName, LocalDate registrationDate, BigDecimal monthlyFee) {
        setFullName(fullName);
        this.registrationDate = registrationDate;
        this.monthlyFee = monthlyFee;
        this.active = true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        // The UI often needs a single display name, so we combine the stored name parts here.
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }

        return firstName + " " + lastName;
    }

    public void setFullName(String fullName) {
        // This setter is intentionally simple because some screens work with a single name field.
        this.firstName = fullName;
        this.lastName = "";
    }

    public String getClassGroup() {
        return classGroup;
    }

    public void setClassGroup(String classGroup) {
        this.classGroup = classGroup;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getParentFullName() {
        return parentFullName;
    }

    public void setParentFullName(String parentFullName) {
        this.parentFullName = parentFullName;
    }

    public String getParentPhone() {
        return parentPhone;
    }

    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public BigDecimal getMonthlyFee() {
        return monthlyFee;
    }

    public void setMonthlyFee(BigDecimal monthlyFee) {
        this.monthlyFee = monthlyFee;
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
        return getFullName();
    }
}
