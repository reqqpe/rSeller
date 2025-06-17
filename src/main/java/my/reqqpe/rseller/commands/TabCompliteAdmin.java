package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.managers.ItemManager;
import my.reqqpe.rseller.model.ItemData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TabCompliteAdmin implements TabCompleter {

    private final Main plugin;
    private final ItemManager itemManager;

    public TabCompliteAdmin(Main plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("rseller.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "points", "createitem", "deleteitem").stream()
                    .filter(arg -> arg.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("points")) {
            if (args.length == 2) {
                return Arrays.asList("add", "remove", "set").stream()
                        .filter(arg -> arg.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                return Arrays.asList("10.0", "50.0", "100.0").stream()
                        .filter(arg -> arg.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
            if (args.length == 4) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(p -> p.getName())
                        .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args[0].equalsIgnoreCase("createitem")) {
            if (args.length == 2) {
                return new ArrayList<>();
            }
            if (args.length == 3) {
                return Arrays.asList("10.0", "20.0", "50.0").stream()
                        .filter(arg -> arg.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
            if (args.length == 4) {
                return Arrays.asList("0.1", "1.0", "5.0").stream()
                        .filter(arg -> arg.startsWith(args[3]))
                        .collect(Collectors.toList());
            }
        }

        if (args[0].equalsIgnoreCase("deleteitem")) {
            if (args.length == 2) {
                return itemManager.getAllItems().stream()
                        .map(ItemData::getId)
                        .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}