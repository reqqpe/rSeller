package my.reqqpe.rseller.listeners;

import my.reqqpe.rseller.managers.MenuManager;
import my.reqqpe.rseller.menus.AbstractMenu;
import my.reqqpe.rseller.menus.CustomInventoryHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;


public class InventoryClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;

        String id = holder.id();

        AbstractMenu menu = MenuManager.getMenu(id);
        if (menu == null) return;

        menu.handleClick(e);
    }
}
