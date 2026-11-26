package my.reqqpe.rseller.configs;

import my.reqqpe.rseller.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MigrateConfig {

    public void migrate(Main plugin) {
        migrateOldConfig(plugin);
        migrateItemsFormat(plugin);
        migrateBoostersFormat(plugin);
    }

    private void migrateOldConfig(Main plugin) {
        File dataFolder = plugin.getDataFolder();
        File oldConfig = new File(dataFolder, "config.yml");

        if (!oldConfig.exists()) return;

        FileConfiguration old = YamlConfiguration.loadConfiguration(oldConfig);
        int migrated = 0;

        migrated += migrateSection(plugin, old, "database", new File(dataFolder, "database.yml"));
        migrated += migrateSection(plugin, old, "levels", new File(dataFolder, "levels.yml"));
        migrated += migrateSection(plugin, old, "messages", new File(dataFolder, "messages.yml"));

        if (migrated == 0) {
            plugin.getLogger().info("[Migrate] config.yml found but nothing to migrate.");
        } else {
            plugin.getLogger().info("[Migrate] Migrated " + migrated + " section(s) from config.yml.");
            plugin.getLogger().warning("[Migrate] Run /rseller reload to apply changes.");
        }
    }

    private int migrateSection(Main plugin, FileConfiguration source, String section, File targetFile) {
        if (!source.contains(section)) return 0;

        ConfigurationSection sec = source.getConfigurationSection(section);
        if (sec == null) return 0;

        FileConfiguration target = targetFile.exists()
                ? YamlConfiguration.loadConfiguration(targetFile)
                : new YamlConfiguration();

        copySection(sec, target, "");

        try {
            target.save(targetFile);
            plugin.getLogger().info("[Migrate] Section '" + section + "' -> " + targetFile.getName());
            return 1;
        } catch (IOException e) {
            plugin.getLogger().warning("[Migrate] Failed to save " + targetFile.getName() + ": " + e.getMessage());
            return 0;
        }
    }

    private void migrateItemsFormat(Main plugin) {
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);

        boolean changed = false;

        changed |= migrateCustomItems(config);
        changed |= migrateItemsWithoutMaterial(config);
        changed |= flattenItemData(config);

        if (changed) {
            config.set("custom-items", null);
            try {
                config.save(itemsFile);
                plugin.getLogger().info("[Migrate] items.yml migrated to new format.");
            } catch (IOException e) {
                plugin.getLogger().warning("[Migrate] Failed to save items.yml: " + e.getMessage());
            }
        }
    }

    private boolean migrateCustomItems(FileConfiguration config) {
        ConfigurationSection customSec = config.getConfigurationSection("custom-items");
        if (customSec == null) return false;

        boolean changed = false;

        for (String id : customSec.getKeys(false)) {
            ConfigurationSection entry = customSec.getConfigurationSection(id);
            if (entry == null) continue;

            String base = "items." + id + ".";

            String material = entry.getString("material");
            if (material != null) config.set(base + "material", material);

            ConfigurationSection dataSec = entry.getConfigurationSection("item-data");
            if (dataSec != null) {
                for (String key : dataSec.getKeys(false)) {
                    config.set(base + key, dataSec.get(key));
                }
            }

            if (!config.contains(base + "price")) {
                config.set(base + "price", 0.0);
            }
            if (!config.contains(base + "points")) {
                config.set(base + "points", 0.0);
            }

            changed = true;
        }

        return changed;
    }

    private boolean migrateItemsWithoutMaterial(FileConfiguration config) {
        ConfigurationSection itemsSec = config.getConfigurationSection("items");
        if (itemsSec == null) return false;

        boolean changed = false;

        for (String id : itemsSec.getKeys(false)) {
            String materialPath = "items." + id + ".material";
            if (!config.contains(materialPath)) {
                config.set(materialPath, id);
                changed = true;
            }
        }

        return changed;
    }

    private boolean flattenItemData(FileConfiguration config) {
        ConfigurationSection itemsSec = config.getConfigurationSection("items");
        if (itemsSec == null) return false;

        boolean changed = false;

        for (String id : itemsSec.getKeys(false)) {
            String dataPath = "items." + id + ".item-data";
            ConfigurationSection dataSec = config.getConfigurationSection(dataPath);
            if (dataSec == null) continue;

            String base = "items." + id + ".";
            for (String key : dataSec.getKeys(false)) {
                config.set(base + key, dataSec.get(key));
            }
            config.set(dataPath, null);
            changed = true;
        }

        return changed;
    }

    private void copySection(ConfigurationSection source, FileConfiguration target, String prefix) {
        for (String key : source.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (source.isConfigurationSection(key)) {
                copySection(source.getConfigurationSection(key), target, fullKey);
            } else {
                target.set(fullKey, source.get(key));
            }
        }
    }

    private void migrateBoostersFormat(Main plugin) {
        File dataFolder = plugin.getDataFolder();
        File configFile = new File(dataFolder, "config.yml");
        if (!configFile.exists()) return;

        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection oldBoosters = oldConfig.getConfigurationSection("boosters");
        if (oldBoosters == null) return;

        File boostersFile = new File(dataFolder, "boosters.yml");
        if (boostersFile.exists()) {
            FileConfiguration existing = YamlConfiguration.loadConfiguration(boostersFile);
            if (existing.contains("boosters.permission-boosters")) {
                return;
            }
        }

        FileConfiguration boostersConfig = boostersFile.exists()
                ? YamlConfiguration.loadConfiguration(boostersFile)
                : new YamlConfiguration();

        int count = 0;
        for (String key : oldBoosters.getKeys(false)) {
            ConfigurationSection entry = oldBoosters.getConfigurationSection(key);
            if (entry == null) continue;

            String base = "boosters.permission-boosters." + key + ".";
            boostersConfig.set(base + "permission", "rseller.booster." + key);
            boostersConfig.set(base + "coins", entry.getDouble("coin-multiplier", 1.0));
            boostersConfig.set(base + "points", entry.getDouble("point-multiplier", 1.0));
            boostersConfig.set(base + "priority", 1);
            boostersConfig.set(base + "source", "all");
            count++;
        }

        try {
            boostersConfig.save(boostersFile);
            plugin.getLogger().info("[Migrate] Migrated " + count + " booster(s) from config.yml to boosters.yml.");
        } catch (IOException e) {
            plugin.getLogger().warning("[Migrate] Failed to save boosters.yml: " + e.getMessage());
        }
    }
}
