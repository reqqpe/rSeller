package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.configs.impl.LevelConfig;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.models.Level;
import org.bukkit.entity.Player;

public class LevelManager {

    private final Database database;
    private final LevelConfig levelConfig;

    public LevelManager(Database database, LevelConfig levelConfig) {
        this.database = database;
        this.levelConfig = levelConfig;
    }

    public Level getLevelInfo(Player player) {

        double points = database
                .getPlayerData(player.getUniqueId())
                .getPoints();

        Level current = null;

        for (Level level : levelConfig.getLevels().values()) {

            if (current == null) {
                current = level;
                continue;
            }

            if (points >= level.requiredPoints()) {
                current = level;
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

        for (Level level : levelConfig.getLevels().values()) {
            if (level.level() > currentLevel) {
                return level.requiredPoints();
            }
        }

        return -1;
    }

    public double getPointsForNextLevel(Player player) {
        return getPointsForNextLevel(getLevel(player));
    }
}