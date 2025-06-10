package my.reqqpe.rseller.configurations;

import my.reqqpe.rseller.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class CustomConfigs {
    private final Main plugin;
    private final String relativePath;
    private final String fileName;
    private File configFile;
    private FileConfiguration config;

    public CustomConfigs(Main plugin, String relativePath) {
        this.plugin = plugin;
        this.relativePath = relativePath.replace("\\", "/"); // на всякий случай
        this.fileName = new File(relativePath).getName();
        this.configFile = new File(plugin.getDataFolder(), this.relativePath);
    }

    public void setup() {
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        if (!configFile.exists()) {
            plugin.saveResource(relativePath, false);
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
            plugin.getLogger().warning("Cannot save " + relativePath + ": config is not initialized!");
            return;
        }
        try {
            config.save(configFile);
            plugin.getLogger().info(fileName + " был сохранён!");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + relativePath, e);
        }
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info(fileName + " был перезагружен!");
    }
}
