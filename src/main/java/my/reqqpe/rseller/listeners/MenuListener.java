package my.reqqpe.rseller.listeners;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.managers.MenuManager;
import my.reqqpe.rseller.menu.AbstractMenu;
import my.reqqpe.rseller.menu.CustomInventoryHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuListener implements Listener {

    private final Main plugin;

    public MenuListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;

        String id = holder.id();

        AbstractMenu menu = MenuManager.getMenu(id);
        if (menu == null) return;

        Player player = (Player) e.getWhoClicked();
        menu.handleClick(player, e);
    }
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        AbstractMenu menu = MenuManager.getMenu(holder.id());
        if (menu == null) return;

        Player player = (Player) e.getPlayer();

        menu.closeMenu(player, e);
    }
}
