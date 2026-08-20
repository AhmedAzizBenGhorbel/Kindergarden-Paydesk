package almohtadinepaydesk.models;

/**
 * Lists the payment methods accepted by the application.
 * The enum keeps the labels aligned with the French UI.
 */
public enum PaymentMethod {
    ESPECES("Esp\u00e8ces"),
    CHEQUE("Ch\u00e8que"),
    VIREMENT("Virement"),
    AUTRE("Autre");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    /**
     * Returns the French label for display in the UI.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Accepts either the enum name or the French label when data comes from the database.
     *
     * @param value stored method value
     * @return matching payment method, or ESPECES as a simple default
     */
    public static PaymentMethod fromLabelOrName(String value) {
        if (value == null || value.isBlank()) {
            return ESPECES;
        }

        for (PaymentMethod method : values()) {
            if (method.name().equalsIgnoreCase(value) || method.getLabel().equalsIgnoreCase(value)) {
                return method;
            }
        }

        return ESPECES;
    }

    /**
     * Displays the French label in controls and tables.
     */
    @Override
    public String toString() {
        return label;
    }
}
