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

    // Настройка конфига (аналог setup)
    public void setup() {
        // Проверяем, существует ли папка плагина, если нет — создаем
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Копируем файл из resources, если он не существует
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info(fileName + " был создан!");
        }

        // Загружаем конфиг
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    // Получение конфига
    public FileConfiguration getConfig() {
        if (config == null) {
            setup();
        }
        return config;
    }

    // Сохранение конфига
    public void saveConfig() {
        if (config == null || configFile == null) {
            plugin.getLogger().warning("Cannot save " + fileName + ": config is not initialized!");
            return;
        }
        try {
            config.save(configFile);
            plugin.getLogger().info(fileName + " has been saved!");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + fileName, e);
        }
    }

    // Перезагрузка конфига
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info(fileName + " has been reloaded!");
    }

    // Проверка существования файла
    public boolean exists() {
        return configFile.exists();
    }
}
