package my.reqqpe.rseller.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import my.reqqpe.rseller.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Database {
    private final Main plugin;
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private HikariDataSource dataSource;
    private DBType dbType;

    public PlayerData getPlayerData(UUID uuid) {
        PlayerData playerData = players.get(uuid);
        return playerData != null ? playerData : create(uuid);
    }

    public PlayerData create(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        players.put(uuid, data);
        return data;
    }

    public Database(Main plugin) {
        this.plugin = plugin;
        try {
            initPool(plugin.getConfig());
        }  catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        createTable();
    }

    private void initPool(FileConfiguration config) throws ClassNotFoundException {
        ConfigurationSection db = config.getConfigurationSection("database");
        String type = db.getString("type", "sqlite").toLowerCase();

        ConfigurationSection hikari = db.getConfigurationSection("hikari");
        ConfigurationSection conn = db.getConfigurationSection("connection");

        HikariConfig hc = new HikariConfig();

        switch (type) {
            case "mysql" -> {
                this.dbType = DBType.MYSQL;
                Class.forName("com.mysql.cj.jdbc.Driver");
                String host = conn.getString("host", "localhost");
                int port = conn.getInt("port", 3306);
                String database = conn.getString("database", "rseller");
                String username = conn.getString("username", "root");
                String password = conn.getString("password", "");
                hc.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
                hc.setUsername(username);
                hc.setPassword(password);
            }
            case "postgres", "postgresql" -> {
                this.dbType = DBType.POSTGRESQL;
                Class.forName("org.postgresql.Driver");
                String host = conn.getString("host", "localhost");
                int port = conn.getInt("port", 5432);
                String database = conn.getString("database", "rseller");
                String username = conn.getString("username", "postgres");
                String password = conn.getString("password", "");
                hc.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
                hc.setUsername(username);
                hc.setPassword(password);
            }
            default -> {
                this.dbType = DBType.SQLITE;
                Class.forName("org.sqlite.JDBC");
                String path = plugin.getDataFolder() + "/data.db";
                hc.setJdbcUrl("jdbc:sqlite:" + path);
            }
        }

        // Настройки HikariCP
        hc.setMaximumPoolSize(hikari.getInt("max-pool-size", 10));
        hc.setIdleTimeout(hikari.getLong("idle-timeout", 600000));
        hc.setConnectionTimeout(hikari.getLong("connection-timeout", 30000));
        hc.setKeepaliveTime(hikari.getLong("keep-alive", 0));
        hc.setMaxLifetime(hikari.getLong("max-life-time", 1800000));
        hc.setPoolName("rSellerPool");

        dataSource = new HikariDataSource(hc);
    }

    private void createTable() {
        String sql;
        switch (dbType) {
            case MYSQL -> sql = "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "points DOUBLE NOT NULL DEFAULT 0, " +
                    "autosell TEXT" +
                    ")";
            case POSTGRESQL -> sql = "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "points DOUBLE PRECISION NOT NULL DEFAULT 0, " +
                    "autosell TEXT DEFAULT '{}'" +
                    ")";
            default -> sql = "CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "points REAL NOT NULL DEFAULT 0, " +
                    "autosell TEXT DEFAULT '{}'" +
                    ")";
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Ошибка при создании таблицы " + e);
            }
        });
    }

    public void loadPlayerData(UUID uuid) {
        String sql = "SELECT points, autosell FROM players WHERE uuid=?";
        PlayerData data = create(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, String.valueOf(uuid));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        data.setPoints(rs.getDouble("points"));
                        data.deserializeAutosell(rs.getString("autosell"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Ошибка при загрузке данных игрока " + e);
            }
        });
    }

    public void savePlayerData(UUID uuid) {
        String sql;
        switch (dbType) {
            case MYSQL -> sql = "INSERT INTO players (uuid, points, autosell) VALUES (?,?,?) " +
                    "ON DUPLICATE KEY UPDATE points=VALUES(points), autosell=VALUES(autosell)";
            case POSTGRESQL -> sql = "INSERT INTO players (uuid, points, autosell) VALUES (?,?,?) " +
                    "ON CONFLICT (uuid) DO UPDATE SET points=EXCLUDED.points, autosell=EXCLUDED.autosell";
            default -> sql = "INSERT OR REPLACE INTO players (uuid, points, autosell) VALUES (?,?,?)";
        }
        PlayerData data = getPlayerData(uuid);
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(uuid));
            ps.setDouble(2, data.getPoints());
            ps.setString(3, data.serializeAutosell());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка при сохранении данных игрока " + e);
        }
    }

    public void saveAll() {
        for (PlayerData data : players.values()) savePlayerData(data.getUuid());
        if (dataSource != null) dataSource.close();
    }

    public void savePlayerDataAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerData(uuid));
    }

    private enum DBType {
        SQLITE,
        MYSQL,
        POSTGRESQL
    }
}