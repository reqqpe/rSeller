package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class SellCommand implements CommandExecutor {

    private final Main plugin;

    public SellCommand(Main plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;

        Player player = (Player) sender;
        String openedGUI = plugin.getOpenedGUI();

        if (openedGUI.equals("allSellGUI")) plugin.getAllSellMenu().openMenu(player);
        if (openedGUI.equals("autoSellGUI")) plugin.getAutoSellMenu().openMenu(player);
        if (openedGUI.equals("mainGUI")) plugin.getMainMenu().openMenu(player);

        return true;
    }
}
