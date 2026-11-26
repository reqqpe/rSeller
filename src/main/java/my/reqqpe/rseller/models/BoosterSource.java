package my.reqqpe.rseller.models;

public enum BoosterSource {
    ALL, GUI, MENU_ACTION, AUTOSELL;

    public static BoosterSource from(String value) {
        if (value == null) return ALL;
        return switch (value.toUpperCase()) {
            case "GUI" -> GUI;
            case "MENU_ACTION" -> MENU_ACTION;
            case "AUTOSELL" -> AUTOSELL;
            default -> ALL;
        };
    }

    public boolean appliesTo(SellSource sellSource) {
        return switch (this) {
            case ALL -> true;
            case GUI -> sellSource == SellSource.GUI;
            case MENU_ACTION -> sellSource == SellSource.MENU_ACTION;
            case AUTOSELL -> sellSource == SellSource.AUTOSELL;
        };
    }
}
