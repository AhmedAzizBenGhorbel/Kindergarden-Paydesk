package almohtadinepaydesk.models;

/**
 * Stores the allowed monthly payment states.
 * The app only uses these four values, and overpayment still stays in Payé with an advance amount.
 */
public enum PaymentStatus {
    PAYE("Pay\u00e9"),
    NON_PAYE("Non pay\u00e9"),
    PARTIEL("Partiel"),
    EN_RETARD("En retard");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    /**
     * Returns the French display label used in the UI.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Converts a label read from the database or UI back into the enum.
     *
     * @param label French status label
     * @return the matching status, or NON_PAYE when nothing matches
     */
    public static PaymentStatus fromLabel(String label) {
        for (PaymentStatus status : values()) {
            if (status.getLabel().equals(label)) {
                return status;
            }
        }

        return NON_PAYE;
    }

    /**
     * Makes combo boxes and tables show the French label instead of the enum name.
     */
    @Override
    public String toString() {
        return label;
    }
}
