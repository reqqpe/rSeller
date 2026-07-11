package my.reqqpe.rseller.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.managers.NumberFormatManager;
import my.reqqpe.rseller.models.item.Item;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SellerPlaceholderAPI extends PlaceholderExpansion {
    private final Main plugin;
    private final NumberFormatManager numberFormat;
    private final AutoSellManager autoSellManager;

    public SellerPlaceholderAPI(Main plugin, AutoSellManager autoSellManager) {
        this.plugin = plugin;
        this.numberFormat = plugin.getFormatManager();
        this.autoSellManager = autoSellManager;
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

            String[] args = params.split("_");

            switch (args[0]) {
                case "level": {
                    int level = plugin.getLevelManager().getLevel(player);
                    return String.valueOf(level);
                }
                case "points": {
                    if (args.length == 1) {
                        PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());
                        double points = playerData.getPoints();
                        return numberFormat.format("placeholders.points", points);
                    }
                    switch (args[1]) {
                        case "needed": {
                            double points = Math.max(0, plugin.getLevelManager().getPointsForNextLevel(player));
                            return numberFormat.format("placeholders.points_needed", points);
                        }
                        case "fornextlevel": {
                            PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());
                            double points = playerData.getPoints();
                            double pointsForNextLevel = plugin.getLevelManager().getPointsForNextLevel(player);
                            double pointsToNext = Math.max(0, pointsForNextLevel - points);
                            return numberFormat.format("placeholders.points_fornextlevel", pointsToNext);
                        }
                        default:{
                            return "null";
                        }
                    }
                }
                case "multiplier": {
                    switch (args[1]) {
                        case "points": {
                            double multiplier = plugin.getBoosterManager()
                                    .getBoosterByPlayer(player)
                                    .pointMultiplier();
                            return numberFormat.format("placeholders.multiplier_points", multiplier);
                        }
                        case "coins": {
                            double multiplier = plugin.getBoosterManager()
                                    .getBoosterByPlayer(player)
                                    .coinMultiplier();
                            return numberFormat.format("placeholders.multiplier_coins", multiplier);
                        }
                        default: {
                            return "null";
                        }
                    }
                }
                case "autosell": {
                    switch (args[1]) {
                        case "state": {
                            PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());

                            boolean enabled = autoSellManager.getAllItemIds().stream()
                                    .allMatch(playerData::isAutosell);

                            String state = enabled
                                    ? plugin.getMessageConfig().getAutosellEnable()
                                    : plugin.getMessageConfig().getAutosellDisable();

                            return Colorizer.color(state);
                        }
                        case "category": {
                            if (args.length < 3) {
                                return "unknow id";
                            }

                            String id = args[2];
                            String state = !getCategoryState(player, id)
                                    ? plugin.getMessageConfig().getAutosellEnable()
                                    : plugin.getMessageConfig().getAutosellDisable();
                            return Colorizer.color(state);
                        }
                        case "item": {
                            if (args.length < 3) {
                                return "unknow id";
                            }
                            String id = args[2];
                            PlayerData pd = PlayerDataCache.getOrCreate(player.getUniqueId());
                            String state = pd.isAutosell(id)
                                    ? plugin.getMessageConfig().getAutosellEnable()
                                    : plugin.getMessageConfig().getAutosellDisable();
                            return Colorizer.color(state);
                        }
                    }
                }
            }

        }
        return null;
    }



    private boolean getCategoryState(Player player, String categoryId) {
        List<Item> items = autoSellManager.getCategoryItems(categoryId);
        if (items.isEmpty()) return false;

        PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());

        for (Item item : items) {
            if (!playerData.isAutosell(item.id())) {
                return false;
            }
        }
        return true;
    }
}
