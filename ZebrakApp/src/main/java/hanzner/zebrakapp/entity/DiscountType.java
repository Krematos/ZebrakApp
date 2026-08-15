package hanzner.zebrakapp.entity;

public enum DiscountType {
    PERMANENT("Trvalé nízké ceny"),
    FLASH_SALES("Nárazové výprodeje / akční dny");

    private final String label;

    DiscountType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
