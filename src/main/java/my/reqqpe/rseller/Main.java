package my.reqqpe.rseller;

import my.reqqpe.rseller.commands.SellCommand;
import my.reqqpe.rseller.commands.SelladminCommand;
import my.reqqpe.rseller.configurations.CustomConfigs;
import my.reqqpe.rseller.configurations.DataBase;
import my.reqqpe.rseller.managers.LevelManager;
import my.reqqpe.rseller.managers.SellManager;
import my.reqqpe.rseller.menu.SellMenu;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.UUID;


public final class Main extends JavaPlugin {

    private CustomConfigs itemsConfig;
    private CustomConfigs mainGUI;
    private SellMenu sellMenu;
    private DataBase dataBase;
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


        // DataBase
        try {

            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }

            dataBase = new DataBase(this, getDataFolder().getAbsolutePath() + "/data.db");
        } catch (SQLException ex) {
            ex.printStackTrace();
            getLogger().severe("Не удалось подключиться к базе данных");
            getPluginLoader().disablePlugin(this);
            return;
        }
        // managers
        levelManager = new LevelManager(this);
        sellManager = new SellManager(this);

        // events

        sellMenu = new SellMenu(this, sellManager);
        getServer().getPluginManager().registerEvents(sellMenu, this);

        //commands
        getCommand("sell").setExecutor(new SellCommand(sellMenu));
        getCommand("rseller").setExecutor(new SelladminCommand(this));


    }

    @Override
    public void onDisable() {

        // close connection data_base
        try {
            dataBase.closeConnection();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public CustomConfigs getItemsConfig() {
        return itemsConfig;
    }

    public CustomConfigs getMainGUI() {
        return mainGUI;
    }

    public DataBase getDataBase() {
        return dataBase;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }
}
