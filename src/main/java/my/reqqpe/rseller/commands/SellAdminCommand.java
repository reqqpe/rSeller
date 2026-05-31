package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.configs.MigrateConfig;
import my.reqqpe.rseller.configs.impl.MessageConfig;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.database.repositories.PlayerRepository;
import my.reqqpe.rseller.events.PointsUpdateEvent;
import my.reqqpe.rseller.managers.MenuManager;
import my.reqqpe.rseller.menu.AutoSellMenu;
import my.reqqpe.rseller.menu.MainMenu;
import my.reqqpe.rseller.menu.SellMenu;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.HeadUtil;import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SellAdminCommand implements CommandExecutor {

    private final Main plugin;

    public SellAdminCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        MessageConfig msg = plugin.getMessageConfig();

        if (!commandSender.hasPermission("rseller.admin")) {
            commandSender.sendMessage(Colorizer.color(msg.getNoPermission()));
            return true;
        }

        if (args.length == 0) {
            commandSender.sendMessage(Colorizer.color(msg.getNoArguments()));
            return true;
        }
        if (args[0].equalsIgnoreCase("migrate")) {

            if (commandSender instanceof Player) {
                commandSender.sendMessage(Colorizer.color(msg.getOnlyConsole()));
                return true;
            }

            MigrateConfig migrateConfig = new MigrateConfig();
            migrateConfig.migrate(plugin);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (args.length > 1) {
                String type = args[1];
                if (type.equalsIgnoreCase("items")) {
                    reloadItems();
                    commandSender.sendMessage(Colorizer.color(msg.getReloadItems()));
                    return true;
                }
                if (type.equalsIgnoreCase("config")) {
                    reloadConfig();
                    commandSender.sendMessage(Colorizer.color(msg.getReloadConfig()));
                    return true;
                }
                if (type.equalsIgnoreCase("guis")) {
                    reloadGUIs();
                    commandSender.sendMessage(Colorizer.color(msg.getReloadGuis()));
                    return true;
                }
            }

            reloadConfig();
            reloadItems();
            reloadGUIs();

            if (plugin.setupEconomy()) {
                commandSender.sendMessage(Colorizer.color(msg.getReloadAll()));
            } else {
                commandSender.sendMessage(Colorizer.color("&cError check console"));
            }
        }

        if (args[0].equalsIgnoreCase("customitem")) {
            if (args.length < 3) {
                commandSender.sendMessage(Colorizer.color("&cИспользование: /" + label + " <create/remove> <id>"));
                return true;
            }

            if (args[1].equalsIgnoreCase("create")) {
                if (!(commandSender instanceof Player player)) {
                    commandSender.sendMessage(Colorizer.color(msg.getOnlyPlayer()));
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
                    player.sendMessage(Colorizer.color(msg.getNoItem()));
                    return true;
                }

                Item item = Item.fromItemStack(id, itemStack, price, points, plugin);
                if (item == null) {
                    player.sendMessage(Colorizer.color("&cПроизошла ошибка при создании предмета, проверьте консоль"));
                    return true;
                }

                if (!plugin.getItemStorage().save(item)) {
                    player.sendMessage(Colorizer.color("&cПроизошла ошибка при сохранении предмета, проверьте консоль"));
                    return true;
                }

                player.sendMessage(Colorizer.color(msg.getCreateSuccess()
                        .replace("{id}", id)
                        .replace("{price}", String.valueOf(price))
                        .replace("{points}", String.valueOf(points))));
                return true;
            }

            if (args[1].equalsIgnoreCase("remove")) {
                String id = args[2];

                if (!plugin.getItemStorage().delete(id)) {
                    commandSender.sendMessage(Colorizer.color("&cПроизошла ошибка при удалении предмета, посмотрите в консоль"));
                }

                commandSender.sendMessage(Colorizer.color("&aВы успешно удалили предмет с id: " + id + ", теперь выполните перезагрузку /rseller reload items"));
                return true;
            }
        }

        if (args[0].equalsIgnoreCase("points")) {
            if (args.length < 3) {
                commandSender.sendMessage(Colorizer.color(msg.getPointsUsage()));
                return true;
            }

            String action = args[1].toLowerCase();
            double amount;

            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                commandSender.sendMessage(Colorizer.color(msg.getUnInt().replace("{value}", args[2])));
                return true;
            }

            Player target = (args.length >= 4) ? Bukkit.getPlayerExact(args[3]) :
                    (commandSender instanceof Player ? (Player) commandSender : null);

            if (target == null) {
                commandSender.sendMessage(Colorizer.color(msg.getNotFoundPlayer()));
                return true;
            }

            PlayerData data = PlayerDataCache.getOrCreate(target.getUniqueId());

            switch (action) {
                case "add" -> {
                    if (amount < 0) {
                        commandSender.sendMessage(Colorizer.color(msg.getNegativeValue()));
                        return true;
                    }
                    data.addPoints(amount);
                }
                case "remove" -> {
                    double current = data.getPoints();
                    if (amount < 0) {
                        commandSender.sendMessage(Colorizer.color(msg.getNegativeValue()));
                        return true;
                    }
                    if (current - amount < 0) {
                        commandSender.sendMessage(Colorizer.color(msg.getNotEnoughPoints()
                                .replace("{current}", String.valueOf(current))));
                        return true;
                    }
                    data.removePoints(amount);
                }
                case "set" -> {
                    if (amount < 0) {
                        commandSender.sendMessage(Colorizer.color(msg.getNegativeSet()));
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
            commandSender.sendMessage(Colorizer.color(msg.getUpdatePointsSender()
                    .replace("{player}", target.getName())
                    .replace("{value}", String.valueOf(newPoints))));
            if (!target.equals(commandSender)) {
                target.sendMessage(Colorizer.color(msg.getUpdatePointsTarget()
                        .replace("{value}", String.valueOf(newPoints))));
            }
            Bukkit.getPluginManager().callEvent(new PointsUpdateEvent(target, amount, true, action));
        }
        return true;
    }



    private void reloadGUIs() {
        HeadUtil.clearCache();
        plugin.getAutoSellGUIConfig().reload();
        MenuManager.reloadMenu("autoSellGUI", new AutoSellMenu(plugin));
        plugin.getAllSellGUIConfig().reload();
        MenuManager.reloadMenu("allSellGUI", new SellMenu(plugin));
        plugin.getMainGUIConfig().reload();
        MenuManager.reloadMenu("mainGUI", new MainMenu(plugin));
    }

    private void reloadConfig() {
        plugin.reloadConfig();
        plugin.getMainConfig().reload();
        plugin.getMessageConfig().reload();
        plugin.getLevelConfig().reload();
        plugin.getAutoSellManager().loadConfig();
        plugin.getLevelManager().reloadLevels();
        plugin.getBoosterManager().load();
        plugin.getFormatManager().reload();
        plugin.setupBlockWorlds();
    }
    private void reloadItems() {
        plugin.getItemsConfig().reload();
        plugin.getItemManager().reload();
    }
}