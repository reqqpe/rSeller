package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.models.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.TreeMap;

public class LevelManager {

    private final Main plugin;
    private final Database database;
    private final TreeMap<Integer, Level> levels = new TreeMap<>();

    public LevelManager(Main plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        loadLevels();
    }

    private void loadLevels() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("levels");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            int level;
            try {
                level = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }

            double coinMultiplier = section.getDouble(key + ".coin-multiplier", 1.0);
            double pointMultiplier = section.getDouble(key + ".point-multiplier", 1.0);
            double requiredPoints = section.getDouble(key + ".required-points", 0);


            levels.put(level, new Level(level, coinMultiplier, pointMultiplier, requiredPoints));
        }
    }

    public Level getLevelInfo(Player player) {
        PlayerData data = database.getPlayerData(player.getUniqueId());
        double points = data.getPoints();

        Level current = levels.firstEntry().getValue();
        for (Map.Entry<Integer, Level> entry : levels.entrySet()) {
            if (points >= entry.getValue().requiredPoints()) {
                current = entry.getValue();
            } else {
                break;
            }
        }

        return current;
    }

    public double getCoinMultiplier(Player player) {
        return getLevelInfo(player).coinMultiplier();
    }

    public double getPointMultiplier(Player player) {
        return getLevelInfo(player).pointMultiplier();
    }

    public int getLevel(Player player) {
        return getLevelInfo(player).level();
    }

    public double getPointsForNextLevel(int currentLevel) {
        for (Map.Entry<Integer, Level> entry : levels.entrySet()) {
            if (entry.getKey() > currentLevel) {
                return entry.getValue().requiredPoints();
            }
        }
        return -1;
    }

    public double getPointsForNextLevel(Player player) {
        int currentLevel = getLevel(player);
        return getPointsForNextLevel(currentLevel);
    }


    public void reloadLevels() {
        levels.clear();
        loadLevels();
    }
}