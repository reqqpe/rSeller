package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.EconomySetup;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.utils.Colorizer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.List;
import java.util.Objects;

public class SellManager {

    private final Main plugin;
    private final Economy economy;
    private final Database database;
    private final LevelManager levelManager;

    public SellManager(Main plugin, Database database) {
        this.plugin = plugin;
        this.economy = EconomySetup.getEconomy();
        this.database = database;
        this.levelManager = plugin.getLevelManager();
    }

    public void sellItems(Player player, Inventory inv, List<Integer> sellSlots) {
        FileConfiguration itemConfig = plugin.getItemsConfig().getConfig();

        double totalCoins = 0;
        double totalPoints = 0;

        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            String key = "items." + item.getType().name();
            if (!itemConfig.contains(key)) continue;

            double price = itemConfig.getDouble(key + ".price", 0);
            double points = itemConfig.getDouble(key + ".points", 0);

            int amount = item.getAmount();

            totalCoins += price * amount;
            totalPoints += points * amount;

            inv.setItem(slot, null);
        }
        // применяю бусты
        LevelManager.LevelInfo levelInfo = levelManager.getLevelInfo(player);

        double coinBoost = levelInfo.coinMultiplier();
        double pointBoost = levelInfo.pointMultiplier();

        totalCoins *= coinBoost;
        totalPoints *= pointBoost;

        if (totalCoins > 0) {
            economy.depositPlayer(player, totalCoins);
        }

        if (totalPoints > 0) {
            PlayerData data = database.getPlayerData(player.getUniqueId());
            data.addPoints(totalPoints);
        }
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("messages");
        if (totalCoins > 0 || totalPoints > 0) {
            ConfigurationSection formats = plugin.getConfig().getConfigurationSection("numbers_format.messages");
            String message = Colorizer.color(sec.getString("sell-items")
                    .replace("{coins}", String.format(Objects.requireNonNull(formats.getString("coins")), totalCoins))
                    .replace("{points}", String.format(Objects.requireNonNull(formats.getString("points")), totalPoints)));
            player.sendMessage(message);
        } else {
            String message = Colorizer.color(sec.getString("no-sell-items"));
            player.sendMessage(message);
        }
    }

    public void autoSell(Player player) {
        FileConfiguration itemConfig = plugin.getItemsConfig().getConfig();
        Inventory inv = player.getInventory();
        PlayerData data = database.getPlayerData(player.getUniqueId());

        double totalCoins = 0;
        double totalPoints = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            Material mat = item.getType();
            if (!data.isAutosell(mat)) continue;

            String key = "items." + mat.name();
            if (!itemConfig.contains(key)) continue;

            double price = itemConfig.getDouble(key + ".price", 0);
            double points = itemConfig.getDouble(key + ".points", 0);
            int amount = item.getAmount();

            totalCoins += price * amount;
            totalPoints += points * amount;

            inv.setItem(i, null);
        }

        // применяю бусты
        LevelManager.LevelInfo levelInfo = levelManager.getLevelInfo(player);
        totalCoins *= levelInfo.coinMultiplier();
        totalPoints *= levelInfo.pointMultiplier();

        if (totalCoins > 0) economy.depositPlayer(player, totalCoins);
        if (totalPoints > 0) data.addPoints(totalPoints);

        if (totalCoins > 0 || totalPoints > 0) {
            ConfigurationSection formats = plugin.getConfig().getConfigurationSection("numbers_format.messages");
            String msg = Colorizer.color(plugin.getConfig().getString("messages.auto-sell")
                    .replace("{coins}", String.format(Objects.requireNonNull(formats.getString("coins")), totalCoins))
                    .replace("{points}", String.format(Objects.requireNonNull(formats.getString("points")), totalPoints)));
            player.sendMessage(msg);
        }
    }
}
