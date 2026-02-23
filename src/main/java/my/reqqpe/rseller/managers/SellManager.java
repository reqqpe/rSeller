package my.reqqpe.rseller.managers;

import it.unimi.dsi.fastutil.ints.IntList;
import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.database.DataBase;
import my.reqqpe.rseller.hooks.EconomyHook;
import my.reqqpe.rseller.models.Multiplier;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.models.SellableItem;
import my.reqqpe.rseller.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class SellManager {
    private final RSeller plugin;
    private final ItemManager itemManager;
    private final DataBase db;
    private final LevelManager levelManager;
    private final MultiplierManager multiplierManager;

    public SellManager(RSeller plugin, ItemManager itemManager, DataBase db, LevelManager levelManager, MultiplierManager multiplierManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.db = db;
        this.levelManager = levelManager;
        this.multiplierManager = multiplierManager;
    }


    public void sell(Player player, Inventory inv, IntList sellSlots) {
        double totalMoney = 0;
        double totalPoints = 0;

        int finalAmount = 0;
        Map<SellableItem, Integer> sellItems = new HashMap<>();
        for (int slot : sellSlots) {

            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            SellableItem sellableItem = itemManager.getSellableItem(item);
            if (sellableItem == null) {
                continue;
            }

            int amount = item.getAmount();

            if (sellableItem.money() > 0) {
                totalMoney += sellableItem.money() * amount;
            }
            if (sellableItem.points() > 0) {
                totalPoints += sellableItem.points() * amount;
            }

            if (sellableItem.points() > 0 || sellableItem.money() > 0) {
                finalAmount += amount;
                sellItems.merge(sellableItem, amount, Integer::sum);
                sellItems.put(sellableItem, amount);
                inv.setItem(slot, null);
            }
        }

        if (finalAmount == 0) {
            return;
        }
        Multiplier multiplier = multiplierManager.getMultiplierForPlayer(player);
        totalMoney *= multiplier.money();
        totalPoints *= multiplier.points();

        if (totalMoney > 0) {
            EconomyHook.get().depositPlayer(player, totalMoney);
        }
        if (totalPoints > 0) {
            PlayerData pd = db.getPlayerData(player.getUniqueId());
            pd.addPoints(totalPoints);
        }

        String message = MessageUtil.getString("sell-message");

        HashMap<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(finalAmount));
        placeholders.put("money", String.valueOf(totalMoney));
        placeholders.put("points", String.valueOf(totalPoints));

        message = MessageUtil.replacePlaceholders(player, message, placeholders);

        MessageUtil.sendMessage(player, message);

        levelManager.tryLevelUp(player);
    }
}
