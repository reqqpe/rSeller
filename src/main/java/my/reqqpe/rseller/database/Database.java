package my.reqqpe.rseller.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import my.reqqpe.rseller.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Вместо того чтобы постоянно лезть в бд мы будем загружать данные игрока при входе на сервер
// Создавать объект с полями которого мы будем работать и при выходе игрока с сервера сохранять данные
public class Database {
    private final Main plugin;
    private final Set<PlayerData> players = new HashSet<>();
    private HikariDataSource dataSource;

    public PlayerData getPlayerData(UUID uuid) {
        return players.stream()
                .filter(data -> data.getUuid().equals(uuid))
                .findFirst()
                //Можно поменять на null
                .orElse(create(uuid));
    }

    public PlayerData create(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        players.add(data);
        return data;
    }

    public Database(Main plugin) {
        this.plugin = plugin;
        initPool(plugin.getConfig());
        createTable();
    }

    private void initPool(FileConfiguration config) {
        String path = plugin.getDataFolder() + "/data.db";
        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:sqlite:" + path);

        ConfigurationSection data = config.getConfigurationSection("database");
        hc.setMaximumPoolSize(data.getInt("max-pool-size"));
        hc.setIdleTimeout(data.getLong("idle-timeout"));
        hc.setConnectionTimeout(data.getLong("connection-timeout"));
        hc.setKeepaliveTime(data.getLong("keep-alive"));
        hc.setMaxLifetime(data.getInt("max-life-time"));
        hc.setPoolName("rSellerPool");
        dataSource = new HikariDataSource(hc);
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid TEXT PRIMARY KEY, " +
                "points INTEGER NOT NULL DEFAULT 0)";
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
        String sql = "SELECT points FROM players WHERE uuid=?";
        PlayerData data = create(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, String.valueOf(uuid));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        data.setPoints(rs.getInt("points"));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Ошибка при загрузке данных игрока " + e);
            }
        });
    }

    public void savePlayerData(UUID uuid) {
        String sql = "INSERT OR REPLACE INTO players (uuid, points) VALUES (?,?)";
        PlayerData data = getPlayerData(uuid);
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, String.valueOf(uuid));
            ps.setInt(2, data.getPoints());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка при сохранении данных игрока " + e);
        }
    }

    public void saveAll() {
        for (PlayerData data : players) savePlayerData(data.getUuid());
        if (dataSource != null) dataSource.close();
    }

    public void savePlayerDataAsync(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            savePlayerData(uuid);
        });
    }
}
