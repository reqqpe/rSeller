package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.model.Booster;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoosterManager {
    private final Main plugin;
    private final LevelManager levelManager;

    private final Map<String, Booster> boosters = new HashMap<>();



    public BoosterManager(Main plugin) {
        this.plugin = plugin;
        this.levelManager = plugin.getLevelManager();
        load();
    }


    public void load() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("boosters");

        if (section == null) return;

        boosters.clear();

        for (String key : section.getKeys(false)) {
            ConfigurationSection boosterSection = section.getConfigurationSection(key);
            if (boosterSection == null) continue;

            double coin = boosterSection.getDouble("coin-multiplier", 1.0);
            double point = boosterSection.getDouble("point-multiplier", 1.0);

            boosters.put(key.toLowerCase(), new Booster(coin, point));
        }
    }

    public List<Booster> getActiveBoosters(Player player) {
        List<Booster> active = new ArrayList<>();

        for (Map.Entry<String, Booster> entry : boosters.entrySet()) {
            String boosterKey = entry.getKey();
            String permission = "rseller.booster." + boosterKey;

            if (player.hasPermission(permission)) {
                active.add(entry.getValue());
            }
        }
        return active;
    }


    public Booster getBoosterByPlayer(Player player) {
        List<Booster> active = getActiveBoosters(player);
        double perm_booster_points = 1.0;
        double perm_booster_coins = 1.0;
        for (Booster booster : active) {
            if (booster == null) continue;
            perm_booster_coins += booster.getCoinMultiplier() - 1;
            perm_booster_points += booster.getPointMultiplier() - 1;
        }

        double level_booster_coins = levelManager.getCoinMultiplier(player);
        double level_booster_points = levelManager.getPointMultiplier(player);

        double final_booster_coins = Math.max(1.0, perm_booster_coins + level_booster_coins - 1);
        double final_booster_points = Math.max(1.0, perm_booster_points + level_booster_points - 1);

        return new Booster(final_booster_coins, final_booster_points);
    }
}
