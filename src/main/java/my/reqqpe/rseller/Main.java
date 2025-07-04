package my.reqqpe.rseller;

import lombok.Getter;
import my.reqqpe.rseller.commands.SellCommand;
import my.reqqpe.rseller.commands.SellAdminCommand;
import my.reqqpe.rseller.commands.TabCompliteAdmin;
import my.reqqpe.rseller.configurations.CustomConfigs;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.DatabaseListener;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.managers.LevelManager;
import my.reqqpe.rseller.managers.SellManager;
import my.reqqpe.rseller.menu.AutoSellMenu;
import my.reqqpe.rseller.menu.SellMenu;
import my.reqqpe.rseller.tasks.AutoSellTask;
import my.reqqpe.rseller.utils.Metrics;
import my.reqqpe.rseller.utils.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Main extends JavaPlugin {

    private AutoSellTask autoSellTask;
    private Database database;

    @Getter
    private SellManager sellManager;
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

    @Override
    public void onEnable() {

        EconomySetup.setupEconomy(this);

        if (EconomySetup.getEconomy() == null) {
            getLogger().severe("Vault не найден или не настроен! Плагин отключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // configs

        loadConfigs();


        if (getServer().getPluginManager().getPlugin("PlaceHolderAPI") != null) {
            new PlaceholderAPI(this, database).register();
            getLogger().info("PlaceHolderAPI найден");

        }

        // managers
        levelManager = new LevelManager(this, database);
        sellManager = new SellManager(this, database);
        autoSellManager = new AutoSellManager(this);

        // menus
        sellMenu = new SellMenu(this);
        autoSellMenu = new AutoSellMenu(this, database);

        // events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(sellMenu, this);
        pm.registerEvents(autoSellMenu, this);
        pm.registerEvents(new DatabaseListener(database), this);

        //commands
        getCommand("sell").setExecutor(new SellCommand(sellMenu));
        getCommand("rseller").setExecutor(new SellAdminCommand(this, database));

        // tab complite
        getCommand("rseller").setTabCompleter(new TabCompliteAdmin());

        // tasks
        autoSellTask = new AutoSellTask(this, sellManager);
        if (getConfig().getBoolean("autosell.enable")) {
            autoSellTask.autoSellTask();
        }

        if (getConfig().getBoolean("metrics", true)) {
            int pluginId = 25999;
            Metrics metrics = new Metrics(this, pluginId);
            getLogger().info("bStats успешно инициализирован!");
        }
    }

    @Override
    public void onDisable() {
        database.saveAll();
    }
    private void loadConfigs() {
        saveDefaultConfig();

        if (!getConfig().contains("numbers_format")) {
            File oldConfigFile = new File(getDataFolder(), "config.yml");
            File renamedConfigFile = new File(getDataFolder(), "old_config.yml");

            if (oldConfigFile.exists()) {
                if (renamedConfigFile.exists()) {
                    renamedConfigFile = new File(getDataFolder(), "old_config_" + System.currentTimeMillis() + ".yml");
                }
                if (oldConfigFile.renameTo(renamedConfigFile)) {
                    getLogger().info("config.yml отсутствует раздел numbers_format. Переименован в " + renamedConfigFile.getName());
                } else {
                    getLogger().severe("Не удалось переименовать config.yml в " + renamedConfigFile.getName());
                }
            }
            saveDefaultConfig();
            reloadConfig();
        }

        itemsConfig = new CustomConfigs(this, "items.yml");
        itemsConfig.setup();
        getLogger().info("items.yml успешно загружен");

        mainGUI = new CustomConfigs(this, "GUI/mainGUI.yml");
        mainGUI.setup();
        getLogger().info("mainGUI.yml успешно загружен");

        autoSellGUI = new CustomConfigs(this, "GUI/autoSellGUI.yml");
        autoSellGUI.setup();
        getLogger().info("autoSellGUI.yml успешно загружен");

        database = new Database(this);
        for (Player pl : Bukkit.getOnlinePlayers()) database.loadPlayerData(pl.getUniqueId());
    }

    
}
