package my.reqqpe.rseller.database;

public enum DataBaseType {
    SQLITE,
    MYSQL,
    POSTGRESQL;


    public static DataBaseType from(String value) {
        if (value == null) return SQLITE;

        try {
            return valueOf(value.toUpperCase());
        } catch (Exception e) {
            return SQLITE;
        }
    }
}
