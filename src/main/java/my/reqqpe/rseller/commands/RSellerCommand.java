package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class RSellerCommand implements CommandExecutor {

    private final RSeller plugin;

    public RSellerCommand(RSeller plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!commandSender.hasPermission("rseller.sell")) {
            String noPerm = plugin.getConfig().getString("messages.no-permission");
            commandSender.sendMessage(Colorizer.color(noPerm));
            return true;
        }

        plugin.registerMenus();

        return true;
    }
}
