package my.reqqpe.rseller.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.configs.impl.DataBaseConfig;

import java.sql.Connection;
import java.sql.SQLException;

@Getter
public class DataBaseManager {

    private final Main plugin;
    private final DataBaseConfig config;

    private HikariDataSource dataSource;
    private DataBaseType dbType;

    private final String PREFIX_TABLES;

    public DataBaseManager(Main plugin, DataBaseConfig config) {
        this.plugin = plugin;
        this.config = config;

        this.PREFIX_TABLES = config.getTablePrefix();
        initPool();
    }

    public void initPool() {

        HikariConfig hc = new HikariConfig();

        this.dbType = config.getDbType();

        switch (dbType) {

            case MYSQL -> {
                loadDriver("com.mysql.cj.jdbc.Driver");

                hc.setJdbcUrl("jdbc:mysql://" +
                        config.getHost() + ":" +
                        config.getPort() + "/" +
                        config.getDatabase());

                hc.setUsername(config.getUserName());
                hc.setPassword(config.getPassword());
            }

            case POSTGRESQL -> {
                loadDriver("org.postgresql.Driver");

                hc.setJdbcUrl("jdbc:postgresql://" +
                        config.getHost() + ":" +
                        config.getPort() + "/" +
                        config.getDatabase());

                hc.setUsername(config.getUserName());
                hc.setPassword(config.getPassword());
            }

            case SQLITE -> {
                loadDriver("org.sqlite.JDBC");

                String path = plugin.getDataFolder() + "/data.db";
                hc.setJdbcUrl("jdbc:sqlite:" + path);
            }
        }

        hc.setMaximumPoolSize(config.getMaxPoolSize());
        hc.setIdleTimeout(config.getIdleTimeout());
        hc.setConnectionTimeout(config.getConnectionTimeout());
        hc.setKeepaliveTime(config.getKeepAlive());
        hc.setMaxLifetime(config.getMaxLifeTime());

        this.dataSource = new HikariDataSource(hc);

        plugin.getLogger().info(plugin.getMessageConfig().getConsoleDatabaseInitialized().replace("{type}", dbType.name()));
    }

    private void loadDriver(String driver) {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Missing JDBC driver: " + driver, e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}