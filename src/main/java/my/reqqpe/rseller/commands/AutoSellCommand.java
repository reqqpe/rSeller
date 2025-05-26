package my.reqqpe.rseller.commands;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.menu.AutoSellMenu;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class AutoSellCommand implements CommandExecutor {

    private final Main plugin;
    private final Database database;
    private final AutoSellMenu autoSellMenu;

    public AutoSellCommand(Main plugin, Database database, AutoSellMenu autoSellMenu) {
        this.plugin = plugin;
        this.database = database;
        this.autoSellMenu = autoSellMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection sec = config.getConfigurationSection("messages");
        if (!sender.hasPermission("rSeller.autosell")) {
            sender.sendMessage(Colorizer.color(sec.getString("no-permission")));
        }

        if (!(sender instanceof Player player)) { return true; }

        if (args.length != 1) {
            player.sendMessage(Colorizer.color(sec.getString("usage-auto-sell")));
            return true;
        }

        Material material;
        try {
            material = Material.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Colorizer.color(sec.getString("not-found-material")));
            return true;
        }

        if (!plugin.getAutoSellManager().isSellable(material)) {
            player.sendMessage(Colorizer.color(sec.getString("not-item-in-conf")));
            return true;
        }

        PlayerData data = database.getPlayerData(player.getUniqueId());

        boolean newState = !data.isAutosell(material);
        data.setAutosell(material, newState);

        String enabled = Colorizer.color(sec.getString("autosell-enable"));
        String disable = Colorizer.color(sec.getString("autosell-disable"));

        String status = newState ? enabled : disable;
        player.sendMessage(Colorizer.color(sec.getString("autosell-message")
                .replace("{material}", material.name())
                .replace("{status}", status)));
        return true;
    }
}

