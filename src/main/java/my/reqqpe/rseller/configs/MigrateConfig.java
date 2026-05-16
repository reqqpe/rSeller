package my.reqqpe.rseller.configs;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MigrateConfig {

    public void migrate(Main plugin) {
        File dataFolder = plugin.getDataFolder();
        File oldConfig = new File(dataFolder, "config.yml");

        if (!oldConfig.exists()) {
            plugin.getLogger().info("config.yml not found");
            return;
        }

        FileConfiguration old = YamlConfiguration.loadConfiguration(oldConfig);

        int migrated = 0;

        if (old.contains("database")) {
            File dbFile = new File(dataFolder, "database.yml");
            FileConfiguration db = dbFile.exists()
                    ? YamlConfiguration.loadConfiguration(dbFile)
                    : new YamlConfiguration();

            ConfigurationSection dbSec = old.getConfigurationSection("database");
            if (dbSec != null) {
                copySection(dbSec, db, "");
                try {
                    db.save(dbFile);
                    plugin.getLogger().info("database section migrated");
                    migrated++;
                } catch (IOException e) {
                    plugin.getLogger().warning("database section error migrated: " + e.getMessage());
                }
            }
        }

        if (old.contains("levels")) {
            File lvlFile = new File(dataFolder, "levels.yml");
            FileConfiguration lvl = lvlFile.exists()
                    ? YamlConfiguration.loadConfiguration(lvlFile)
                    : new YamlConfiguration();

            ConfigurationSection lvlSec = old.getConfigurationSection("levels");
            if (lvlSec != null) {
                copySection(lvlSec, lvl, "");
                try {
                    lvl.save(lvlFile);
                    plugin.getLogger().info("levels section migrated");
                    migrated++;
                } catch (IOException e) {
                    plugin.getLogger().warning("levels section error migrated: " + e.getMessage());
                }
            }
        }

        if (old.contains("messages")) {
            File msgFile = new File(dataFolder, "messages.yml");
            FileConfiguration msg = msgFile.exists()
                    ? YamlConfiguration.loadConfiguration(msgFile)
                    : new YamlConfiguration();

            ConfigurationSection msgSec = old.getConfigurationSection("messages");
            if (msgSec != null) {
                copySection(msgSec, msg, "");
                try {
                    msg.save(msgFile);
                    plugin.getLogger().info("messages section migrated");
                    migrated++;
                } catch (IOException e) {
                    plugin.getLogger().warning("messages section error migrated: " + e.getMessage());
                }
            }
        }

        if (migrated == 0) {
            plugin.getLogger().info("Nothing to migrate - sections database/levels/messages not found in config.yml.");
        } else {
            plugin.getLogger().info("Migration completed, files updated: " + migrated);
            plugin.getLogger().warning("Run the /rseller reload command to apply the changes.");
            plugin.getLogger().info("The old config.yml has not been deleted you can delete the database/levels/messages sections manually.");
        }
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
}
