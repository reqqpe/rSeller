package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.ItemManager;
import my.reqqpe.rseller.model.ItemData;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Команда администрирования для управления предметами и очками игроков
 */
public class SellAdminCommand implements CommandExecutor {

    private final Main plugin;
    private final Database database;
    private final ItemManager itemManager;

    /**
     * Конструктор команды администрирования
     * @param plugin Главный класс плагина
     * @param database База данных игроков
     */
    public SellAdminCommand(Main plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.itemManager = new ItemManager(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("messages");

        if (!commandSender.hasPermission("rseller.admin")) {
            String message = sec.getString("no-permission", "&cУ вас нет прав для выполнения этой команды!");
            commandSender.sendMessage(Colorizer.color(message));
            return true;
        }

        if (args.length == 0) {
            String message = sec.getString("no-arguments", "&cУкажите аргументы команды!");
            commandSender.sendMessage(Colorizer.color(message));
            return true;
        }

        if (args[0].equalsIgnoreCase("createitem")) {
            if (!(commandSender instanceof Player)) {
                String message = sec.getString("only-player", "&cЭта команда только для игроков!");
                commandSender.sendMessage(Colorizer.color(message));
                return true;
            }

            Player player = (Player) commandSender;
            if (args.length < 4) {
                String message = sec.getString("no-arguments", "&cИспользование: /rseller createitem <id> <price> <points>");
                player.sendMessage(Colorizer.color(message));
                return true;
            }

            String id = args[1].toLowerCase();
            ItemData searchItem = itemManager.getById(id);
            if (searchItem != null) {
                String message = sec.getString("already-exists-item", "&cПредмет с ID {id} уже существует!");
                player.sendMessage(Colorizer.color(message.replace("{id}", id)));
                return true;
            }

            double price;
            double points;

            try {
                price = Double.parseDouble(args[2]);
                points = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                String message = "&cОшибка: Некорректный формат цены или очков!";
                player.sendMessage(Colorizer.color(message));
                return true;
            }

            ItemStack itemStack = player.getInventory().getItemInMainHand();
            if (itemStack == null || itemStack.getType().isAir()) {
                String message = sec.getString("no-item", "&cДержите предмет в руке!");
                player.sendMessage(Colorizer.color(message));
                return true;
            }

            if (itemManager.createItem(id, points, price, itemStack)) {
                String message = sec.getString("create-success", "&aПредмет с ID {id} успешно создан!");
                player.sendMessage(Colorizer.color(message.replace("{id}", id)));
                plugin.getItemsConfig().reloadConfig();
                itemManager.load();
            } else {
                String message = sec.getString("create-failed", "&cНе удалось создать предмет с ID {id}!");
                player.sendMessage(Colorizer.color(message.replace("{id}", id)));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("deleteitem")) {
            if (args.length < 2) {
                String message = sec.getString("deleteitem-usage", "&cИспользование: /rseller deleteitem <id>");
                commandSender.sendMessage(Colorizer.color(message));
                return true;
            }

            String id = args[1].toLowerCase();
            ItemData searchItem = itemManager.getById(id);
            if (searchItem == null) {
                String message = sec.getString("item-not-found", "&cПредмет с ID {id} не найден!");
                commandSender.sendMessage(Colorizer.color(message.replace("{id}", id)));
                return true;
            }

            if (itemManager.removeItem(id)) {
                String message = sec.getString("deleteitem-success", "&aПредмет с ID {id} успешно удален!");
                commandSender.sendMessage(Colorizer.color(message.replace("{id}", id)));
                plugin.getItemsConfig().reloadConfig();
                itemManager.load();
            } else {
                String message = sec.getString("deleteitem-failed", "&cНе удалось удалить предмет с ID {id}!");
                commandSender.sendMessage(Colorizer.color(message.replace("{id}", id)));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getAutoSellGUI().reloadConfig();
            plugin.getMainGUI().reloadConfig();
            plugin.getItemsConfig().reloadConfig();
            plugin.getLevelManager().reloadLevels();
            plugin.getAutoSellManager().loadConfig();
            itemManager.load();
            String message = sec.getString("reload", "&aКонфигурация успешно перезагружена!");
            commandSender.sendMessage(Colorizer.color(message));
            return true;
        }

        if (args[0].equalsIgnoreCase("points")) {
            if (args.length < 3) {
                String message = sec.getString("points-usage", "&cИспользование: /rseller points <add/remove/set> <количество> [игрок]");
                commandSender.sendMessage(Colorizer.color(message));
                return true;
            }

            String action = args[1].toLowerCase();
            double amount;

            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                String message = sec.getString("un-int", "&c{value} не является числом!").replace("{value}", args[2]);
                commandSender.sendMessage(Colorizer.color(message));
                return true;
            }

            Player target = (args.length >= 4) ? Bukkit.getPlayerExact(args[3]) :
                    (commandSender instanceof Player ? (Player) commandSender : null);

            if (target == null) {
                String message = sec.getString("not-found-player", "&cИгрок не найден!");
                commandSender.sendMessage(Colorizer.color(message));
                return true;
            }

            PlayerData data = database.getPlayerData(target.getUniqueId());

            switch (action) {
                case "add" -> {
                    if (amount < 0) {
                        String message = sec.getString("negative-value", "&cЗначение не может быть отрицательным!");
                        commandSender.sendMessage(Colorizer.color(message));
                        return true;
                    }
                    data.addPoints(amount);
                }
                case "remove" -> {
                    double current = data.getPoints();
                    if (amount < 0) {
                        String message = sec.getString("negative-value", "&cЗначение не может быть отрицательным!");
                        commandSender.sendMessage(Colorizer.color(message));
                        return true;
                    }
                    if (current - amount < 0) {
                        String message = sec.getString("not-enough-points", "&cНедостаточно очков! Текущие очки: {current}")
                                .replace("{current}", String.valueOf(current));
                        commandSender.sendMessage(Colorizer.color(message));
                        return true;
                    }
                    data.removePoints(amount);
                }
                case "set" -> {
                    if (amount < 0) {
                        String message = sec.getString("negative-set", "&cЗначение не может быть отрицательным!");
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
            String message = sec.getString("update-points-sender", "&aОчки игрока {player} обновлены: {value}")
                    .replace("{player}", target.getName())
                    .replace("{value}", String.valueOf(newPoints));
            commandSender.sendMessage(Colorizer.color(message));
            if (!target.equals(commandSender)) {
                String message2 = sec.getString("update-points-target", "&aВаши очки обновлены: {value}")
                        .replace("{value}", String.valueOf(newPoints));
                target.sendMessage(Colorizer.color(message2));
            }
            return true;
        }

        return true;
    }
}