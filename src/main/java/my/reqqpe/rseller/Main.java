package my.reqqpe.rseller;

import lombok.Getter;
import my.reqqpe.rseller.commands.SellAdminCommand;
import my.reqqpe.rseller.commands.SellCommand;
import my.reqqpe.rseller.commands.TabCompleteAdmin;
import my.reqqpe.rseller.configs.CustomConfig;
import my.reqqpe.rseller.configs.impl.ItemsConfig;
import my.reqqpe.rseller.configs.storage.ItemStorage;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.DatabaseListener;
import my.reqqpe.rseller.economy.CoinsEngineEconomyProvider;
import my.reqqpe.rseller.economy.EconomyProvider;
import my.reqqpe.rseller.economy.PlayerPointsEconomyProvider;
import my.reqqpe.rseller.economy.VaultEconomyProvider;
import my.reqqpe.rseller.listeners.MenuListener;
import my.reqqpe.rseller.listeners.PlayerPickupItem;
import my.reqqpe.rseller.managers.*;
import my.reqqpe.rseller.menu.AutoSellMenu;
import my.reqqpe.rseller.menu.MainMenu;
import my.reqqpe.rseller.menu.SellMenu;
import my.reqqpe.rseller.updateCheker.UpdateChecker;
import my.reqqpe.rseller.utils.Metrics;
import my.reqqpe.rseller.utils.SellerPlaceholderAPI;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
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
    private ItemsConfig itemsConfig;
    @Getter
    private ItemStorage itemStorage;


    @Getter
    private CustomConfig allSellGUIConfig;
    @Getter
    private CustomConfig autoSellGUIConfig;
    @Getter
    private CustomConfig mainGUIConfig;

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

    @Getter
    private EconomyProvider economy;

    @Getter
    private Main instance;


    public static boolean useNBTAPI = false;
    private Set<String> blockWorlds = new HashSet<>();


    @Override
    public void onEnable() {
        instance = this;



        saveDefaultConfig();
        loadConfigs();

        if (!setupEconomy()) return;

        if (getServer().getPluginManager().getPlugin("NBTAPI") != null) {
            useNBTAPI = true;
        }

        formatManager = new NumberFormatManager(this);
        levelManager = new LevelManager(this, database);
        boosterManager = new BoosterManager(this);
        itemManager = new ItemManager(this);
        sellManager = new SellManager(this, database);
        autoSellManager = new AutoSellManager(this);

        if (getServer().getPluginManager().getPlugin("PlaceHolderAPI") == null) {
            getLogger().severe("PlaceholderAPI не найден");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        new SellerPlaceholderAPI(this, database).register();


        MenuManager.registerMenu("mainGUI", new MainMenu(this));
        MenuManager.registerMenu("allSellGUI", new SellMenu(this));
        MenuManager.registerMenu("autoSellGUI", new AutoSellMenu(this, database));


        PluginManager pm = getServer().getPluginManager();

        pm.registerEvents(new MenuListener(this), this);

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
            setupBlockWorlds();
            setupSettingsAutoSell();
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
        itemsConfig = new CustomConfig(this, "items.yml");
        itemsConfig.setup();
        getLogger().info("items.yml успешно загружен");

        allSellGUIConfig = new CustomConfig(this, "GUI/allSellGUI.yml");
        allSellGUIConfig.setup();
        getLogger().info("allSellGUI.yml успешно загружен");

        autoSellGUIConfig = new CustomConfig(this, "GUI/autoSellGUI.yml");
        autoSellGUIConfig.setup();
        getLogger().info("autoSellGUI.yml успешно загружен");

        mainGUIConfig = new CustomConfig(this, "GUI/mainGUI.yml");
        mainGUIConfig.setup();
        getLogger().info("mainGUI.yml успешно загружен");

        database = new Database(this);
        for (Player pl : Bukkit.getOnlinePlayers()) {
            database.loadPlayerData(pl.getUniqueId());
        }
    }


    public void setupBlockWorlds() {
        blockWorlds.clear();

        List<String> list = getConfig().getStringList("autosell.worlds-list");
        boolean whitelist = getConfig().getBoolean("autosell.type-list", false);
        if (whitelist) {
            for (World world : Bukkit.getWorlds()) {
                String worldName = world.getName();
                if (!list.contains(worldName)) {
                    blockWorlds.add(worldName);
                }
            }
        } else {
            blockWorlds.addAll(list);
        }
    }

    public boolean isBlockedWorld(String world) {
        return blockWorlds.contains(world);
    }


    public boolean setupEconomy() {
        ConfigurationSection economySection = getConfig().getConfigurationSection("economy");
        String type = "VAULT";
        if (economySection != null) {
            type = economySection.getString("type", "VAULT");
        }

        switch (type.toUpperCase()) {
            case "VAULT": {
                if (getServer().getPluginManager().getPlugin("Vault") == null) {
                    getLogger().severe("Not found Vault, plugin disable");
                    getServer().getPluginManager().disablePlugin(this);
                    return false;
                }
                this.economy = new VaultEconomyProvider();
                return true;
            }
            case "COINSENGINE": {
                if (getServer().getPluginManager().getPlugin("CoinsEngine") == null) {
                    getLogger().severe("Not found CoinsEngine, plugin disable");
                    getServer().getPluginManager().disablePlugin(this);
                    return false;
                }

                String currency = economySection.getString("coinsengine.currency", "money");
                this.economy = new CoinsEngineEconomyProvider(currency);
                return true;
            }
            case "PLAYERPOINTS": {
                if (getServer().getPluginManager().getPlugin("PlayerPoints") == null) {
                    getLogger().severe("Not found PlayerPoints, plugin disable");
                    getServer().getPluginManager().disablePlugin(this);
                    return false;
                }

                PlayerPointsAPI playerPointsAPI = PlayerPoints.getInstance().getAPI();
                this.economy = new PlayerPointsEconomyProvider(playerPointsAPI);
            }
            default: {
                getLogger().severe("Unknow economy type: " + type);
                getLogger().severe("Plugin disable");
                getServer().getPluginManager().disablePlugin(this);
                return false;
            }
        }
    }


    private void setupSettingsAutoSell() {
        String type = getConfig().getString("autosell.settings.type", "pickup");

        if (type.equals("task")) {
            int delay = getConfig().getInt("autosell.settings.task-delay", 5);
            new AutoSellTask(delay, this, database, formatManager).startTask();
        } else {
            boolean inventorySell = getConfig().getBoolean("autosell.settings.sell-inventory", false);
            getServer().getPluginManager().registerEvents(new PlayerPickupItem(this, database, inventorySell), this);
        }
    }
}
