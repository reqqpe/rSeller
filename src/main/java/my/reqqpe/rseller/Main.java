package my.reqqpe.rseller;

import lombok.Getter;
import my.reqqpe.rseller.commands.SellAdminCommand;
import my.reqqpe.rseller.commands.SellCommand;
import my.reqqpe.rseller.commands.TabCompleteAdmin;
import my.reqqpe.rseller.configurations.CustomConfigs;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.DatabaseListener;
import my.reqqpe.rseller.listeners.PlayerPickupItem;
import my.reqqpe.rseller.managers.*;
import my.reqqpe.rseller.menu.AutoSellMenu;
import my.reqqpe.rseller.menu.MainMenu;
import my.reqqpe.rseller.menu.SellMenu;
import my.reqqpe.rseller.updateCheker.UpdateChecker;
import my.reqqpe.rseller.utils.Metrics;
import my.reqqpe.rseller.utils.SellerPlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class Main extends JavaPlugin {

    private Database database;

    @Getter
    private String openedGUI;

    @Getter
    private CustomConfigs itemsConfig;
    @Getter
    private CustomConfigs allSellGUIConfig;
    @Getter
    private CustomConfigs autoSellGUIConfig;
    @Getter
    private CustomConfigs mainGUIConfig;

    @Getter
    private SellMenu allSellMenu;
    @Getter
    private AutoSellMenu autoSellMenu;
    @Getter
    private MainMenu mainMenu;

    @Getter
    private ItemManager itemManager;
    @Getter
    private SellManager sellManager;
    @Getter
    private AutoSellManager autoSellManager;
    @Getter
    private NumberFormatManager formatManager;
    @Getter
    private BoosterManager boosterManager;
    @Getter
    private LevelManager levelManager;


    public static boolean useNBTAPI = false;
    private Set<String> blockWorlds = new HashSet<>();


    @Override
    public void onEnable() {

        EconomySetup.setupEconomy(this);

        if (EconomySetup.getEconomy() == null) {
            getLogger().severe("Vault не найден или не настроен! Плагин отключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("NBTAPI") != null) {
            useNBTAPI = true;
        }

        saveDefaultConfig();
        loadConfigs();

        formatManager = new NumberFormatManager(this);
        levelManager = new LevelManager(this, database);
        boosterManager = new BoosterManager(this);
        itemManager = new ItemManager(this);
        sellManager = new SellManager(this, database);
        autoSellManager = new AutoSellManager(this);

        if (getServer().getPluginManager().getPlugin("PlaceHolderAPI") != null) {
            new SellerPlaceholderAPI(this, database).register();
            getLogger().info("PlaceHolderAPI найден");
        }

        mainMenu = new MainMenu(this);
        allSellMenu = new SellMenu(this);
        autoSellMenu = new AutoSellMenu(this, database);

        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(allSellMenu, this);
        pm.registerEvents(autoSellMenu, this);
        pm.registerEvents(mainMenu, this);

        pm.registerEvents(new DatabaseListener(database), this);

        getCommand("sell").setExecutor(new SellCommand(this));

        FileConfiguration config = getConfig();

        String openedGUIValue = config.getString("sell-command", "mainGUI");
        Set<String> allowedValue = Set.of("mainGUI", "allSellGUI", "autoSellGUI");
        if (allowedValue.contains(openedGUIValue)) {
            openedGUI = openedGUIValue;
        } else {
            openedGUI = "mainGUI";
        }

        PluginCommand rsellerCommand = getCommand("rseller");
        rsellerCommand.setExecutor(new SellAdminCommand(this, database));
        rsellerCommand.setTabCompleter(new TabCompleteAdmin());

        // other
        if (config.getBoolean("metrics", true)) {
            int pluginId = 25999;
            new Metrics(this, pluginId);
            getLogger().info("bStats успешно инициализирован!");
        }

        boolean autoSellEnabled = config.getBoolean("autosell.enable", true);

        if (autoSellEnabled) {
            pm.registerEvents(new PlayerPickupItem(this, database), this);
        }

        boolean updateCheck = getConfig().getBoolean("update-check", true);

        if (updateCheck) {
            UpdateChecker updateChecker = new UpdateChecker(this);
            updateChecker.check();
        }

    }

    @Override
    public void onDisable() {
        if (database != null) database.saveAll();
    }

    private void loadConfigs() {
        itemsConfig = new CustomConfigs(this, "items.yml");
        itemsConfig.setup();
        getLogger().info("items.yml успешно загружен");

        allSellGUIConfig = new CustomConfigs(this, "GUI/allSellGUI.yml");
        allSellGUIConfig.setup();
        getLogger().info("allSellGUI.yml успешно загружен");

        autoSellGUIConfig = new CustomConfigs(this, "GUI/autoSellGUI.yml");
        autoSellGUIConfig.setup();
        getLogger().info("autoSellGUI.yml успешно загружен");

        mainGUIConfig = new CustomConfigs(this, "GUI/mainGUI.yml");
        mainGUIConfig.setup();
        getLogger().info("mainGUI.yml успешно загружен");

        database = new Database(this);
        for (Player pl : Bukkit.getOnlinePlayers()) {
            database.loadPlayerData(pl.getUniqueId());
        }
    }


    public void reloadBlockWorlds() {
        blockWorlds.clear();

        List<String> list = getConfig().getStringList("autosell.block-worlds");
        blockWorlds.addAll(list);
    }

    public boolean isBlockedWorld(String world) {
        return blockWorlds.contains(world);
    }

}
