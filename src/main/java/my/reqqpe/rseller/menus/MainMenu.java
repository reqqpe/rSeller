package my.reqqpe.rseller.menus;

import my.reqqpe.rseller.RSeller;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MainMenu extends AbstractMenu {

    public MainMenu(FileConfiguration guiConfig, RSeller plugin) {
        super(guiConfig, plugin);
    }

    @Override
    public String getMenuId() {
        return "MAIN_MENU";
    }

    @Override
    public void openMenu(Player player) {
        super.openMenu(player);
    }
}
