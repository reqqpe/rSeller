package my.reqqpe.rseller.listeners;

import lombok.RequiredArgsConstructor;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.database.repositories.PlayerRepository;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.models.PlayerData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class DatabaseListener implements Listener {

    private final Main plugin;
    private final PlayerRepository playerRepository;
    private final AutoSellManager autoSellManager;

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        playerRepository.loadPlayerData(uuid);

        PlayerData playerData = PlayerDataCache.getOrCreate(uuid);
        Set<String> autosellItemsIds = playerData.getAutosellMap().keySet();
        for (String id : autosellItemsIds) {
            if (!autoSellManager.isInAnyCategory(id)) {
                String msg = plugin.getMessageConfig().getConsoleNotFoundAutoSellItem();
                msg = msg
                        .replace("{player}", e.getPlayer().getName())
                        .replace("{id}", id);
                plugin.getLogger().warning(msg);
                playerData.removeAutosell(id);
            }
        }
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
