package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.menu.AbstractMenu;
import my.reqqpe.rseller.menu.CustomInventoryHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MenuManager {
    private static final Map<String, AbstractMenu> menus = new HashMap<>();


    public static AbstractMenu getMenu(String name) {
        return menus.get(name);
    }

    public static void registerMenu(String name, AbstractMenu menu) {
        menus.put(name, menu);
    }

    public static void unRegisterMenu(String name) {
        if (menus.containsKey(name)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof CustomInventoryHolder holder) {
                    if (holder.id().equals(name)) {
                        player.closeInventory();
                        menus.remove(name);
                    }
                }
            }
        }
    }
    public static void reloadMenu(String name, AbstractMenu menu) {
        unRegisterMenu(name);
        registerMenu(name, menu);
    }

    public static void openMenu(String name, Player player) {
        menus.get(name).openMenu(player);
    }

    public static boolean hasMenu(String name) {
        return menus.containsKey(name);
    }
}
