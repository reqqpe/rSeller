package my.reqqpe.rseller;

import my.reqqpe.rseller.database.DataBase;
import my.reqqpe.rseller.listeners.DataBaseListeners;
import my.reqqpe.rseller.listeners.InventoryClickListener;
import my.reqqpe.rseller.listeners.InventoryCloseListener;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


public final class RSeller extends JavaPlugin implements Listener {
    private DataBase dataBase;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataBase = new DataBase(this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            dataBase.loadPlayerData(player.getUniqueId());
        }

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new InventoryClickListener(), this);
        pm.registerEvents(new InventoryCloseListener(), this);
        pm.registerEvents(new DataBaseListeners(dataBase), this);



    }

    @Override
    public void onDisable() {
        if (dataBase != null) {
            dataBase.saveAll();
        }
    }
}
