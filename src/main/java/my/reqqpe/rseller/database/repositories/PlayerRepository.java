package my.reqqpe.rseller.database.repositories;

import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.database.DataBaseManager;
import my.reqqpe.rseller.database.PlayerData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerRepository {

    private final DataBaseManager dbManager;

    public PlayerRepository(DataBaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void createTable() {

        String sql = switch (dbManager.getDbType()) {

            case MYSQL -> """
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    points DOUBLE NOT NULL DEFAULT 0,
                    autosell TEXT
                )
                """;

            case POSTGRESQL -> """
                CREATE TABLE IF NOT EXISTS players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    points DOUBLE PRECISION NOT NULL DEFAULT 0,
                    autosell TEXT DEFAULT '{}'
                )
                """;

            default -> """
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    points REAL NOT NULL DEFAULT 0,
                    autosell TEXT DEFAULT '{}'
                )
                """;
        };

        CompletableFuture.runAsync(() -> execute(sql, "create-table"));
    }

    public void loadPlayerData(UUID uuid) {

        PlayerData data = PlayerDataCache.getOrCreate(uuid);

        CompletableFuture.runAsync(() -> {

            String sql = "SELECT points, autosell FROM players WHERE uuid=?";

            try (Connection c = dbManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {

                        data.setPoints(rs.getDouble("points"));

                        String auto = rs.getString("autosell");
                        data.deserializeAutosell(auto == null ? "{}" : auto);
                    }
                }

            } catch (SQLException e) {
                log("database-load-error", e);
            }
        });
    }


    public void savePlayerData(UUID uuid) {

        PlayerData data = PlayerDataCache.getOrCreate(uuid);

        String sql = switch (dbManager.getDbType()) {

            case MYSQL -> """
                INSERT INTO players (uuid, points, autosell)
                VALUES (?,?,?)
                ON DUPLICATE KEY UPDATE
                    points = VALUES(points),
                    autosell = VALUES(autosell)
                """;

            case POSTGRESQL -> """
                INSERT INTO players (uuid, points, autosell)
                VALUES (?,?,?)
                ON CONFLICT (uuid)
                DO UPDATE SET
                    points = EXCLUDED.points,
                    autosell = EXCLUDED.autosell
                """;

            default -> """
                INSERT OR REPLACE INTO players (uuid, points, autosell)
                VALUES (?,?,?)
                """;
        };

        try (Connection c = dbManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setDouble(2, data.getPoints());
            ps.setString(3, data.serializeAutosell());

            ps.executeUpdate();

        } catch (SQLException e) {
            log("database-save-error", e);
        }
    }


    public void saveAll() {
        for (PlayerData data : PlayerDataCache.getCache().values()) {
            savePlayerData(data.getUuid());
        }
    }


    public void savePlayerDataAsync(UUID uuid) {
        CompletableFuture.runAsync(() -> savePlayerData(uuid));
    }

    private void execute(String sql, String action) {
        try (Connection c = dbManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.executeUpdate();

        } catch (SQLException e) {
            log(action, e);
        }
    }

    private void log(String action, Exception e) {
        dbManager.getPlugin().getLogger().severe(
                "[DB:" + action + "] " + e.getMessage()
        );
    }
}