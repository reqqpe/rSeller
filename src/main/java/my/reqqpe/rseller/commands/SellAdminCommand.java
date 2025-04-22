package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SellAdminCommand implements CommandExecutor {

    private final Main plugin;
    private final Database database;

    public SellAdminCommand(Main plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!commandSender.hasPermission("rseller.admin")) {
            String message = plugin.getConfig().getString("messages.no-permission");
            commandSender.sendMessage(Colorizer.color(message));
            return true;
        }

        if (args.length == 0) {
            commandSender.sendMessage(Colorizer.color(plugin.getConfig().getString("messages.no-arguments")));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getMainGUI().reloadConfig();
            plugin.getItemsConfig().reloadConfig();
            plugin.reloadConfig();
            commandSender.sendMessage(Colorizer.color(plugin.getConfig().getString("messages.reload")));
        }
        if (args[0].equalsIgnoreCase("points")) {
            if (args.length < 3) {
                commandSender.sendMessage(Colorizer.color(plugin.getConfig().getString("messages.points-usage")));
                return true;
            }

            String action = args[1].toLowerCase();
            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                commandSender.sendMessage(Colorizer.color(plugin.getConfig().getString("messages.un-int").replace("{valuer}", args[2])));
                return true;
            }

            Player target = (args.length >= 4) ? Bukkit.getPlayerExact(args[3]) :
                    (commandSender instanceof Player ? (Player) commandSender : null);

            if (target == null) {
                commandSender.sendMessage(Colorizer.color(plugin.getConfig().getString("messages.not-found-player")));
                return true;
            }
            PlayerData data = database.getPlayerData(target.getUniqueId());
            switch (action) {
                case "add" -> data.addPoints(amount);
                case "remove" -> data.removePoints(amount);
                case "set" -> data.setPoints(amount);
                default -> commandSender.sendMessage(Colorizer.color("&cНеизвестное действие: " + action));
            }

            int newPoints = data.getPoints();
            commandSender.sendMessage(Colorizer.color(plugin.getConfig().getString("messages.update-points-sender")
                    .replace("{player}", target.getName())
                    .replace("{value}", String.valueOf(newPoints))));
            if (!target.equals(commandSender)) {
                commandSender.sendMessage(Colorizer.color(plugin.getConfig().getString("messages.update-points-sender")
                        .replace("{value}", String.valueOf(newPoints))));
            }
        }
        return true;
    }
}
