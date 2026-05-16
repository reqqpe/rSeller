package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.managers.MenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;


public class SellCommand implements CommandExecutor {

    private final Main plugin;

    public SellCommand(Main plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;

        String openedGUI = plugin.getMainConfig().getSellCommand();
        Set<String> allowed = Set.of("mainGUI", "allSellGUI", "autoSellGUI");
        String guiToOpen = allowed.contains(openedGUI) ? openedGUI : "mainGUI";

        switch (guiToOpen) {
            case "allSellGUI" -> MenuManager.openMenu("allSellGUI", player);
            case "autoSellGUI" -> MenuManager.openMenu("autoSellGUI", player);
            case "mainGUI" -> MenuManager.openMenu("mainGUI", player);
        }

        return true;
    }
}
