package my.reqqpe.rseller.listeners;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.economy.EconomyProvider;
import my.reqqpe.rseller.managers.NumberFormatManager;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;


public class PlayerPickupItem implements Listener {
    private final Main plugin;
    private final Database database;
    private final EconomyProvider economy;
    private final NumberFormatManager numberFormatManager;
    private final boolean inventorySell;

    public PlayerPickupItem(Main plugin, Database database, boolean inventorySell) {
        this.plugin = plugin;
        this.database = database;
        this.economy = plugin.getEconomy();
        this.numberFormatManager = plugin.getFormatManager();
        this.inventorySell = inventorySell;
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
        Item item = plugin.getItemManager().search(itemStack);
        if (item == null) return false;

        PlayerData playerData = database.getPlayerData(player.getUniqueId());
        if (!playerData.isAutosell(item.id())) return false;

        double price = item.price();
        double points = item.points();

        if (price <= 0 && points <= 0) return false;

        int amount = itemStack.getAmount();

        price *= amount;
        points *= amount;

        Booster booster = plugin.getBoosterManager().getBoosterByPlayer(player);
        if (booster == null) {
            booster = new Booster(1.0, 1.0);
        }

        double totalCoins = price * Math.max(1.0, booster.coinMultiplier());
        double totalPoints = points * Math.max(1.0, booster.pointMultiplier());

        if (totalCoins > 0) economy.deposit(player, totalCoins);
        if (totalPoints > 0) playerData.addPoints(totalPoints);

        if (totalCoins > 0 || totalPoints > 0) {
            String coinsFormat = numberFormatManager.format("messages.coins", totalCoins);
            String pointsFormat = numberFormatManager.format("messages.points", totalPoints);

            String msg = Colorizer.color(plugin.getConfig().getString("messages.auto-sell")
                    .replace("{coins}", coinsFormat)
                    .replace("{points}", pointsFormat)
                    .replace("{item_name}", item.getDisplayName(plugin))
                    .replace("{amount}", String.valueOf(amount)));

            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(msg);
            }
        }

        return true;
    }

}
