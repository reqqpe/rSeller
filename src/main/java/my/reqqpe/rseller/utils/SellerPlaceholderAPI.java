package my.reqqpe.rseller.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.NumberFormatManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SellerPlaceholderAPI extends PlaceholderExpansion {
    private final Main plugin;
    private final Database database;
    private final NumberFormatManager numberFormat;

    public SellerPlaceholderAPI(Main plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.numberFormat = plugin.getFormatManager();
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors()); //
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "rseller";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer.getPlayer() != null && offlinePlayer.getPlayer().isOnline()) {

            Player player = offlinePlayer.getPlayer();
            if (params.equalsIgnoreCase("level")) {
                int level = plugin.getLevelManager().getLevel(player);
                return String.valueOf(level);
            }
            if (params.equalsIgnoreCase("points")) {
                PlayerData playerData = database.getPlayerData(player.getUniqueId());
                if (playerData != null) {
                    double points = playerData.getPoints();
                    return String.format(numberFormat.format("placeholders.points", points));
                }
            }
            if (params.equalsIgnoreCase("points_needed")) {
                double points = Math.max(0, plugin.getLevelManager().getPointsForNextLevel(player));
                return numberFormat.format("placeholders.points_needed", points);
            }
            if (params.equalsIgnoreCase("points_fornextlevel")) {
                PlayerData playerData = database.getPlayerData(player.getUniqueId());
                if (playerData != null) {
                    double points = playerData.getPoints();
                    double pointsForNextLevel = plugin.getLevelManager().getPointsForNextLevel(player);
                    double pointsToNext = Math.max(0, pointsForNextLevel - points);
                    return numberFormat.format("placeholders.points_fornextlevel", pointsToNext);
                }

            }
            if (params.equalsIgnoreCase("multiplier_points")) {
                double multiplier = plugin.getBoosterManager()
                        .getBoosterByPlayer(player)
                        .pointMultiplier();
                return numberFormat.format("placeholders.multiplier_points", multiplier);
            }
            if (params.equalsIgnoreCase("multiplier_coins")) {
                double multiplier = plugin.getBoosterManager()
                        .getBoosterByPlayer(player)
                        .coinMultiplier();
                return numberFormat.format("placeholders.multiplier_coins", multiplier);
            }
        }
        return null;
    }
}
