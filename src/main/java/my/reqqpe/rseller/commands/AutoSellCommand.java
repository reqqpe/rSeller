package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.configs.impl.MessageConfig;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.managers.ItemManager;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public class AutoSellCommand implements CommandExecutor {

    private final Main plugin;
    private final AutoSellManager autoSellManager;
    private final ItemManager itemManager;
    private final MessageConfig msg;

    public AutoSellCommand(Main plugin, AutoSellManager autoSellManager, ItemManager itemManager) {
        this.plugin = plugin;
        this.autoSellManager = autoSellManager;
        this.itemManager = itemManager;
        this.msg = plugin.getMessageConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!commandSender.hasPermission("rseller.autosell.command")) {
            commandSender.sendMessage(Colorizer.color(msg.getNoPermission()));
            return true;
        }

        if (args.length < 2) {
            commandSender.sendMessage(Colorizer.color(msg.getNoArguments()));
            return true;
        }


        String type = args[0];
        String id = args[1];

        switch (type.toLowerCase()) {
            case "category": {
                if (args.length >= 3) {
                    if (!commandSender.hasPermission("rseller.autosell.command.other")) {
                        commandSender.sendMessage(Colorizer.color(msg.getNoPermission()));
                        break;
                    }

                    String targetName = args[2];
                    Player target = Bukkit.getPlayer(targetName);
                    if (target == null || !target.isOnline()) {
                        commandSender.sendMessage(Colorizer.color(msg.getNotFoundPlayer()));
                        break;
                    }
                    switchStateCategory(target, id);
                } else {
                    if (!(commandSender instanceof Player)) {
                        commandSender.sendMessage(Colorizer.color(msg.getAutosellUsageAdmin()));
                        break;
                    }
                    Player player = (Player) commandSender;

                    if (!autoSellManager.hasCategory(id)) {
                        player.sendMessage(Colorizer.color(msg.getAutosellNotFoundCategory()
                                .replace("{id}", id)));
                        break;
                    }
                    switchStateCategory(player, id);
                    break;
                }
            }
            case "item": {
                if (args.length >= 3) {
                    if (!commandSender.hasPermission("rseller.autosell.command.other")) {
                        commandSender.sendMessage(Colorizer.color(msg.getNoPermission()));
                        return true;
                    }
                    String targetName = args[2];
                    Player target = Bukkit.getPlayer(targetName);
                    if (target == null || !target.isOnline()) {
                        commandSender.sendMessage(Colorizer.color(msg.getNotFoundPlayer()));
                        break;
                    }
                    switchStateItem(target, id);
                } else {
                    if (!(commandSender instanceof Player)) {
                        commandSender.sendMessage(Colorizer.color(msg.getAutosellUsageAdmin()));
                        break;
                    }
                    Player player = (Player) commandSender;

                    Item item = itemManager.getById(id);
                    if (item == null) {
                        player.sendMessage(Colorizer.color(msg.getAutosellNotFoundItem()
                                .replace("{id}", id)));
                        break;
                    }
                    switchStateItem(player, id);
                }
                break;
            }
            default: {
                if (commandSender.hasPermission("rseller.autosell.command.other")
                || commandSender.hasPermission("rseller.admin")) {
                    commandSender.sendMessage(Colorizer.color(msg.getAutosellUsageAdmin()));
                } else {
                    commandSender.sendMessage(Colorizer.color(msg.getAutosellUsageDef()));
                }
                break;
            }
        }

        return true;
    }

    private void switchStateCategory(Player player, String categoryId) {
        List<Item> categoryItems = autoSellManager.getCategoryItems(categoryId);

        boolean categoryState = getCategoryState(player, categoryId);
        PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());

        for (Item item : categoryItems) {
            playerData.setAutosell(item.id(), !categoryState);
        }


        String message = msg.getAutosellCategoryToggle();
        if (message == null || message.isEmpty()) return;

        String state = !categoryState
                ? plugin.getMessageConfig().getAutosellEnable()
                : plugin.getMessageConfig().getAutosellDisable();

        message = message
                .replace("{id}", categoryId)
                .replace("{state}", state);
        player.sendMessage(Colorizer.color(message));
    }

    private void switchStateItem(Player player, String itemId) {
        PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());

        boolean itemState = playerData.isAutosell(itemId);
        playerData.setAutosell(itemId, !itemState);

        String message = msg.getAutosellItemToggle();
        if (message == null || message.isEmpty()) return;

        String state = !itemState
                ? plugin.getMessageConfig().getAutosellEnable()
                : plugin.getMessageConfig().getAutosellDisable();

        message = message
                .replace("{id}", itemId)
                .replace("{state}", state);
        player.sendMessage(Colorizer.color(message));
    }




    private boolean getCategoryState(Player player, String categoryId) {
        List<Item> items = autoSellManager.getCategoryItems(categoryId);
        if (items.isEmpty()) return false;

        PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());

        for (Item item : items) {
            if (!playerData.isAutosell(item.id())) {
                return false;
            }
        }
        return true;
    }

}
