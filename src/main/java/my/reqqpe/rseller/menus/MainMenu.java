package my.reqqpe.rseller.menus;

import my.reqqpe.rseller.RSeller;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MainMenu extends AbstractMenu {

    private final RSeller rSeller;

    public MainMenu(FileConfiguration guiConfig, RSeller rSeller) {
        super(guiConfig);
        this.rSeller = rSeller;
    }

    @Override
    public String getMenuId() {
        return "MAIN_MENU";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return rSeller;
    }


    @Override
    public void openMenu(Player player) {
        super.openMenu(player);
    }
}
