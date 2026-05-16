package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

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
        return "mainGUI";
    }


    @Override
    public void openMenu(Player player) {
        super.openMenu(player);
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        super.handleClick(player, e);
    }
}
