package my.reqqpe.rseller.api;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.managers.MenuManager;
import my.reqqpe.rseller.menu.AbstractMenu;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.models.SellLog;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class rSellerAPIImpl implements rSellerAPI {

    private final Main plugin;

    rSellerAPIImpl(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public double getPoints(UUID uuid) {
        return PlayerDataCache.getOrCreate(uuid).getPoints();
    }

    @Override
    public void addPoints(UUID uuid, double amount) {
        PlayerDataCache.getOrCreate(uuid).addPoints(amount);
    }

    @Override
    public void removePoints(UUID uuid, double amount) {
        PlayerDataCache.getOrCreate(uuid).removePoints(amount);
    }

    @Override
    public void setPoints(UUID uuid, double amount) {
        PlayerDataCache.getOrCreate(uuid).setPoints(amount);
    }

    @Override
    public int getLevel(Player player) {
        return plugin.getLevelManager().getLevel(player);
    }

    @Override
    public double getPointsForNextLevel(Player player) {
        return plugin.getLevelManager().getPointsForNextLevel(player);
    }

    @Override
    public Booster getBooster(Player player) {
        return plugin.getBoosterManager().getBoosterByPlayer(player);
    }

    @Override
    public double getCoinMultiplier(Player player) {
        return getBooster(player).coinMultiplier();
    }

    @Override
    public double getPointMultiplier(Player player) {
        return getBooster(player).pointMultiplier();
    }

    @Override
    public boolean isAutosellEnabled(UUID uuid, String itemId) {
        return PlayerDataCache.getOrCreate(uuid).isAutosell(itemId);
    }

    @Override
    public void setAutosell(UUID uuid, String itemId, boolean enabled) {
        PlayerData data = PlayerDataCache.getOrCreate(uuid);
        data.setAutosell(itemId, enabled);
    }

    @Override
    public boolean sellItem(Player player, String itemId, int amount) {
        return plugin.getSellManager().sellItem(player, itemId, amount);
    }

    @Override
    public boolean sellAll(Player player, ItemStack itemStack) {
        return plugin.getSellManager().autoSellItem(player, itemStack);
    }

    @Override
    public CompletableFuture<List<SellLog>> getLogs(UUID uuid, int limit) {
        if (plugin.getSellLogRepository() == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return plugin.getSellLogRepository().getByPlayer(uuid, limit);
    }

    @Override
    public CompletableFuture<List<SellLog>> getAllLogs(int limit) {
        if (plugin.getSellLogRepository() == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return plugin.getSellLogRepository().getAllLogs(limit);
    }

    @Override
    public CompletableFuture<double[]> getTotals(UUID uuid) {
        if (plugin.getSellLogRepository() == null) {
            return CompletableFuture.completedFuture(new double[]{0.0, 0.0});
        }
        return plugin.getSellLogRepository().getTotalsByPlayer(uuid);
    }

    @Override
    public void registerMenu(String id, AbstractMenu menu) {
        MenuManager.registerMenu(id, menu);
    }

    @Override
    public void unregisterMenu(String id) {
        MenuManager.unRegisterMenu(id);
    }

    @Override
    public void openMenu(String id, Player player) {
        AbstractMenu menu = MenuManager.getMenu(id);
        if (menu != null) {
            menu.openMenu(player);
        }
    }

    @Override
    public AbstractMenu getMenu(String id) {
        return MenuManager.getMenu(id);
    }

    @Override
    public Main getPlugin() {
        return plugin;
    }
}
