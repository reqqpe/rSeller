package my.reqqpe.rseller.database.repositories;

import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.database.DataBaseManager;
import my.reqqpe.rseller.models.PlayerData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerRepository {

    private final DataBaseManager dbManager;
    private final String table;

    public PlayerRepository(DataBaseManager dbManager) {
        this.dbManager = dbManager;
        this.table = dbManager.getPREFIX_TABLES() + "players";
    }

    public void createTable() {
        String sql = switch (dbManager.getDbType()) {
            case MYSQL -> """
                CREATE TABLE IF NOT EXISTS %s (
                    uuid VARCHAR(36) PRIMARY KEY,
                    points DOUBLE NOT NULL DEFAULT 0,
                    autosell TEXT,
                    autosell_message TINYINT(1) NOT NULL DEFAULT 1
                )
                """.formatted(table);
            case POSTGRESQL -> """
                CREATE TABLE IF NOT EXISTS %s (
                    uuid VARCHAR(36) PRIMARY KEY,
                    points DOUBLE PRECISION NOT NULL DEFAULT 0,
                    autosell TEXT DEFAULT '{}',
                    autosell_message BOOLEAN NOT NULL DEFAULT TRUE
                )
                """.formatted(table);
            default -> """
                CREATE TABLE IF NOT EXISTS %s (
                    uuid TEXT PRIMARY KEY,
                    points REAL NOT NULL DEFAULT 0,
                    autosell TEXT DEFAULT '{}',
                    autosell_message INTEGER NOT NULL DEFAULT 1
                )
                """.formatted(table);
        };

        CompletableFuture.runAsync(() -> {
            execute(sql, "create-table");
            migrateTable();
        });
    }

    private void migrateTable() {
        if (columnExists("autosell_message")) return;

        String sql = switch (dbManager.getDbType()) {
            case MYSQL      -> "ALTER TABLE %s ADD COLUMN autosell_message TINYINT(1) NOT NULL DEFAULT 1".formatted(table);
            case POSTGRESQL -> "ALTER TABLE %s ADD COLUMN autosell_message BOOLEAN NOT NULL DEFAULT TRUE".formatted(table);
            default         -> "ALTER TABLE %s ADD COLUMN autosell_message INTEGER NOT NULL DEFAULT 1".formatted(table);
        };

        execute(sql, "migrate-table");
    }


    private boolean columnExists(String column) {
        try (Connection c = dbManager.getConnection();
             ResultSet rs = c.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        } catch (SQLException e) {
            log("column-check", e);
            return false;
        }
    }

    public void loadPlayerData(UUID uuid) {
        PlayerData data = PlayerDataCache.getOrCreate(uuid);

        CompletableFuture.runAsync(() -> {
            String sql = "SELECT points, autosell, autosell_message FROM %s WHERE uuid=?".formatted(table);

            try (Connection c = dbManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        data.setPoints(rs.getDouble("points"));

                        String auto = rs.getString("autosell");
                        data.deserializeAutosell(auto == null ? "{}" : auto);

                        data.setAutosellMessage(rs.getBoolean("autosell_message"));
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
                INSERT INTO %s (uuid, points, autosell, autosell_message)
                VALUES (?,?,?,?)
                ON DUPLICATE KEY UPDATE
                    points = VALUES(points),
                    autosell = VALUES(autosell),
                    autosell_message = VALUES(autosell_message)
                """.formatted(table);
            case POSTGRESQL -> """
                INSERT INTO %s (uuid, points, autosell, autosell_message)
                VALUES (?,?,?,?)
                ON CONFLICT (uuid)
                DO UPDATE SET
                    points = EXCLUDED.points,
                    autosell = EXCLUDED.autosell,
                    autosell_message = EXCLUDED.autosell_message
                """.formatted(table);
            default -> """
                INSERT OR REPLACE INTO %s (uuid, points, autosell, autosell_message)
                VALUES (?,?,?,?)
                """.formatted(table);
        };

        try (Connection c = dbManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.setDouble(2, data.getPoints());
            ps.setString(3, data.serializeAutosell());
            ps.setBoolean(4, data.isAutosellMessage());

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

    public CompletableFuture<Void> savePlayerDataAsync(UUID uuid) {
        return CompletableFuture.runAsync(() -> savePlayerData(uuid));
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
