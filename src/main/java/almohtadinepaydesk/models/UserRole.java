package almohtadinepaydesk.models;

/**
 * Lists the two application roles.
 * These labels are shown in French in the UI, while the enum names stay in English for Java code.
 */
public enum UserRole {
    ADMIN("Administrateur"),
    PERSONNEL("Personnel");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
