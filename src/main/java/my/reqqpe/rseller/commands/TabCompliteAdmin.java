package my.reqqpe.rseller.commands;

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
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if (!sender.hasPermission("rseller.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "points").stream()
                    .filter(arg -> arg.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }


        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            return Arrays.asList("items", "config", "guis").stream()
                    .filter(arg -> arg.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("points")) {
            return Arrays.asList("add", "remove", "set").stream()
                    .filter(arg -> arg.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("points")) {
            return Arrays.asList("10", "50", "100").stream()
                    .filter(arg -> arg.startsWith(args[2]))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("points")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}