package my.reqqpe.rseller.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import my.reqqpe.rseller.models.item.MaterialCategory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TabCompleteAdmin implements TabCompleter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if (!sender.hasPermission("rseller.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return Stream.of("reload", "points", "customitem", "logs", "migrate", "generate", "items")
                    .filter(arg -> arg.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("logs")) {
            if (args.length == 2) {
                String today = LocalDate.now().format(DATE_FMT);
                List<String> suggestions = new ArrayList<>();

                String playerPrefix = "player:";
                if (args[1].toLowerCase().startsWith(playerPrefix)) {
                    String partial = args[1].substring(playerPrefix.length());
                    suggestions.add("player:me");
                    Bukkit.getOnlinePlayers().stream()
                            .map(HumanEntity::getName)
                            .filter(n -> n.toLowerCase().startsWith(partial.toLowerCase()))
                            .map(n -> "player:" + n)
                            .forEach(suggestions::add);
                } else {
                    suggestions.add("player:me");
                    Bukkit.getOnlinePlayers().stream()
                            .map(n -> "player:" + n.getName())
                            .forEach(suggestions::add);
                    suggestions.addAll(List.of(
                            "from:" + today, "to:" + today,
                            "sort:date", "sort:coins", "sort:points", "sort:amount",
                            "order:desc", "order:asc"
                    ));
                }

                return suggestions.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args.length >= 3) {
                String current = args[args.length - 1];
                String today = LocalDate.now().format(DATE_FMT);

                boolean hasPlayer = Stream.of(args).anyMatch(a -> a.toLowerCase().startsWith("player:"));
                boolean hasFrom = Stream.of(args).anyMatch(a -> a.toLowerCase().startsWith("from:"));
                boolean hasTo = Stream.of(args).anyMatch(a -> a.toLowerCase().startsWith("to:"));
                boolean hasSort = Stream.of(args).anyMatch(a -> a.toLowerCase().startsWith("sort:"));
                boolean hasOrder = Stream.of(args).anyMatch(a -> a.toLowerCase().startsWith("order:"));

                List<String> suggestions = new ArrayList<>();
                if (!hasPlayer) {
                    suggestions.add("player:me");
                    Bukkit.getOnlinePlayers().stream()
                            .map(n -> "player:" + n.getName())
                            .forEach(suggestions::add);
                }
                if (!hasFrom)  suggestions.add("from:" + today);
                if (!hasTo)    suggestions.add("to:"   + today);
                if (!hasSort)  suggestions.addAll(List.of("sort:date","sort:coins","sort:points","sort:amount"));
                if (!hasOrder) suggestions.addAll(List.of("order:desc","order:asc"));

                return suggestions.stream()
                        .filter(s -> s.toLowerCase().startsWith(current.toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args[0].equalsIgnoreCase("generate")) {
            if (args.length == 2) {
                return MaterialCategory.NAMES.stream()
                        .filter(c -> c.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 3) return List.of("1", "10", "50", "100");
            if (args.length == 4) return List.of("10", "50", "100", "500");
            if (args.length == 5) return List.of("0", "0.5", "1");
            if (args.length == 6) return List.of("0", "1", "5");
            if (args.length == 7) return Stream.of("round", "overwrite:false", "overwrite:true")
                    .filter(s -> s.startsWith(args[6].toLowerCase()))
                    .collect(Collectors.toList());
            if (args.length == 8) return Stream.of("overwrite:false", "overwrite:true")
                    .filter(s -> s.startsWith(args[7].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            return Stream.of("items", "config", "guis")
                    .filter(arg -> arg.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("customitem")) {
            return Stream.of("create", "remove")
                    .filter(arg -> arg.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("points")) {
            return Stream.of("add", "remove", "set")
                    .filter(arg -> arg.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("points")) {
            return Stream.of("10", "50", "100")
                    .filter(arg -> arg.startsWith(args[2]))
                    .collect(Collectors.toList());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("points")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(HumanEntity::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }
}