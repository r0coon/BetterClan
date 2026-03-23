package com.betterclan.storage;

public enum Dialect {

    MARIADB("org.mariadb.jdbc.Driver", "jdbc:mariadb://"),
    MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://"),
    POSTGRES("org.postgresql.Driver", "jdbc:postgresql://");

    private final String driverClass;
    private final String urlPrefix;

    Dialect(String driverClass, String urlPrefix) {
        this.driverClass = driverClass;
        this.urlPrefix = urlPrefix;
    }

    public String driverClass() { return driverClass; }
    public String urlPrefix()   { return urlPrefix; }

    public String autoIncrement() {
        return this == POSTGRES ? "SERIAL" : "INT AUTO_INCREMENT";
    }

    public String blobType() {
        return this == POSTGRES ? "BYTEA" : "MEDIUMBLOB";
    }

    public String boolType() {
        return this == POSTGRES ? "BOOLEAN" : "TINYINT(1)";
    }

    @SuppressWarnings("unused")
    public String upsertSuffix(String conflictCol, String updatePart) {
        if (this == POSTGRES) {
            return " ON CONFLICT (" + conflictCol + ") DO UPDATE SET " + updatePart;
        }
        return " ON DUPLICATE KEY UPDATE " + updatePart;
    }

    public static Dialect fromString(String s) {
        if (s == null) return MARIADB;
        return switch (s.toLowerCase().trim()) {
            case "mysql" -> MYSQL;
            case "postgres", "postgresql" -> POSTGRES;
            default -> MARIADB;
        };
    }
}

