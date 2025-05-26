package my.reqqpe.rseller;

import lombok.Getter;
import my.reqqpe.rseller.commands.AutoSellCommand;
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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

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

        // managers
        levelManager = new LevelManager(this, database);
        sellManager = new SellManager(this, database);
        autoSellManager = new AutoSellManager(this);

        // menus
        sellMenu = new SellMenu(this, sellManager, database);
        autoSellMenu = new AutoSellMenu(this, database);

        // events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(sellMenu, this);
        pm.registerEvents(autoSellMenu, this);
        pm.registerEvents(new DatabaseListener(database), this);

        //commands
        getCommand("sell").setExecutor(new SellCommand(sellMenu));
        getCommand("rseller").setExecutor(new SellAdminCommand(this, database));
        getCommand("autosell").setExecutor(new AutoSellCommand(this, database, autoSellMenu));

        // tab complite
        getCommand("rseller").setTabCompleter(new TabCompliteAdmin());

        // tasks
        autoSellTask = new AutoSellTask(this, sellManager);
        if (getConfig().getBoolean("autosell.enable")) {
            autoSellTask.autoSellTask();
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

        mainGUI = new CustomConfigs(this, "mainGUI.yml");
        mainGUI.setup();
        getLogger().info("mainGUI.yml успешно загружен");

        autoSellGUI = new CustomConfigs(this, "autoSellGUI.yml");
        autoSellGUI.setup();
        getLogger().info("autoSellGUI.yml успешно загружен");

        database = new Database(this);
        for (Player pl : Bukkit.getOnlinePlayers()) database.loadPlayerData(pl.getUniqueId());
    }
}
