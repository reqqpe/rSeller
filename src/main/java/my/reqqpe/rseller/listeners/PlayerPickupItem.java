package my.reqqpe.rseller.listeners;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.cache.SellDataCache;
import my.reqqpe.rseller.configs.impl.MainConfig;
import my.reqqpe.rseller.managers.SellManager;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.economy.EconomyProvider;
import my.reqqpe.rseller.managers.NumberFormatManager;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.SellData;
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
    private final SellManager sellManager;
    private final boolean inventorySell;

    public PlayerPickupItem(Main plugin, SellManager sellManager, boolean inventorySell) {
        this.plugin = plugin;
        this.sellManager = sellManager;
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

                    if (sellManager.autoSellItem(player, itemStack)) {
                        inv.setItem(slot, null);
                    }
                }
            }, 1L);
        }
        else {
            ItemStack itemStack = event.getItem().getItemStack();

            if (sellManager.autoSellItem(player, itemStack)) {
                event.setCancelled(true);
                event.getItem().remove();
            }
        }
    }
}
