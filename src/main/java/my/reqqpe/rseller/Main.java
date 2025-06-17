package my.reqqpe.rseller;

import lombok.Getter;
import my.reqqpe.rseller.commands.SellCommand;
import my.reqqpe.rseller.commands.SellAdminCommand;
import my.reqqpe.rseller.commands.TabCompliteAdmin;
import my.reqqpe.rseller.configurations.CustomConfigs;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.DatabaseListener;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.managers.ItemManager;
import my.reqqpe.rseller.managers.LevelManager;
import my.reqqpe.rseller.managers.SellManager;
import my.reqqpe.rseller.menu.AutoSellMenu;
import my.reqqpe.rseller.menu.SellMenu;
import my.reqqpe.rseller.tasks.AutoSellTask;
import my.reqqpe.rseller.utils.Bstatsmetrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Главный класс плагина rSeller
 */
public final class Main extends JavaPlugin {

    private AutoSellTask autoSellTask;
    private SellManager sellManager;
    private Database database;

    @Getter
    private SellMenu sellMenu;
    @Getter
    private AutoSellMenu autoSellMenu;
    @Getter
    private AutoSellManager autoSellManager;
    @Getter
    private CustomConfigs itemsConfig;
    @Getter
    private CustomConfigs mainGUI;
    @Getter
    private LevelManager levelManager;
    @Getter
    private CustomConfigs autoSellGUI;
    @Getter
    private ItemManager itemManager;

    @Override
    public void onEnable() {
        EconomySetup.setupEconomy(this);

        if (EconomySetup.getEconomy() == null) {
            getLogger().severe("Vault не найден или не настроен! Плагин отключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        initializeConfigsAndDatabase();

        itemManager = new ItemManager(this);
        levelManager = new LevelManager(this, database);
        sellManager = new SellManager(this, database);
        autoSellManager = new AutoSellManager(this);

        convertSimpleItemsToStandardFormat();

        sellMenu = new SellMenu(this, sellManager, database);
        autoSellMenu = new AutoSellMenu(this, database);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(sellMenu, this);
        pm.registerEvents(autoSellMenu, this);
        pm.registerEvents(new DatabaseListener(database), this);

        getCommand("sell").setExecutor(new SellCommand(sellMenu));
        getCommand("rseller").setExecutor(new SellAdminCommand(this, database));
        getCommand("rseller").setTabCompleter(new TabCompliteAdmin(this));

        itemManager.load();
        autoSellManager.loadConfig();

        autoSellTask = new AutoSellTask(this, sellManager);
        if (getConfig().getBoolean("autosell.enable")) {
            autoSellTask.autoSellTask();
        }

        if (getConfig().getBoolean("metrics", true)) {
            int pluginId = 25999;
            Bstatsmetrics metrics = new Bstatsmetrics(this, pluginId);
            getLogger().info("bStats успешно инициализирован!");
        }
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.saveAll();
        }
    }


    private void initializeConfigsAndDatabase() {
        saveDefaultConfig();

        if (!getConfig().contains("numbers_format")) {
            File oldConfigFile = new File(getDataFolder(), "config.yml");
            File renamedConfigFile = new File(getDataFolder(), "old_config_" + System.currentTimeMillis() + ".yml");

            if (oldConfigFile.exists()) {
                try {
                    Files.copy(oldConfigFile.toPath(), renamedConfigFile.toPath());
                    getLogger().info("config.yml отсутствует раздел numbers_format. Создан бэкап: " + renamedConfigFile.getName());
                } catch (IOException e) {
                    getLogger().severe("Не удалось создать бэкап config.yml: " + e.getMessage());
                }
            }
            saveDefaultConfig();
            reloadConfig();
        }

        File itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            saveResource("items.yml", false);
            getLogger().info("items.yml скопирован из ресурсов, так как файл не существовал.");
        }

        this.itemsConfig = new CustomConfigs(this, "items.yml");
        this.itemsConfig.setup();
        getLogger().info("items.yml успешно загружен и готов к использованию.");

        mainGUI = new CustomConfigs(this, "GUI/mainGUI.yml");
        mainGUI.setup();
        getLogger().info("mainGUI.yml успешно загружен");

        autoSellGUI = new CustomConfigs(this, "GUI/autoSellGUI.yml");
        autoSellGUI.setup();
        getLogger().info("autoSellGUI.yml успешно загружен");

        database = new Database(this);
        for (Player pl : Bukkit.getOnlinePlayers()) database.loadPlayerData(pl.getUniqueId());
    }


    public void convertSimpleItemsToStandardFormat() {
        File itemsFile = new File(getDataFolder(), "items.yml");
        File backupFile = new File(getDataFolder(), "items_backup_pre_conversion_" + System.currentTimeMillis() + ".yml");

        ConfigurationSection itemsSection = itemsConfig.getConfig().getConfigurationSection("items");

        if (itemsSection == null || itemsSection.getKeys(false).isEmpty()) {
            getLogger().info("Секция 'items' в items.yml не найдена или пуста. Конвертация не требуется.");
            return;
        }

        boolean needsConversion = false;
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection item = itemsSection.getConfigurationSection(key);
            if (item == null || !item.contains("itemstack")) {
                needsConversion = true;
                getLogger().info("Предмет '" + key + "' требует конвертации.");
                break;
            }
        }

        if (!needsConversion) {
            getLogger().info("Конфигурация items.yml уже в стандартизированном формате. Конвертация не требуется.");
            return;
        }

        try {
            Files.copy(itemsFile.toPath(), backupFile.toPath());
            getLogger().info("Бэкап items.yml создан: " + backupFile.getName());
        } catch (IOException e) {
            getLogger().severe("Ошибка при создании бэкапа items.yml: " + e.getMessage());
        }

        for (String itemId : new ArrayList<>(itemsSection.getKeys(false))) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null) {
                getLogger().warning("Секция для предмета '" + itemId + "' не является ConfigurationSection. Пропускаем.");
                continue;
            }

            Material material = Material.matchMaterial(itemId);
            if (material == null) {
                getLogger().warning("Неверный материал для предмета '" + itemId + "'. Пропускаем.");
                continue;
            }

            double price = itemSection.getDouble("price", 0.0);
            double points = itemSection.getDouble("points", 0.0);

            ItemStack itemStack = new ItemStack(material, 1);

            itemsConfig.getConfig().set("items." + itemId, null);

            String newId = itemId.toLowerCase();
            if (itemManager.createItem(newId, points, price, itemStack)) {
                getLogger().info("Сконвертирован предмет: " + newId);
            } else {
                getLogger().warning("Не удалось конвертировать предмет: " + newId);
            }
        }

        try {
            itemsConfig.saveConfig();
            getLogger().info("items.yml успешно сохранен после конвертации.");
        } catch (Exception e) {
            getLogger().severe("Ошибка при сохранении items.yml после конвертации: " + e.getMessage());
        }

        itemsConfig.reloadConfig();
        getLogger().info("Конфигурация items.yml перезагружена после конвертации.");

        ConfigurationSection newItemsSection = itemsConfig.getConfig().getConfigurationSection("items");
        if (newItemsSection != null) {
            getLogger().info("Содержимое items.yml после конвертации:");
            for (String key : newItemsSection.getKeys(false)) {
                getLogger().info("Предмет: " + key + ", itemstack: " + (newItemsSection.getConfigurationSection(key + ".itemstack") != null));
            }
        } else {
            getLogger().warning("Секция 'items' в items.yml пуста после конвертации!");
        }
    }
}