package my.reqqpe.rseller.tasks;

import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.database.DataBase;
import my.reqqpe.rseller.hooks.EconomyHook;
import my.reqqpe.rseller.managers.*;
import my.reqqpe.rseller.models.Multiplier;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.models.SellableItem;
import my.reqqpe.rseller.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;

public class AutoSellTask {
    private final RSeller plugin;
    private final MultiplierManager multiplierManager;
    private final DataBase dataBase;
    private final ItemManager itemManager;
    private final LevelManager levelManager;

    private final long timerDelay;

    public AutoSellTask(RSeller plugin, MultiplierManager multiplierManager, ItemManager itemManager, DataBase dataBase, LevelManager levelManager) {
        this.plugin = plugin;

        this.multiplierManager = multiplierManager;
        this.dataBase = dataBase;
        this.itemManager = itemManager;

        this.timerDelay = plugin.getConfig().getLong("auto-sell.delay", 60);
        this.levelManager = levelManager;
    }



    public void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.isBlockedWorld(player.getWorld().getName())) continue;
                    if (!player.hasPermission("rseller.autosell")) continue;

                    PlayerData playerData = dataBase.getPlayerData(player.getUniqueId());
                    if (playerData == null) continue;
                    if (!playerData.hasEnabledAutosell()) continue;

                    PlayerInventory inv = player.getInventory();
                    for (int slot = 0; slot < inv.getSize(); slot++) {
                        ItemStack itemStack = inv.getItem(slot);
                        if (itemStack == null) continue;

                        if (sellItem(player, itemStack)) {
                            inv.setItem(slot, null);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, timerDelay);
    }


    public boolean sellItem(Player player, ItemStack itemStack) {
        SellableItem item = itemManager.getSellableItem(itemStack);
        if (item == null) return false;

        PlayerData playerData = dataBase.getPlayerData(player.getUniqueId());
        if (!playerData.isAutosell(item.id())) return false;

        double price = item.money();
        double points = item.points();

        if (price <= 0 || points <= 0) return false;

        int amount = itemStack.getAmount();

        price *= amount;
        points *= amount;

        Multiplier booster = multiplierManager.getMultiplierForPlayer(player);

        double totalMoney = price * Math.max(1.0, booster.money());
        double totalPoints = points * Math.max(1.0, booster.points());

        if (totalMoney > 0) EconomyHook.get().depositPlayer(player, totalMoney);
        if (totalPoints > 0) playerData.addPoints(totalPoints);

        if (totalMoney > 0 || totalPoints > 0) {

            String msg = MessageUtils.getString("auto-sell-message");

            HashMap<String, String> placeholders = new HashMap<>();
            placeholders.put("money", String.valueOf(totalMoney));
            placeholders.put("points", String.valueOf(totalPoints));
            placeholders.put("item_name", item.displayName());
            placeholders.put("amout", String.valueOf(amount));

            msg = MessageUtils.replacePlaceholders(player, msg, placeholders);

            MessageUtils.sendMessage(player, msg);
        }

        levelManager.tryLevelUp(player);

        return true;
    }
}
