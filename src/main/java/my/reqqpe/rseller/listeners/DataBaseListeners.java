package my.reqqpe.rseller.listeners;

import lombok.RequiredArgsConstructor;
import my.reqqpe.rseller.database.DataBase;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;


@RequiredArgsConstructor
public class DataBaseListeners implements Listener {

    private final DataBase database;

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
