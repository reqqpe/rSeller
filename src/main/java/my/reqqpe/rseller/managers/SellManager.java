package my.reqqpe.rseller.managers;

import it.unimi.dsi.fastutil.ints.IntList;
import my.reqqpe.rseller.EconomySetup;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.events.PointsUpdateEvent;
import my.reqqpe.rseller.events.SellEvent;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;


public class SellManager {

    private final Main plugin;
    private final Economy economy;
    private final Database database;
    private final NumberFormatManager numberFormatManager;
    private final ItemManager itemManager;

    public SellManager(Main plugin, Database database) {
        this.plugin = plugin;
        this.economy = EconomySetup.getEconomy();
        this.database = database;
        this.numberFormatManager = plugin.getFormatManager();
        this.itemManager = plugin.getItemManager();
    }

    public SellResult sellResult(Player player, double price, double points) {
        Booster booster = plugin.getBoosterManager().getBoosterByPlayer(player);
        if (booster == null) {
            booster = new Booster(1.0, 1.0);
        }


        double finalPrice = price * Math.max(1.0, booster.coinMultiplier());
        double finalPoints = points * Math.max(1.0, booster.pointMultiplier());

        return new SellResult(finalPrice, finalPoints);
    }


    public void sellItems(Player player, Inventory inv, IntList sellSlots) {
        double totalCoins = 0;
        double totalPoints = 0;

        int finalAmount = 0;
        List<Item> sellItems = new ArrayList<>();

        for (int slot : sellSlots) {
            ItemStack itemStack = inv.getItem(slot);
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;


            Item item = itemManager.searchItem(itemStack);
            if (item == null) continue;


            double price = item.price();
            double points = item.points();

            int amount = itemStack.getAmount();

            if (price > 0) {
                totalCoins += price * amount;
            }

            if (points > 0) {
                totalPoints += points * amount;
            }
            
            if (price > 0 || points > 0) {
                finalAmount += amount;
                sellItems.add(item);
                inv.setItem(slot, null);
            }
        }

        SellResult sellResult = sellResult(player, totalCoins, totalPoints);

        if (sellResult.coins > 0) {
            economy.depositPlayer(player, sellResult.coins);
        }
        if (sellResult.points > 0) {
            PlayerData playerData = database.getPlayerData(player.getUniqueId());
            playerData.addPoints(sellResult.points);
            Bukkit.getPluginManager().callEvent(new PointsUpdateEvent(player, sellResult.points, false, "add", database));
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("messages");
        if (sellResult.coins > 0 || sellResult.points > 0) {

            String coinsFormat = numberFormatManager.format("messages.coins", sellResult.coins);
            String pointsFormat = numberFormatManager.format("messages.points", sellResult.points);

            String message = Colorizer.color(sec.getString("sell-items")
                    .replace("{coins}", coinsFormat)
                    .replace("{points}", pointsFormat)
                    .replace("{amount}", String.valueOf(finalAmount)));
            player.sendMessage(message);

            Bukkit.getPluginManager().callEvent(new SellEvent(player, sellItems, database));

        } else {
            String message = Colorizer.color(sec.getString("no-sell-items"));
            player.sendMessage(message);
        }
    }

    public SellResult calculateSellPreview(Player player, Inventory inv, IntList sellSlots) {


        double totalCoins = 0;
        double totalPoints = 0;

        for (int slot : sellSlots) {
            ItemStack itemStack = inv.getItem(slot);
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;


            Item item = itemManager.searchItem(itemStack);
            if (item == null) continue;

            double price = item.price();
            double points = item.points();

            if (price <= 0 && points <= 0) {
                continue;
            }
            int amount = itemStack.getAmount();

            if (price > 0) {
                totalCoins += price * amount;
            }

            if (points > 0) {
                totalPoints += points * amount;
            }
        }

        return sellResult(player, totalCoins, totalPoints);
    }

    public record SellResult(double coins, double points) {
    }
}
