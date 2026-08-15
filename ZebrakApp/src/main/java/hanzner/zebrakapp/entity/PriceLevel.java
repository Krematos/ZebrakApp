package hanzner.zebrakapp.entity;

public enum PriceLevel {
    LOW("Levné (€)"),
    VERY_LOW("Velmi levné (€€)"),
    EXTREME("Extrémní výprodej (€€€)");

    private final String label;

    PriceLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
