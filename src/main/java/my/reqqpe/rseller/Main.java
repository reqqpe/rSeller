package my.reqqpe.rseller;

import lombok.Getter;
import my.reqqpe.rseller.commands.*;
import my.reqqpe.rseller.configs.CustomConfig;
import my.reqqpe.rseller.configs.impl.DataBaseConfig;
import my.reqqpe.rseller.configs.impl.ItemsConfig;
import my.reqqpe.rseller.configs.impl.LevelConfig;
import my.reqqpe.rseller.configs.impl.MainConfig;
import my.reqqpe.rseller.configs.impl.MessageConfig;
import my.reqqpe.rseller.configs.storage.ItemStorage;
import my.reqqpe.rseller.database.DataBaseManager;
import my.reqqpe.rseller.listeners.DatabaseListener;
import my.reqqpe.rseller.database.repositories.PlayerRepository;
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
import my.reqqpe.rseller.tasks.AutoSellMessageTask;
import my.reqqpe.rseller.tasks.AutoSellTask;
import my.reqqpe.rseller.tasks.SavePlayerDataCacheTask;
import my.reqqpe.rseller.updateCheker.UpdateChecker;
import my.reqqpe.rseller.utils.Metrics;
import my.reqqpe.rseller.utils.SellerPlaceholderAPI;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class Main extends JavaPlugin {

    @Getter
    private PlayerRepository playerRepository;

    @Getter
    private ItemsConfig itemsConfig;
    @Getter
    private ItemStorage itemStorage;
    @Getter
    private LevelConfig levelConfig;
    @Getter
    private MessageConfig messageConfig;
    @Getter
    private MainConfig mainConfig;
    @Getter
    private DataBaseConfig dataBaseConfig;

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

    private DataBaseManager dataBaseManager;

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
        levelManager = new LevelManager(levelConfig);
        boosterManager = new BoosterManager(this);
        itemManager = new ItemManager(itemsConfig, this);
        sellManager = new SellManager(this);
        autoSellManager = new AutoSellManager(this);

        if (getServer().getPluginManager().getPlugin("PlaceHolderAPI") == null) {
            getLogger().severe(messageConfig.getConsolePlaceholderApiNotFound());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        new SellerPlaceholderAPI(this).register();

        MenuManager.registerMenu("mainGUI", new MainMenu(this));
        MenuManager.registerMenu("allSellGUI", new SellMenu(this));
        MenuManager.registerMenu("autoSellGUI", new AutoSellMenu(this));

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MenuListener(this), this);
        pm.registerEvents(new DatabaseListener(playerRepository), this);

        getCommand("sell").setExecutor(new SellCommand(this));

        PluginCommand rsellerCommand = getCommand("rseller");
        rsellerCommand.setExecutor(new SellAdminCommand(this));
        rsellerCommand.setTabCompleter(new TabCompleteAdmin());

        PluginCommand autosellCommand = getCommand("autosell");
        autosellCommand.setExecutor(new AutoSellCommand(this, autoSellManager, itemManager));
        autosellCommand.setTabCompleter(new AutoSellTabComplete(autoSellManager, itemManager));

        new SavePlayerDataCacheTask(this, playerRepository, dataBaseConfig);

        if (mainConfig.isMetrics()) {
            new Metrics(this, 25999);
            getLogger().info(getMessageConfig().getConsoleBstatsInitialized());
        }

        if (mainConfig.getAutosell().isEnabled()) {
            setupBlockWorlds();
            setupSettingsAutoSell();
        }

        if (mainConfig.isUpdateCheck()) {
            new UpdateChecker(this).check();
        }
    }

    @Override
    public void onDisable() {
        if (playerRepository != null) playerRepository.saveAll();
        if (dataBaseManager != null) dataBaseManager.close();
    }

    private void loadConfigs() {
        messageConfig = new MessageConfig(this);
        messageConfig.setup();

        mainConfig = new MainConfig(this);
        mainConfig.setup();

        dataBaseConfig = new DataBaseConfig(this);
        dataBaseConfig.setup();

        itemsConfig = new ItemsConfig(this);
        itemsConfig.setup();

        levelConfig = new LevelConfig(this);
        levelConfig.setup();

        allSellGUIConfig = new CustomConfig(this, "GUI/allSellGUI.yml") {
            @Override protected void load() {}
        };
        allSellGUIConfig.setup();

        autoSellGUIConfig = new CustomConfig(this, "GUI/autoSellGUI.yml") {
            @Override protected void load() {}
        };
        autoSellGUIConfig.setup();

        mainGUIConfig = new CustomConfig(this, "GUI/mainGUI.yml") {
            @Override protected void load() {}
        };
        mainGUIConfig.setup();

        itemStorage = new ItemStorage(this);

        dataBaseManager = new DataBaseManager(this, dataBaseConfig);
        playerRepository = new PlayerRepository(dataBaseManager);
        playerRepository.createTable();

        for (Player pl : Bukkit.getOnlinePlayers()) {
            playerRepository.loadPlayerData(pl.getUniqueId());
        }
    }

    public void setupBlockWorlds() {
        blockWorlds.clear();

        List<String> list = mainConfig.getAutosell().getWorlds();
        boolean whitelist = mainConfig.getAutosell().isWhitelist();
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

    public void playSound(Player player, String soundName, float volume, float pitch) {
        if (soundName == null || soundName.isBlank()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Unknown sound: " + soundName);
        }
    }

    public boolean setupEconomy() {
        String type = mainConfig.getEconomy().getType();

        switch (type.toUpperCase()) {
            case "VAULT": {
                if (getServer().getPluginManager().getPlugin("Vault") == null) {
                    getLogger().severe(getMessageConfig().getConsoleEconomyNotFound().replace("{type}", "Vault"));
                    getServer().getPluginManager().disablePlugin(this);
                    return false;
                }
                this.economy = new VaultEconomyProvider();
                return true;
            }
            case "COINSENGINE": {
                if (getServer().getPluginManager().getPlugin("CoinsEngine") == null) {
                    getLogger().severe(getMessageConfig().getConsoleEconomyNotFound().replace("{type}", "CoinsEngine"));
                    getServer().getPluginManager().disablePlugin(this);
                    return false;
                }
                String currency = mainConfig.getEconomy().getCoinsengineCurrency();
                this.economy = new CoinsEngineEconomyProvider(currency);
                return true;
            }
            case "PLAYERPOINTS": {
                if (getServer().getPluginManager().getPlugin("PlayerPoints") == null) {
                    getLogger().severe(getMessageConfig().getConsoleEconomyNotFound().replace("{type}", "PlayerPoints"));
                    getServer().getPluginManager().disablePlugin(this);
                    return false;
                }
                PlayerPointsAPI playerPointsAPI = PlayerPoints.getInstance().getAPI();
                this.economy = new PlayerPointsEconomyProvider(playerPointsAPI);
                return true;
            }
            default: {
                getLogger().severe(getMessageConfig().getConsoleEconomyUnknown().replace("{type}", type));
                getServer().getPluginManager().disablePlugin(this);
                return false;
            }
        }
    }

    private void setupSettingsAutoSell() {
        String type = mainConfig.getAutosell().getTypeSell();

        if (type.equalsIgnoreCase("task")) {
            new AutoSellTask(this, sellManager).startTask();
        } else {
            boolean inventorySell = mainConfig.getAutosell().isSellInventory();
            getServer().getPluginManager().registerEvents(new PlayerPickupItem(this, sellManager, inventorySell), this);
        }

        String messageType = mainConfig.getAutosell().getTypeMessage();

        if (messageType.equalsIgnoreCase("task")) {
            new AutoSellMessageTask(this, formatManager).startTask();
        }
    }

}
