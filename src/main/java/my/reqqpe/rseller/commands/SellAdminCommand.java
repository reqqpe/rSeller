package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
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

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("messages");

        if (!commandSender.hasPermission("rseller.admin")) {
            String message = sec.getString("no-permission");
            commandSender.sendMessage(Colorizer.color(message));
            return true;
        }

        if (args.length == 0) {
            String message = sec.getString("no-arguments");
            commandSender.sendMessage(Colorizer.color(message));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getAutoSellGUI().reloadConfig();
            plugin.getMainGUI().reloadConfig();
            plugin.getItemsConfig().reloadConfig();
            plugin.getLevelManager().reloadLevels();
            plugin.getAutoSellManager().loadConfig();
            plugin.getFormatManager().reload();
            plugin.getBoosterManager().load();

            String message = sec.getString("reload");
            commandSender.sendMessage(Colorizer.color(message));
        }
        if (args[0].equalsIgnoreCase("points")) {
            if (args.length < 3) {
                String message = sec.getString("points-usage");
                commandSender.sendMessage(Colorizer.color(message));
                return true;
            }

            String action = args[1].toLowerCase();
            double amount;

            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                String message = sec.getString("un-int").replace("{value}", args[2]);
                commandSender.sendMessage(Colorizer.color(message));
                return true;
            }

            Player target = (args.length >= 4) ? Bukkit.getPlayerExact(args[3]) :
                    (commandSender instanceof Player ? (Player) commandSender : null);

            if (target == null) {
                String message = sec.getString("not-found-player");
                commandSender.sendMessage(Colorizer.color(message));
                return true;
            }
            PlayerData data = database.getPlayerData(target.getUniqueId());
            data.getPoints();

            switch (action) {
                case "add" -> {
                    if (amount < 0) {
                        String message = sec.getString("negative-value");
                        commandSender.sendMessage(Colorizer.color(message));
                        return true;
                    }
                    data.addPoints(amount);
                }
                case "remove" -> {
                    double current = data.getPoints();
                    if (amount < 0) {
                        String message = sec.getString("negative-value");
                        commandSender.sendMessage(Colorizer.color(message));
                        return true;
                    }
                    if (current - amount < 0) {
                        String message = sec.getString("not-enough-points")
                                .replace("{current}", String.valueOf(current));
                        commandSender.sendMessage(Colorizer.color(message));
                        return true;
                    }
                    data.removePoints(amount);
                }
                case "set" -> {
                    if (amount < 0) {
                        String message = sec.getString("negative-set");
                        commandSender.sendMessage(Colorizer.color(message));
                        return true;
                    }
                    data.setPoints(amount);
                }
                default -> {
                    commandSender.sendMessage(Colorizer.color("&cНеизвестное действие: " + action));
                    return true;
                }
            }

            double newPoints = data.getPoints();
            String message = sec.getString("update-points-sender")
                    .replace("{player}", target.getName())
                    .replace("{value}", String.valueOf(newPoints));;
            commandSender.sendMessage(Colorizer.color(message));
            if (!target.equals(commandSender)) {
                String message2 = sec.getString("update-points-target")
                        .replace("{value}", String.valueOf(newPoints));
                target.sendMessage(Colorizer.color(message2));
            }
        }
        return true;
    }

}