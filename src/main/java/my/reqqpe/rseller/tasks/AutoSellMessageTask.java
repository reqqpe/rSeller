package my.reqqpe.rseller.tasks;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.cache.SellDataCache;
import my.reqqpe.rseller.configs.impl.MainConfig;
import my.reqqpe.rseller.configs.impl.MessageConfig;
import my.reqqpe.rseller.managers.NumberFormatManager;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.models.SellData;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;


public class AutoSellMessageTask {

    private final Main plugin;
    private final MainConfig config;
    private final MessageConfig msg;
    private final NumberFormatManager numberFormatManager;

    public AutoSellMessageTask(Main plugin, NumberFormatManager numberFormatManager) {
        this.plugin = plugin;
        this.config = plugin.getMainConfig();
        this.msg = plugin.getMessageConfig();
        this.numberFormatManager = numberFormatManager;

    }



    public void startTask() {

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String message = msg.getAutoSellTask();

            List<UUID> playerUUIDs = SellDataCache.getCache().keySet().stream().toList();
            for (UUID uuid : playerUUIDs) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    SellDataCache.remove(uuid);
                    continue;
                }
                PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());
                if (!playerData.isAutosellMessage()) continue;

                SellData sellData = SellDataCache.getOrCreate(uuid);
                if (sellData.getCount() <= 0 && sellData.getPoints() <= 0 && sellData.getMoney() <= 0) {
                    SellDataCache.remove(uuid);
                    continue;
                }


                MainConfig.SoundsSection sounds = plugin.getMainConfig().getSounds();
                plugin.playSound(player, sounds.getAutosell(), sounds.getAutosellVolume(), sounds.getAutosellPitch());

                String moneyFormat = numberFormatManager.format("messages.coins", sellData.getMoney());
                String pointsFormat = numberFormatManager.format("messages.points", sellData.getPoints());

                String finalMessage = Colorizer.color(message
                        .replace("{coins}", moneyFormat)
                        .replace("{points}", pointsFormat)
                        .replace("{amount}", String.valueOf(sellData.getCount())));

                player.sendMessage(finalMessage);
                SellDataCache.remove(uuid);
            }
        }, 0L ,config.getAutosell().getDelayMessage() * 20L);
    }
}
