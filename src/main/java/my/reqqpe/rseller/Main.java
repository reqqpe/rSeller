package my.reqqpe.rseller;

import lombok.Getter;
import my.reqqpe.rseller.commands.AutoSellCommand;
import my.reqqpe.rseller.commands.SellCommand;
import my.reqqpe.rseller.commands.SellAdminCommand;
import my.reqqpe.rseller.commands.TabCompliteAdmin;
import my.reqqpe.rseller.configurations.CustomConfigs;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.DatabaseListener;
import my.reqqpe.rseller.managers.ItemManager;
import my.reqqpe.rseller.managers.LevelManager;
import my.reqqpe.rseller.managers.SellManager;
import my.reqqpe.rseller.menu.SellMenu;
import my.reqqpe.rseller.tasks.AutoSellTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private AutoSellTask autoSellTask;
    private SellManager sellManager;
    private SellMenu sellMenu;
    private Database database;

    @Getter
    private CustomConfigs itemsConfig;
    @Getter
    private CustomConfigs mainGUI;
    @Getter
    private LevelManager levelManager;
    @Getter
    private ItemManager itemManager;
    @Getter CustomConfigs autoSellGUI;

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
        itemManager = new ItemManager(this);

        // menus
        sellMenu = new SellMenu(this, sellManager, database);

        // events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(sellMenu, this);
        pm.registerEvents(new DatabaseListener(database), this);

        //commands
        getCommand("sell").setExecutor(new SellCommand(sellMenu));
        getCommand("rseller").setExecutor(new SellAdminCommand(this, database));
        getCommand("autosell").setExecutor(new AutoSellCommand(this, database));

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

        itemsConfig = new CustomConfigs(this, "items.yml");
        itemsConfig.setup();
        getLogger().info("items.yml успешно загружен");

        mainGUI = new CustomConfigs(this, "mainGUI.yml");
        mainGUI.setup();
        getLogger().info("mainGUI.yml успешно загружен");

        //autoSellGUI = new CustomConfigs(this, "autoSellGUI.yml");
        //mainGUI.setup();
        //getLogger().info("autoSellGUI.yml успешно загружен");

        database = new Database(this);
        for (Player pl : Bukkit.getOnlinePlayers()) database.loadPlayerData(pl.getUniqueId());
    }
}
