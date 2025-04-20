package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.menu.SellMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class SellCommand implements CommandExecutor {

    private final SellMenu sellMenu;

    public SellCommand(SellMenu sellMenu) {
        this.sellMenu = sellMenu;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;

        Player player = (Player) sender;
        sellMenu.openMenu(player);
        return true;
    }
}
