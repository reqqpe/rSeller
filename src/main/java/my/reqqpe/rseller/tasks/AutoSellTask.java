package my.reqqpe.rseller.tasks;

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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class AutoSellTask {

    private final Main plugin;
    private final SellManager sellManager;

    public AutoSellTask(Main plugin, SellManager sellManager) {
        this.plugin = plugin;
        this.sellManager = sellManager;
    }


    public void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.isBlockedWorld(player.getWorld().getName())) continue;
                    if (!player.hasPermission("rseller.autosell")) continue;

                    PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());
                    if (playerData == null) continue;
                    if (!playerData.hasEnabledAutosell()) continue;

                    PlayerInventory inv = player.getInventory();
                    for (int slot = 0; slot < inv.getSize(); slot++) {
                        ItemStack itemStack = inv.getItem(slot);

                        if (itemStack == null) continue;

                        if (sellManager.autoSellItem(player, itemStack)) {
                            inv.setItem(slot, null);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, plugin.getMainConfig().getAutosell().getTaskDelay() * 20L);
    }
}
