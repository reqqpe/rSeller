package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.configurations.DataBase;
import my.reqqpe.rseller.utils.colorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SelladminCommand implements CommandExecutor {

    private final Main plugin;
    private final DataBase dataBase;

    public SelladminCommand(Main plugin) {
        this.plugin = plugin;
        this.dataBase = plugin.getDataBase();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!commandSender.hasPermission("rseller.admin")) {
            String message = plugin.getConfig().getString("messages.no-permission");
            commandSender.sendMessage(colorUtils.color(message));
            return true;
        }

        if (args.length == 0) {
            commandSender.sendMessage(colorUtils.color(plugin.getConfig().getString("messages.no-arguments")));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getMainGUI().reloadConfig();
            plugin.getItemsConfig().reloadConfig();
            plugin.reloadConfig();
            commandSender.sendMessage(colorUtils.color(plugin.getConfig().getString("messages.reload")));
        }
        if (args[0].equalsIgnoreCase("points")) {
            if (args.length < 3) {
                commandSender.sendMessage(colorUtils.color(plugin.getConfig().getString("messages.points-usage")));
                return true;
            }

            String action = args[1].toLowerCase();
            int amount;

            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                commandSender.sendMessage(colorUtils.color(plugin.getConfig().getString("messages.un-int").replace("{valuer}", args[2])));
                return true;
            }

            Player target = (args.length >= 4) ? Bukkit.getPlayerExact(args[3]) :
                    (commandSender instanceof Player ? (Player) commandSender : null);

            if (target == null) {
                commandSender.sendMessage(colorUtils.color(plugin.getConfig().getString("messages.not-found-player")));
                return true;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    switch (action) {
                        case "add" -> dataBase.addPoints(target, amount);
                        case "remove" -> dataBase.removePoints(target, amount);
                        case "set" -> dataBase.setPoints(target, amount);
                        default -> {
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    commandSender.sendMessage(colorUtils.color("&cНеизвестное действие: " + action))
                            );
                            return;
                        }
                    }

                    int newPoints = dataBase.getPoints(target);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        commandSender.sendMessage(colorUtils.color(plugin.getConfig().getString("messages.update-points-sender")
                                .replace("{player}", target.getName())
                                .replace("{value}", String.valueOf(newPoints))));
                        if (!target.equals(commandSender)) {
                            commandSender.sendMessage(colorUtils.color(plugin.getConfig().getString("messages.update-points-sender")
                                    .replace("{value}", String.valueOf(newPoints))));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Bukkit.getScheduler().runTask(plugin, () ->
                            commandSender.sendMessage(colorUtils.color("&cПроизошла ошибка при работе с базой данных."))
                    );
                }
            });
            return true;
        }
        return true;
    }
}
