package my.reqqpe.rseller.tasks;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.managers.SellManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoSellTask {
    private final Main plugin;
    private long updateTime;
    private final SellManager sellManager;

    public AutoSellTask(Main plugin, SellManager sellManager) {
        this.plugin = plugin;
        this.updateTime = plugin.getConfig().getLong("autosell.check-interval-tick");
        this.sellManager = sellManager;
    }

    public void autoSellTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    sellManager.autoSell(player);
                }
            }
        }.runTaskTimer(plugin, 0L, updateTime);

    }
}
