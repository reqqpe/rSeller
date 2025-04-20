package my.reqqpe.rseller.configurations;

import my.reqqpe.rseller.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DataBase {

    private final Connection connection;
    private final Main plugin;

    public DataBase(Main plugin,String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        this.plugin = plugin;

        try (Statement statement = connection.createStatement();) {
            statement.execute("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "username TEXT NOT NULL, " +
                    "points INTEGER NOT NULL DEFAULT 0)"
            );
        }
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public void addPlayer(Player player) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO players (uuid, username) VALUES(?, ?)")) {
            statement.setString(1, player.getUniqueId().toString());
            statement.setString(2, player.getName());
            statement.executeUpdate();
        }
    }

    public boolean playerExists(Player player) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
            statement.setString(1, player.getUniqueId().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void updatePlayer(Player player, int points) throws SQLException {

        if (!playerExists(player)) {
            addPlayer(player);
        }

        try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET points = ? WHERE uuid = ?")) {
            statement.setInt(1, points);
            statement.setString(2, player.getUniqueId().toString());
            statement.executeUpdate();
        }
    }

    public int getPoints(Player player) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT points FROM players WHERE uuid = ?")) {
            statement.setString(1, player.getUniqueId().toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("points");
                }
            }
        }
        return 0;
    }
    public void setPoints(Player player, int points) throws SQLException {
        updatePlayer(player, points);
    }

    public void addPoints(Player player, int amount) throws SQLException {
        int current = getPoints(player);
        updatePlayer(player, current + amount);
    }

    public void removePoints(Player player, int amount) throws SQLException {
        int current = getPoints(player);
        updatePlayer(player, Math.max(0, current - amount));
    }
}
