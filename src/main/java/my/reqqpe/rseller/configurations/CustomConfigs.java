package my.reqqpe.rseller.configurations;

import my.reqqpe.rseller.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class CustomConfigs {
    private final Main plugin;
    private File configFile;
    private FileConfiguration config;
    private final String fileName;

    public CustomConfigs(Main plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.configFile = new File(plugin.getDataFolder(), fileName);
    }

    public void setup() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info(fileName + " был создан!");
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            setup();
        }
        return config;
    }

    public void saveConfig() {
        if (config == null || configFile == null) {
            plugin.getLogger().warning("Cannot save " + fileName + ": config is not initialized!");
            return;
        }
        try {
            config.save(configFile);
            plugin.getLogger().info(fileName + " был сохранён!");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + fileName, e);
        }
    }
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info(fileName + " был перезагружен!");
    }
}
