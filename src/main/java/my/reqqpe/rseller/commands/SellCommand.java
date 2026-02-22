package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.managers.MenuManager;
import my.reqqpe.rseller.menus.AbstractMenu;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SellCommand implements CommandExecutor {

    private final RSeller plugin;

    public SellCommand(RSeller plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!commandSender.hasPermission("rseller.sell")) {
            String noPerm = plugin.getConfig().getString("messages.no-permission");
            commandSender.sendMessage(Colorizer.color(noPerm));
            return true;
        }

        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Эту команду может использовать только игрок");
        }

        Player player = (Player) commandSender;

        AbstractMenu menu = MenuManager.getMenu("ALL_SELL_MENU");
        if (menu == null) {
            player.sendMessage("Не удалось открыть меню, возможно указан неверный айди");
            return true;
        }

        menu.openMenu(player);
        return true;
    }
}
