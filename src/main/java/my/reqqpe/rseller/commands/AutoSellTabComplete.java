package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.managers.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoSellTabComplete implements TabCompleter {

    private final AutoSellManager autoSellManager;
    private final ItemManager itemManager;

    public AutoSellTabComplete(AutoSellManager autoSellManager, ItemManager itemManager) {
        this.autoSellManager = autoSellManager;
        this.itemManager = itemManager;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if (!sender.hasPermission("rseller.autosell.command")) {
            return List.of();
        }

        if (args.length == 1) {
            return Stream.of("category", "item")
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("category")) {
            return autoSellManager.getCategories().keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("item")) {
            return itemManager.getAll().stream()
                    .map(item -> item.id())
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && sender.hasPermission("rseller.autosell.command.other")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(HumanEntity::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}