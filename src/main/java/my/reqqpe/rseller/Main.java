package my.reqqpe.rseller;

import my.reqqpe.rseller.commands.SellCommand;
import my.reqqpe.rseller.commands.SellAdminCommand;
import my.reqqpe.rseller.configurations.CustomConfigs;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.DatabaseListener;
import my.reqqpe.rseller.managers.LevelManager;
import my.reqqpe.rseller.sell.SellManager;
import my.reqqpe.rseller.menu.SellMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private CustomConfigs itemsConfig;
    private CustomConfigs mainGUI;
    private SellMenu sellMenu;
    private Database database;
    private LevelManager levelManager;
    private SellManager sellManager;

    @Override
    public void onEnable() {
        EconomySetup.setupEconomy(this);

        if (EconomySetup.getEconomy() == null) {
            getLogger().severe("Vault не найден или не настроен! Плагин отключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // configs
        saveDefaultConfig();

        itemsConfig = new CustomConfigs(this, "items.yml");
        itemsConfig.setup();
        getLogger().info("items.yml успешно загружен");

        mainGUI = new CustomConfigs(this, "mainGUI.yml");
        mainGUI.setup();
        getLogger().info("mainGUI.yml успешно загружен");

        database = new Database(this);
        for (Player pl : Bukkit.getOnlinePlayers()) database.loadPlayerData(pl.getUniqueId());


        // managers
        levelManager = new LevelManager(this, database);
        sellManager = new SellManager(this, database);

        // events

        sellMenu = new SellMenu(this, sellManager, database);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(sellMenu, this);
        pm.registerEvents(new DatabaseListener(database), this);

        //commands
        getCommand("sell").setExecutor(new SellCommand(sellMenu));
        getCommand("rseller").setExecutor(new SellAdminCommand(this, database));


    }

    @Override
    public void onDisable() {
        database.saveAll();
    }

    public CustomConfigs getItemsConfig() {
        return itemsConfig;
    }

    public CustomConfigs getMainGUI() {
        return mainGUI;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }
}
