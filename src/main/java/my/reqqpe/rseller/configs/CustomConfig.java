package my.reqqpe.rseller.configs;

import lombok.Getter;
import my.reqqpe.rseller.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public abstract class CustomConfig {

    protected final Main plugin;
    private final String relativePath;
    private final String fileName;
    private final File configFile;

    @Getter
    protected FileConfiguration config;

    public CustomConfig(Main plugin, String relativePath) {
        this.plugin = plugin;
        this.relativePath = relativePath.replace("\\", "/");
        this.fileName = new File(relativePath).getName();
        this.configFile = new File(plugin.getDataFolder(), this.relativePath);
    }

    public void setup() {
        if (!configFile.getParentFile().exists()) {
            configFile.getParentFile().mkdirs();
        }

        if (!configFile.exists()) {
            plugin.saveResource(relativePath, false);
            plugin.getLogger().info(fileName + " created!");
        }

        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        load();
    }


    public void saveConfig() {
        if (config == null) {
            plugin.getLogger().warning("Cannot save " + relativePath + ": config is not initialized!");
            return;
        }

        try {
            config.save(configFile);
            plugin.getLogger().info(fileName + " saved!");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + relativePath, e);
        }
    }

    protected String getString(String path, String def) {
        if (!config.contains(path)) {
            warn(path, def);
            return def;
        }
        return config.getString(path, def);
    }

    protected int getInt(String path, int def) {
        if (!config.contains(path)) {
            warn(path, def);
            return def;
        }
        return config.getInt(path);
    }

    protected long getLong(String path, long def) {
        if (!config.contains(path)) {
            warn(path, def);
            return def;
        }
        return config.getLong(path);
    }

    protected double getDouble(String path, double def) {
        if (!config.contains(path)) {
            warn(path, def);
            return def;
        }
        return config.getDouble(path);
    }

    protected boolean getBoolean(String path, boolean def) {
        if (!config.contains(path)) {
            warn(path, def);
            return def;
        }
        return config.getBoolean(path);
    }

    protected List<String> getStringList(String path) {
        if (!config.contains(path)) {
            warn(path, "[]");
        }
        return config.getStringList(path);
    }

    protected List<Integer> getIntegerList(String path) {
        if (!config.contains(path)) {
            warn(path, "[]");
        }
        return config.getIntegerList(path);
    }

    protected List<Double> getDoubleList(String path) {
        if (!config.contains(path)) {
            warn(path, "[]");
        }
        return config.getDoubleList(path);
    }

    protected List<Long> getLongList(String path) {
        if (!config.contains(path)) {
            warn(path, "[]");
        }
        return config.getLongList(path);
    }

    protected List<Boolean> getBooleanList(String path) {
        if (!config.contains(path)) {
            warn(path, "[]");
        }
        return config.getBooleanList(path);
    }


    private void warn(String path, Object def) {
        plugin.getLogger().warning("Missing config path: '" + path + "', using default: " + def);
    }

    protected abstract void load();
}