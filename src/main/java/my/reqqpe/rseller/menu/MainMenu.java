package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MainMenu extends AbstractMenu implements Listener {

    public MainMenu(Main plugin) {
        super(plugin);
    }

    @Override
    protected FileConfiguration getGuiConfig() {
        return plugin.getMainGUIConfig().getConfig();
    }

    @Override
    protected String getMenuId() {
        return "MAIN_MENU";
    }


    @Override
    public void openMenu(Player player) {
        super.openMenu(player);
    }


    @EventHandler
    public void onClickInventory(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        handleClick(player, e);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        cancelItemUpdates(player);
    }

}
