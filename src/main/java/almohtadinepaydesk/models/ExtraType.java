package almohtadinepaydesk.models;

/**
 * Distinguishes one-time extras from recurring extras.
 * This affects how the fee is applied across months.
 */
public enum ExtraType {
    PONCTUEL("Ponctuel"),
    RECURRENT("R\u00e9current");

    private final String label;

    ExtraType(String label) {
        this.label = label;
    }

    /**
     * Returns the French label shown in the UI.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Converts a French label back into the enum.
     *
     * @param label French type label
     * @return matching type, or PONCTUEL when nothing matches
     */
    public static ExtraType fromLabel(String label) {
        for (ExtraType type : values()) {
            if (type.getLabel().equals(label)) {
                return type;
            }
        }

        return PONCTUEL;
    }

    /**
     * Displays the French label in combo boxes and tables.
     */
    @Override
    public String toString() {
        return label;
    }
}
