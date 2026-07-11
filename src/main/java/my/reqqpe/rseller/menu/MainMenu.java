package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.io.File;

public class MainMenu extends AbstractMenu implements Listener {

    private final FileConfiguration fileConfiguration;
    private final String id;

    public MainMenu(Main plugin, FileConfiguration fileConfiguration, String id) {
        super(plugin);
        this.fileConfiguration = fileConfiguration;
        this.id = id;
    }

    @Override
    protected FileConfiguration getGuiConfig() {
        return fileConfiguration;
    }

    @Override
    protected String getMenuId() {
        return id;
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
