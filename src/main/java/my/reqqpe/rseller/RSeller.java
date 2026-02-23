package my.reqqpe.rseller;

import my.reqqpe.rseller.commands.RSellerCommand;
import my.reqqpe.rseller.commands.SellCommand;
import my.reqqpe.rseller.database.DataBase;
import my.reqqpe.rseller.hooks.EconomyHook;
import my.reqqpe.rseller.hooks.PAPIHook;
import my.reqqpe.rseller.listeners.DataBaseListeners;
import my.reqqpe.rseller.listeners.InventoryClickListener;
import my.reqqpe.rseller.listeners.InventoryCloseListener;


import my.reqqpe.rseller.listeners.PlayerPickupItem;
import my.reqqpe.rseller.managers.*;
import my.reqqpe.rseller.menus.AbstractMenu;
import my.reqqpe.rseller.menus.AllSellMenu;
import my.reqqpe.rseller.menus.AutoSellMenu;
import my.reqqpe.rseller.tasks.AutoSellTask;
import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.CustomConfig;
import my.reqqpe.rseller.utils.LoggerUtil;
import my.reqqpe.rseller.utils.MessageUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class RSeller extends JavaPlugin implements Listener {

    private BukkitAudiences adventure;

    private DataBase dataBase;
    private ItemManager itemManager;
    private SellManager sellManager;
    private AutoSellManager autoSellManager;
    private LevelManager levelManager;
    private MultiplierManager multiplierManager;


    private Set<String> blockWorlds = new HashSet<>();


    @NonNull
    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }


    @Override
    public void onEnable() {
        //
        LoggerUtil.setLogger(getLogger());
        saveDefaultConfig();

        setupMessages();
        //
        if (!enabledHooks()) {
            LoggerUtil.fine("Not all dependencies were found, the plugin is disabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.adventure = BukkitAudiences.create(this);


        boolean useLegacyFormat = getConfig().getBoolean("use-legacy-format", false);
        Colorizer.setLEGACY_FORMAT(useLegacyFormat);

        dataBase = new DataBase(this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            dataBase.loadPlayerData(player.getUniqueId());
        }

        itemManager = new ItemManager(this);
        levelManager = new LevelManager(this, dataBase);
        multiplierManager = new MultiplierManager(this, levelManager);
        sellManager = new SellManager(this, itemManager, dataBase, levelManager, multiplierManager);
        autoSellManager = new AutoSellManager(this, itemManager);

        getCommand("sell").setExecutor(new SellCommand(this));
        getCommand("rseller").setExecutor(new RSellerCommand(this));

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new InventoryClickListener(), this);
        pm.registerEvents(new InventoryCloseListener(), this);
        pm.registerEvents(new DataBaseListeners(dataBase), this);

        registerMenus();

        setupBlockWorlds();

        boolean autoSellEnabled = getConfig().getBoolean("autosell.enabled", true);
        if (autoSellEnabled) {
            String type = getConfig().getString("autosell.type", "pickup").toLowerCase();
            if (type.equals("task")) {
                new AutoSellTask(this, multiplierManager, itemManager, dataBase, levelManager)
                        .startTask();
            } else {
                pm.registerEvents(
                        new PlayerPickupItem(this, itemManager, multiplierManager, dataBase, levelManager), this
                );
            }
        }


    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        if (dataBase != null) {
            dataBase.saveAll();
        }
        MenuManager.unRegisterAllMenus();
    }


    private boolean enabledHooks() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIHook().register();
            getLogger().info("PlaceholderAPI connect");
        } else {
            getLogger().severe("PlaceholderAPI not found");
            return false;
        }
        if (!EconomyHook.setupEconomy(this)) {
            getLogger().severe("Vault not found or not configured!");
            return false;
        }
        return true;
    }

    public void registerMenus() {
        if (MenuManager.getMenu("ALL_SELL_MENU") != null) {
            MenuManager.unRegisterMenu("ALL_SELL_MENU");
        }
        CustomConfig allSellMenuConfig = new CustomConfig(this, "GUI/ALL_SELL_MENU.yml");
        AbstractMenu allSellMenu = new AllSellMenu(
                allSellMenuConfig.getConfig(),
                this,
                sellManager,
                itemManager,
                multiplierManager
        );
        MenuManager.registerMenu(allSellMenu.getMenuId(), allSellMenu);


        if (MenuManager.getMenu("AUTO_SELL_MENU") != null) {
            MenuManager.unRegisterMenu("AUTO_SELL_MENU");
        }
        CustomConfig autoSellMenuConfig = new CustomConfig(this, "GUI/AUTO_SELL_MENU.yml");
        AbstractMenu autoSellMenu = new AutoSellMenu(
                autoSellMenuConfig.getConfig(),
                this,
                autoSellManager,
                itemManager,
                dataBase,
                multiplierManager
        );
        MenuManager.registerMenu(autoSellMenu.getMenuId(), autoSellMenu);
    }

    public void setupMessages() {
        String lang = getConfig().getString("message-lang", "ru");

        CustomConfig customConfig = new CustomConfig(this, "messages/" + lang + ".yml");
        MessageUtil.setConfig(customConfig.getConfig());
        MessageUtil.setPlugin(this);
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
}
