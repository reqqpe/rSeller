package my.reqqpe.rseller.configs.impl;

import lombok.Getter;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.configs.CustomConfig;
import my.reqqpe.rseller.database.DataBaseType;


@Getter
public class DataBaseConfig extends CustomConfig {

    private DataBaseType dbType;
    private String tablePrefix;

    private int maxPoolSize;
    private int minimumIdle;
    private int keepAlive;
    private int idleTimeout;
    private int connectionTimeout;
    private int maxLifeTime;

    private String host;
    private int port;
    private String database;
    private String userName;
    private String password;

    public DataBaseConfig(Main plugin) {
        super(plugin, "database.yml");
    }

    @Override
    protected void load() {

        this.dbType = DataBaseType.from(config.getString("type"));
        this.tablePrefix = getString("table_prefix", "rseller_");


        String hikariPath = "hikari.";
        this.maxPoolSize = getInt(hikariPath + "max-pool-size", 10);
        this.minimumIdle = getInt(hikariPath + "minimum-idle", 2);
        this.keepAlive = getInt(hikariPath + "hikari.keep-alive", 120000);
        this.idleTimeout = getInt(hikariPath + "hikari.idle-timeout", 60000);
        this.connectionTimeout = getInt(hikariPath + "hikari.connection-timeout", 30000);
        this.maxLifeTime = getInt(hikariPath + "hikari.max-life-time", 1800000);

        String connectionPath = "connection.";
        this.host = getString(connectionPath + "host", "localhost");
        this.port = getInt(connectionPath + "port", 3306);
        this.database = getString(connectionPath + "database", "rseller");
        this.userName = getString(connectionPath + "username", "root");
        this.password = getString(connectionPath + "password", "");
    }

}
