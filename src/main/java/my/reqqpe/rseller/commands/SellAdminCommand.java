package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SellAdminCommand implements CommandExecutor {

    private final Main plugin;
    private final Database database;

    public SellAdminCommand(Main plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

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
            ConfigurationSection reloadSection = sec.getConfigurationSection("reload");
            if (args.length > 1) {
                String type = args[1];
                if (type.equalsIgnoreCase("items")) {
                    plugin.getItemsConfig().reloadConfig();
                    plugin.getItemManager().load();
                    plugin.getAutoSellManager().loadConfig();

                    String message = reloadSection.getString("items", "&aКонфигурация предметов успешно перезагружена");
                    commandSender.sendMessage(Colorizer.color(message));
                    return true;
                }
                if (type.equalsIgnoreCase("config")) {
                    plugin.reloadConfig();
                    plugin.getAutoSellManager().loadConfig();
                    plugin.getLevelManager().reloadLevels();
                    plugin.getBoosterManager().load();
                    plugin.getFormatManager().reload();

                    String message = reloadSection.getString("config", "&aГлавная конфигурация успешно перезагружена");
                    commandSender.sendMessage(Colorizer.color(message));
                    return true;
                }
                if (type.equalsIgnoreCase("guis")) {
                    HeadUtil.clearCache();
                    plugin.getAutoSellGUIConfig().reloadConfig();
                    plugin.getAllSellGUIConfig().reloadConfig();
                    plugin.getMainGUIConfig().reloadConfig();

                    String message = reloadSection.getString("guis", "&aКонфигурация менюшек успешно перезагружена");
                    commandSender.sendMessage(Colorizer.color(message));
                    return true;
                }
            }



            HeadUtil.clearCache();
            plugin.reloadConfig();
            plugin.getAutoSellGUIConfig().reloadConfig();
            plugin.getAllSellGUIConfig().reloadConfig();
            plugin.getMainGUIConfig().reloadConfig();
            plugin.getItemsConfig().reloadConfig();
            plugin.getItemManager().load();
            plugin.getLevelManager().reloadLevels();
            plugin.getAutoSellManager().loadConfig();
            plugin.getFormatManager().reload();
            plugin.getBoosterManager().load();

            String message = reloadSection.getString("all", "&aПлагин успешно перезагружен");
            commandSender.sendMessage(Colorizer.color(message));
        }

        if (args[0].equalsIgnoreCase("customItem")) {
            if (args.length < 3) {
                commandSender.sendMessage(Colorizer.color("&cИспользование: /" + label + " <create/remove> <id>"));
                return true;
            }

            if (args[1].equalsIgnoreCase("create")) {
                if (!(commandSender instanceof Player player)) {
                    String message = sec.getString("only-player", "&cЭта команда доступна только игрокам!");
                    commandSender.sendMessage(Colorizer.color(message));
                    return true;
                }

                if (args.length < 5) {
                    player.sendMessage(Colorizer.color("&cИспользование: /<command> create <id> [price] [points]"));
                    return true;
                }

                String id = args[2];
                if (!id.matches("[a-zA-Z0-9_]+")) {
                    player.sendMessage(Colorizer.color("&cID предмета может содержать только буквы, цифры и подчеркивания"));
                    return true;
                }

                double price;
                double points;

                try {
                    price = Double.parseDouble(args[3]);
                    if (price < 0) {
                        player.sendMessage(Colorizer.color("&cЦена не может быть отрицательной!"));
                        return true;
                    }
                    points = Double.parseDouble(args[4]);
                    if (points < 0) {
                        player.sendMessage(Colorizer.color("&cОчки не могут быть отрицательными!"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(Colorizer.color("&cНеверный формат цены или очков! Используйте числа (например, 10.0)"));
                    return true;
                }

                if (price <= 0 && points <= 0) {
                    player.sendMessage(Colorizer.color("&cУкажите хотя бы цену или очки больше 0!"));
                    return true;
                }

                ItemStack itemStack = player.getInventory().getItemInMainHand();
                if (itemStack.getType().isAir()) {
                    player.sendMessage(Colorizer.color("&cВозьмите предмет в руку прежде чем пытаться создать"));
                    return true;
                }

                Item item = Item.fromItemStack(id, itemStack, price, points, plugin);
                if (item == null) {
                    player.sendMessage(Colorizer.color("&cПроизошла ошибка при создании предмета, проверьте консоль"));
                    return true;
                }

                if (!plugin.getItemManager().addCustomItem(item)) {
                    player.sendMessage(Colorizer.color("&cПроизошла ошибка при сохранении предмета, проверьте консоль"));
                    return true;
                }

                player.sendMessage(Colorizer.color("&aВы успешно создали предмет с ID: " + id + ", цена: " + price + ", очки: " + points));
                return true;
            }

            if (args[1].equalsIgnoreCase("remove")) {
                String id = args[2];

                if (!plugin.getItemManager().removeCustomItem(id)) {
                    commandSender.sendMessage(Colorizer.color("&cПроизошла ошибка при удалении предмета, посмотрите в консоль"));
                }

                commandSender.sendMessage(Colorizer.color("&aВы успешно удалили предмет с id: " + id + ", теперь выполните перезагрузку /rseller reload items"));
                return true;
            }
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
                    .replace("{value}", String.valueOf(newPoints));
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