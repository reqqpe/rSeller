package my.reqqpe.rseller.models;

public enum SellSource {
    GUI,
    MENU_ACTION,
    AUTOSELL;

    public static SellSource from(String value) {
        if (value == null) return GUI;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GUI;
        }
    }
}
