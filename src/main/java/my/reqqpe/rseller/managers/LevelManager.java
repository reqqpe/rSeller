package my.reqqpe.rseller.managers;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.database.DataBase;
import my.reqqpe.rseller.models.Level;
import my.reqqpe.rseller.models.ParsedAction;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.utils.CustomConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class LevelManager {

    private final RSeller plugin;
    private final DataBase dataBase;
    private final CustomConfig levelConfigFile;
    private FileConfiguration levelConfig;
    private final Long2ObjectSortedMap<Level> levels = new Long2ObjectLinkedOpenHashMap<>();

    public LevelManager(RSeller plugin, DataBase dataBase) {
        this.plugin = plugin;
        this.levelConfigFile = new CustomConfig(plugin, "levels.yml");
        this.dataBase = dataBase;
        this.levelConfig = levelConfigFile.getConfig();
        loadLevels();
    }

    public void loadLevels() {
        levels.clear();

        ConfigurationSection levelsSection = levelConfig.getConfigurationSection("levels");
        if (levelsSection == null) {
            plugin.getLogger().info("Ошибка загрузки уровней. Отсутствует секция levels");
            return;
        }

        for (String key : levelsSection.getKeys(false)) {
            int levelNum;
            try {
                levelNum = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Неверный номер уровня: " + key);
                continue;
            }

            double coinMultiplier = levelConfig.getDouble("levels." + key + ".coin-multiplier", 1.0);
            double pointMultiplier = levelConfig.getDouble("levels." + key + ".point-multiplier", 1.0);
            double requiredPoints = levelConfig.getDouble("levels." + key + ".required-points", 0.0);
            List<String> actions = levelConfig.getStringList("levels." + key + ".actions");
            List<ParsedAction> parsedActionList = ParsedAction.parse(actions);


            Level level = new Level(levelNum, coinMultiplier, pointMultiplier, requiredPoints, parsedActionList);
            levels.put(levelNum, level);
        }

        plugin.getLogger().info("Загружено уровней: " + levels.size());
    }

    public Level getLevel(long level) {
        return levels.get(level);
    }


    public Level getPlayerLevel(Player player) {
        UUID uuid = player.getUniqueId();

        PlayerData pd = dataBase.getPlayerData(uuid);
        long playerLevel = pd.getLevel();

        return getLevel(playerLevel);
    }

    public void tryLevelUp(Player player) {
        UUID uuid = player.getUniqueId();

        PlayerData pd = dataBase.getPlayerData(uuid);
        double points = pd.getPoints();
        long playerLevel = pd.getLevel();

        while (true) {
            long nextLevel = playerLevel + 1;

            Level level = getLevel(nextLevel);
            if (level == null) {
                break;
            }
            if (points < level.requiredPoints()) {
                break;
            }
            pd.addLevel(1);
            player.sendMessage("Ваш уровень теперь: " + pd.getLevel());
            playerLevel++;
            level.performActions(player);
        }
    }

    public void reload() {
        levelConfigFile.reloadConfig();
        levelConfig = levelConfigFile.getConfig();
        loadLevels();
    }

}
