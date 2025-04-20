package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.EconomySetup;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.configurations.DataBase;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.sql.SQLException;
import java.util.List;

public class SellManager {

    private final Main plugin;
    private final Economy economy;
    private final DataBase dataBase;
    private final LevelManager levelManager;

    public SellManager(Main plugin) {
        this.plugin = plugin;
        this.economy = EconomySetup.getEconomy();
        this.dataBase = plugin.getDataBase();
        this.levelManager = plugin.getLevelManager();
    }

    public void sellItems(Player player, Inventory inv, List<Integer> sellSlots) {
        FileConfiguration itemConfig = plugin.getItemsConfig().getConfig();

        double totalCoins = 0;
        int totalPoints = 0;

        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            String key = "items." + item.getType().name();
            if (!itemConfig.contains(key)) continue;

            double price = itemConfig.getDouble(key + ".price", 0);
            int points = itemConfig.getInt(key + ".points", 0);

            int amount = item.getAmount();

            totalCoins += price * amount;
            totalPoints += points * amount;

            inv.setItem(slot, null);
        }

        // Применение бустеров
        LevelManager.LevelInfo levelInfo = levelManager.getLevelInfo(player);

        double coinBoost = levelInfo.coinMultiplier();
        double pointBoost = levelInfo.pointMultiplier();

        totalCoins *= coinBoost;
        totalPoints *= pointBoost;

        if (totalCoins > 0) {
            economy.depositPlayer(player, totalCoins);
        }

        if (totalPoints > 0) {
            try {
                dataBase.addPoints(player, totalPoints);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if (totalCoins > 0 || totalPoints > 0) {
            player.sendMessage(ChatColor.GREEN + "Вы продали вещи на сумму " +
                    ChatColor.GOLD + totalCoins + ChatColor.GREEN +
                    " и получили " + ChatColor.YELLOW + totalPoints + ChatColor.GREEN + " очков!");
        } else {
            player.sendMessage(ChatColor.RED + "Нет предметов, которые можно продать.");
        }
    }
}
