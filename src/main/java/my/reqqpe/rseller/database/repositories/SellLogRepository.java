package my.reqqpe.rseller.database.repositories;

import my.reqqpe.rseller.database.DataBaseManager;
import my.reqqpe.rseller.models.SellLog;
import my.reqqpe.rseller.models.SellSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SellLogRepository {

    private final DataBaseManager dbManager;
    private final String table;

    public SellLogRepository(DataBaseManager dbManager) {
        this.dbManager = dbManager;
        this.table = dbManager.getPREFIX_TABLES() + "sell_log";
    }

    public void createTable() {
        String sql = switch (dbManager.getDbType()) {
            case MYSQL -> """
                    CREATE TABLE IF NOT EXISTS %s (
                        id            BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        uuid          VARCHAR(36)   NOT NULL,
                        player_name   VARCHAR(16)   NOT NULL DEFAULT '',
                        item_id       VARCHAR(64)   NOT NULL,
                        amount        INT           NOT NULL DEFAULT 1,
                        coins         DOUBLE        NOT NULL DEFAULT 0,
                        points        DOUBLE        NOT NULL DEFAULT 0,
                        source        VARCHAR(16)   NOT NULL DEFAULT 'GUI',
                        sold_at       BIGINT        NOT NULL,
                        level         INT           NOT NULL DEFAULT 1,
                        coin_booster  DOUBLE        NOT NULL DEFAULT 1,
                        point_booster DOUBLE        NOT NULL DEFAULT 1,
                        world         VARCHAR(64)   NOT NULL DEFAULT '',
                        INDEX idx_uuid    (uuid),
                        INDEX idx_item_id (item_id),
                        INDEX idx_sold_at (sold_at)
                    )
                    """.formatted(table);
            case POSTGRESQL -> """
                    CREATE TABLE IF NOT EXISTS %s (
                        id            BIGSERIAL        PRIMARY KEY,
                        uuid          VARCHAR(36)      NOT NULL,
                        player_name   VARCHAR(16)      NOT NULL DEFAULT '',
                        item_id       VARCHAR(64)      NOT NULL,
                        amount        INT              NOT NULL DEFAULT 1,
                        coins         DOUBLE PRECISION NOT NULL DEFAULT 0,
                        points        DOUBLE PRECISION NOT NULL DEFAULT 0,
                        source        VARCHAR(16)      NOT NULL DEFAULT 'GUI',
                        sold_at       BIGINT           NOT NULL,
                        level         INT              NOT NULL DEFAULT 1,
                        coin_booster  DOUBLE PRECISION NOT NULL DEFAULT 1,
                        point_booster DOUBLE PRECISION NOT NULL DEFAULT 1,
                        world         VARCHAR(64)      NOT NULL DEFAULT ''
                    );
                    CREATE INDEX IF NOT EXISTS idx_%1$s_uuid     ON %1$s (uuid);
                    CREATE INDEX IF NOT EXISTS idx_%1$s_item_id  ON %1$s (item_id);
                    CREATE INDEX IF NOT EXISTS idx_%1$s_sold_at  ON %1$s (sold_at);
                    """.formatted(table);
            default -> // SQLite
                    """
                    CREATE TABLE IF NOT EXISTS %s (
                        id            INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid          TEXT    NOT NULL,
                        player_name   TEXT    NOT NULL DEFAULT '',
                        item_id       TEXT    NOT NULL,
                        amount        INTEGER NOT NULL DEFAULT 1,
                        coins         REAL    NOT NULL DEFAULT 0,
                        points        REAL    NOT NULL DEFAULT 0,
                        source        TEXT    NOT NULL DEFAULT 'GUI',
                        sold_at       INTEGER NOT NULL,
                        level         INTEGER NOT NULL DEFAULT 1,
                        coin_booster  REAL    NOT NULL DEFAULT 1,
                        point_booster REAL    NOT NULL DEFAULT 1,
                        world         TEXT    NOT NULL DEFAULT ''
                    );
                    CREATE INDEX IF NOT EXISTS idx_%1$s_uuid     ON %1$s (uuid);
                    CREATE INDEX IF NOT EXISTS idx_%1$s_item_id  ON %1$s (item_id);
                    CREATE INDEX IF NOT EXISTS idx_%1$s_sold_at  ON %1$s (sold_at);
                    """.formatted(table);
        };

        CompletableFuture.runAsync(() -> {
            for (String statement : splitStatements(sql)) {
                execute(statement, "create-sell-log-table");
            }
            migrateTable();
        });
    }

    private void migrateTable() {
        migrateColumn("player_name",
                switch (dbManager.getDbType()) {
                    case MYSQL -> "ALTER TABLE %s ADD COLUMN player_name VARCHAR(16) NOT NULL DEFAULT ''".formatted(table);
                    case POSTGRESQL -> "ALTER TABLE %s ADD COLUMN player_name VARCHAR(16) NOT NULL DEFAULT ''".formatted(table);
                    default -> "ALTER TABLE %s ADD COLUMN player_name TEXT NOT NULL DEFAULT ''".formatted(table);
                });
        migrateColumn("level",
                switch (dbManager.getDbType()) {
                    case MYSQL -> "ALTER TABLE %s ADD COLUMN level INT NOT NULL DEFAULT 1".formatted(table);
                    case POSTGRESQL -> "ALTER TABLE %s ADD COLUMN level INT NOT NULL DEFAULT 1".formatted(table);
                    default -> "ALTER TABLE %s ADD COLUMN level INTEGER NOT NULL DEFAULT 1".formatted(table);
                });
        migrateColumn("coin_booster",
                switch (dbManager.getDbType()) {
                    case MYSQL -> "ALTER TABLE %s ADD COLUMN coin_booster DOUBLE NOT NULL DEFAULT 1".formatted(table);
                    case POSTGRESQL -> "ALTER TABLE %s ADD COLUMN coin_booster DOUBLE PRECISION NOT NULL DEFAULT 1".formatted(table);
                    default -> "ALTER TABLE %s ADD COLUMN coin_booster REAL NOT NULL DEFAULT 1".formatted(table);
                });
        migrateColumn("point_booster",
                switch (dbManager.getDbType()) {
                    case MYSQL -> "ALTER TABLE %s ADD COLUMN point_booster DOUBLE NOT NULL DEFAULT 1".formatted(table);
                    case POSTGRESQL -> "ALTER TABLE %s ADD COLUMN point_booster DOUBLE PRECISION NOT NULL DEFAULT 1".formatted(table);
                    default -> "ALTER TABLE %s ADD COLUMN point_booster REAL NOT NULL DEFAULT 1".formatted(table);
                });
        migrateColumn("world",
                switch (dbManager.getDbType()) {
                    case MYSQL -> "ALTER TABLE %s ADD COLUMN world VARCHAR(64) NOT NULL DEFAULT ''".formatted(table);
                    case POSTGRESQL -> "ALTER TABLE %s ADD COLUMN world VARCHAR(64) NOT NULL DEFAULT ''".formatted(table);
                    default -> "ALTER TABLE %s ADD COLUMN world TEXT NOT NULL DEFAULT ''".formatted(table);
                });
    }

    private void migrateColumn(String column, String alterSql) {
        if (columnExists(column)) return;
        execute(alterSql, "migrate-sell-log-" + column);
    }

    private boolean columnExists(String column) {
        try (Connection c = dbManager.getConnection();
             ResultSet rs = c.getMetaData().getColumns(null, null, table, column)) {
            return rs.next();
        } catch (SQLException e) {
            log("column-check-" + column, e);
            return false;
        }
    }

    public void logAsync(SellLog log) {
        CompletableFuture.runAsync(() -> insertLog(log));
    }

    public void logBatchAsync(List<SellLog> logs) {
        if (logs == null || logs.isEmpty()) return;
        CompletableFuture.runAsync(() -> insertBatch(logs));
    }

    private void insertLog(SellLog log) {
        String sql = ("INSERT INTO %s " +
                "(uuid, player_name, item_id, amount, coins, points, source, sold_at, level, coin_booster, point_booster, world) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")
                .formatted(table);

        try (Connection c = dbManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            bindLog(ps, log);
            ps.executeUpdate();

        } catch (SQLException e) {
            log("sell-log-insert", e);
        }
    }

    private void insertBatch(List<SellLog> logs) {
        String sql = ("INSERT INTO %s " +
                "(uuid, player_name, item_id, amount, coins, points, source, sold_at, level, coin_booster, point_booster, world) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")
                .formatted(table);

        try (Connection c = dbManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            c.setAutoCommit(false);
            for (SellLog log : logs) {
                bindLog(ps, log);
                ps.addBatch();
            }
            ps.executeBatch();
            c.commit();

        } catch (SQLException e) {
            log("sell-log-batch-insert", e);
        }
    }

    private static void bindLog(PreparedStatement ps, SellLog log) throws SQLException {
        ps.setString(1, log.uuid().toString());
        ps.setString(2, log.playerName());
        ps.setString(3, log.itemId());
        ps.setInt(4, log.amount());
        ps.setDouble(5, log.coins());
        ps.setDouble(6, log.points());
        ps.setString(7, log.source().name());
        ps.setLong(8, log.soldAt());
        ps.setInt(9, log.level());
        ps.setDouble(10, log.coinBooster());
        ps.setDouble(11, log.pointBooster());
        ps.setString(12, log.world());
    }

    public CompletableFuture<List<SellLog>> getAllLogs(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM %s ORDER BY sold_at DESC LIMIT ?".formatted(table);

            List<SellLog> result = new ArrayList<>();
            try (Connection c = dbManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setInt(1, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(mapRow(rs));
                }

            } catch (SQLException e) {
                log("sell-log-get-all", e);
            }
            return result;
        });
    }

    public CompletableFuture<List<SellLog>> getByPlayer(UUID uuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM %s WHERE uuid = ? ORDER BY sold_at DESC LIMIT ?".formatted(table);

            List<SellLog> result = new ArrayList<>();
            try (Connection c = dbManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());
                ps.setInt(2, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(mapRow(rs));
                }

            } catch (SQLException e) {
                log("sell-log-get-by-player", e);
            }
            return result;
        });
    }

    public CompletableFuture<List<SellLog>> getByPlayerAndItem(UUID uuid, String itemId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM %s WHERE uuid = ? AND item_id = ? ORDER BY sold_at DESC LIMIT ?"
                    .formatted(table);

            List<SellLog> result = new ArrayList<>();
            try (Connection c = dbManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());
                ps.setString(2, itemId);
                ps.setInt(3, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(mapRow(rs));
                }

            } catch (SQLException e) {
                log("sell-log-get-by-player-item", e);
            }
            return result;
        });
    }

    public CompletableFuture<List<SellLog>> getByTimeRange(long from, long to, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM %s WHERE sold_at >= ? AND sold_at <= ? ORDER BY sold_at DESC LIMIT ?"
                    .formatted(table);

            List<SellLog> result = new ArrayList<>();
            try (Connection c = dbManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setLong(1, from);
                ps.setLong(2, to);
                ps.setInt(3, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(mapRow(rs));
                }

            } catch (SQLException e) {
                log("sell-log-get-by-time-range", e);
            }
            return result;
        });
    }

    public CompletableFuture<double[]> getTotalsByPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COALESCE(SUM(coins), 0), COALESCE(SUM(points), 0) FROM %s WHERE uuid = ?"
                    .formatted(table);

            try (Connection c = dbManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new double[]{ rs.getDouble(1), rs.getDouble(2) };
                    }
                }

            } catch (SQLException e) {
                log("sell-log-totals-by-player", e);
            }
            return new double[]{ 0.0, 0.0 };
        });
    }

    public CompletableFuture<Integer> purgeOlderThan(long beforeEpoch) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM %s WHERE sold_at < ?".formatted(table);

            try (Connection c = dbManager.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setLong(1, beforeEpoch);
                return ps.executeUpdate();

            } catch (SQLException e) {
                log("sell-log-purge", e);
                return 0;
            }
        });
    }

    private static SellLog mapRow(ResultSet rs) throws SQLException {
        return new SellLog(
                rs.getLong("id"),
                UUID.fromString(rs.getString("uuid")),
                rs.getString("item_id"),
                rs.getInt("amount"),
                rs.getDouble("coins"),
                rs.getDouble("points"),
                SellSource.from(rs.getString("source")),
                rs.getLong("sold_at"),
                rs.getInt("level"),
                rs.getDouble("coin_booster"),
                rs.getDouble("point_booster"),
                rs.getString("world"),
                rs.getString("player_name")
        );
    }

    private static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        for (String part : sql.split(";")) {
            String trimmed = part.strip();
            if (!trimmed.isEmpty()) statements.add(trimmed);
        }
        return statements;
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
