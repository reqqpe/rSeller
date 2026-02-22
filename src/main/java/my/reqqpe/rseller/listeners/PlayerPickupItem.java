package my.reqqpe.rseller.listeners;

import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.database.DataBase;
import my.reqqpe.rseller.hooks.EconomyHook;
import my.reqqpe.rseller.managers.ItemManager;
import my.reqqpe.rseller.managers.LevelManager;
import my.reqqpe.rseller.managers.MultiplierManager;
import my.reqqpe.rseller.models.Multiplier;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.models.SellableItem;
import my.reqqpe.rseller.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;

public class PlayerPickupItem implements Listener {


    private final RSeller plugin;
    private final ItemManager itemManager;
    private final MultiplierManager multiplierManager;
    private final DataBase dataBase;
    private final LevelManager levelManager;


    private final boolean inventorySell;

    public PlayerPickupItem(RSeller plugin, ItemManager itemManager, MultiplierManager multiplierManager, DataBase dataBase, LevelManager levelManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.multiplierManager = multiplierManager;
        this.dataBase = dataBase;

        this.inventorySell = plugin.getConfig().getBoolean("autosell.inventory_mode", false);
        this.levelManager = levelManager;
    }


    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {

        if (plugin.isBlockedWorld(event.getEntity().getWorld().getName())) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.hasPermission("rseller.autosell")) return;

        if (inventorySell) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                PlayerInventory inv = player.getInventory();

                for (int slot = 0; slot < inv.getSize(); slot++) {
                    ItemStack itemStack = inv.getItem(slot);
                    if (itemStack == null) continue;

                    if (sellItem(player, itemStack)) {
                        inv.setItem(slot, null);
                    }
                }
            }, 1L);
        }
        else {
            ItemStack itemStack = event.getItem().getItemStack();

            if (sellItem(player, itemStack)) {
                event.setCancelled(true);
                event.getItem().remove();
            }
        }
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
