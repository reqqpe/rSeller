package my.reqqpe.rseller.api;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.menu.AbstractMenu;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.SellLog;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface rSellerAPI {

    double getPoints(UUID uuid);

    void addPoints(UUID uuid, double amount);

    void removePoints(UUID uuid, double amount);

    void setPoints(UUID uuid, double amount);

    int getLevel(Player player);

    double getPointsForNextLevel(Player player);

    Booster getBooster(Player player);

    double getCoinMultiplier(Player player);

    double getPointMultiplier(Player player);

    boolean isAutosellEnabled(UUID uuid, String itemId);

    void setAutosell(UUID uuid, String itemId, boolean enabled);

    boolean sellItem(Player player, String itemId, int amount);

    boolean sellAll(Player player, ItemStack itemStack);

    CompletableFuture<List<SellLog>> getLogs(UUID uuid, int limit);

    CompletableFuture<List<SellLog>> getAllLogs(int limit);

    CompletableFuture<double[]> getTotals(UUID uuid);

    void registerMenu(String id, AbstractMenu menu);

    void unregisterMenu(String id);

    void openMenu(String id, Player player);

    AbstractMenu getMenu(String id);

    Main getPlugin();
}
