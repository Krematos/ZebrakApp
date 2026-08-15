package hanzner.zebrakapp.entity;

public enum Category {
    FOOD("Potraviny & záchrana jídla", "zlevněné potraviny, velkosklady, prošlé trvanlivosti"),
    SECOND_HAND("Second hand & textil", "sekáče, hrabáky, levná móda"),
    OUTLET("Outlet & výprodeje", "značkové outlety, sezónní výprodeje"),
    PALLET_GOODS("Paletový prodej & vrácené zboží", "nerozbalené/vrácené balíky z e-shopů"),
    FACTORY_STORE("Podnikové prodejny", "prodej přímo z výroby za velkoobchodní ceny"),
    FURNITURE_BAZAAR("Nábytek & bazar", "levné vybavení domácnosti, nábytkové bazary"),
    DRUGSTORE("Drogerie & kosmetika", "dovozová a zlevněná drogerie"),
    OTHER("Ostatní levné nákupy", "různé další výhodné nákupy");

    private final String label;
    private final String description;

    Category(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
