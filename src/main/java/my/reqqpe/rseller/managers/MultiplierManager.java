package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.models.Level;
import my.reqqpe.rseller.models.Multiplier;
import my.reqqpe.rseller.utils.LoggerUtil;
import my.reqqpe.rseller.utils.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



// Сделать бусты
public class MultiplierManager {

    private final RSeller plugin;
    private final LevelManager levelManager;

    Map<String, Multiplier> multiplierMap = new HashMap<>();


    public MultiplierManager(RSeller plugin, LevelManager levelManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
    }


    public void loadMultipliers() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("multipliers");
        if (section == null) {
            LoggerUtil.warn(MessageUtil.getString("log.multipliers-no-section"));
            return;
        }
        for (String id : section.getKeys(true)) {
            if (id == null) {
                LoggerUtil.warn(MessageUtil.getString("log.multiplier-null-id"));
                continue;
            }
            if (multiplierMap.containsKey(id)) {
                LoggerUtil.warn(
                        MessageUtil.getString("log.multiplier-duplicate")
                                .replace("{id}", id)
                );
                continue;
            }
            ConfigurationSection multiplierSection = section.getConfigurationSection(id);
            double multiplierMoney = multiplierSection.getDouble("multiplier_money");
            double multiplierPoints = multiplierSection.getDouble("multiplier_points");

            Multiplier multiplier = new Multiplier(
                    multiplierMoney,
                    multiplierPoints
            );
            multiplierMap.put(id, multiplier);
        }
        LoggerUtil.info(
                MessageUtil.getString("log.multipliers-loaded")
                        .replace("{amount}", String.valueOf(multiplierMap.size()))
        );
    }


    public Set<Multiplier> getActiveMultipliersForPlayer(Player player) {
        if (player == null || !player.isOnline()) {
             return null;
        }
        Set<Multiplier> playerMultipliers = new HashSet<>();
        for (String permission : multiplierMap.keySet()) {
            if (player.hasPermission(permission)) {
                playerMultipliers.add(multiplierMap.get(permission));
            }
        }
        return playerMultipliers;
    }


    public Multiplier getMultiplierForPlayer(Player player) {
        Set<Multiplier> multipliersPlayer = getActiveMultipliersForPlayer(player);
        double multiplierMoney = 1;
        double multiplierPoints = 1;
        for (Multiplier multiplier : multipliersPlayer) {
            if (multiplier == null) {
                continue;
            }
            multiplierMoney += multiplier.money() - 1;
            multiplierPoints = multiplier.points() - 1;
        }

        Level level = levelManager.getPlayerLevel(player);
        if (level != null) {
            multiplierMoney += level.coinMultiplier() - 1;
            multiplierPoints += level.pointMultiplier() - 1;
        }

        double finalMultiplierMoney = Math.max(1.0, multiplierMoney);
        double finalMultiplierPoints = Math.max(1.0, multiplierPoints);

        return new Multiplier(
                finalMultiplierMoney,
                finalMultiplierPoints
        );
    }
}
