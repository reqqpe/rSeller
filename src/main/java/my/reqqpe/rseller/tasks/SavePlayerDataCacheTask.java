package my.reqqpe.rseller.tasks;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.configs.impl.DataBaseConfig;
import my.reqqpe.rseller.database.repositories.PlayerRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class SavePlayerDataCacheTask {

    private final Main plugin;
    private final PlayerRepository playerRepository;
    private final DataBaseConfig dataBaseConfig;


    public SavePlayerDataCacheTask(Main plugin, PlayerRepository playerRepository, DataBaseConfig dataBaseConfig) {
        this.plugin = plugin;
        this.playerRepository = playerRepository;

        this.dataBaseConfig = dataBaseConfig;

        runTask();
    }


    public void runTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            List<UUID> playerUUIDs = Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getUniqueId)
                    .toList();

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                for (UUID uuid : playerUUIDs) {
                    playerRepository.savePlayerData(uuid);
                }
            });
        }, 0L, dataBaseConfig.getSaveCacheTimer() * 20L);
    }
}
