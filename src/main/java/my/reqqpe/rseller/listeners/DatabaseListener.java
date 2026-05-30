package my.reqqpe.rseller.listeners;

import lombok.RequiredArgsConstructor;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.database.repositories.PlayerRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

@RequiredArgsConstructor
public class DatabaseListener implements Listener {
    private final PlayerRepository playerRepository;

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        playerRepository.loadPlayerData(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        playerRepository.savePlayerDataAsync(uuid)
                .thenRun(() -> PlayerDataCache.remove(uuid));
    }

    @EventHandler(ignoreCancelled = true)
    public void onKick(PlayerKickEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        playerRepository.savePlayerDataAsync(uuid)
                .thenRun(() -> PlayerDataCache.remove(uuid));
    }
}
