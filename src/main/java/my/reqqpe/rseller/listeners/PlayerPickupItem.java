package my.reqqpe.rseller.listeners;

import my.reqqpe.rseller.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class PlayerPickupItem implements Listener {
    private final Main plugin;

    public PlayerPickupItem(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.hasPermission("rseller.autosell")) return;

        Bukkit.getScheduler().runTask(plugin, () -> plugin.getSellManager().autoSell(player));

    }
}
