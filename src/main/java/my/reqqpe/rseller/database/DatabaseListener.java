package my.reqqpe.rseller.database;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public class DatabaseListener implements Listener {
    private final Database database;

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        database.loadPlayerData(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        database.savePlayerDataAsync(e.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onKick(PlayerKickEvent e) {
        database.savePlayerDataAsync(e.getPlayer().getUniqueId());
    }
}
