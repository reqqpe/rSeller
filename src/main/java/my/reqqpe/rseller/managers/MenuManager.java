package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.menus.AbstractMenu;
import my.reqqpe.rseller.menus.CustomInventoryHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class MenuManager {
    private static final Map<String, AbstractMenu> menus = new HashMap<>();




    public static AbstractMenu getMenu(String id) {
        return menus.get(id);
    }
    public static void addMenu(String id, AbstractMenu menu) {
        menus.put(id, menu);
    }
    public static void removeMenu(String id) {
        if (!menus.containsKey(id)) {
            return;
        }
        closeMenuAllPlayers(id);
        menus.remove(id);
    }



    public static void closeMenuAllPlayers(String id) {
        if (!menus.containsKey(id)) {
            return;
        }

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            Inventory inventory = player.getOpenInventory().getTopInventory();
            if (inventory == null) {
                return;
            }
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof CustomInventoryHolder holder) {
                if (holder.id().equals(id)) {
                    player.closeInventory();
                }
            }
        }
    }

}
