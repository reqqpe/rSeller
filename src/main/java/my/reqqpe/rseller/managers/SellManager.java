package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.EconomySetup;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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


        double finalPrice = price * Math.max(1.0, booster.getCoinMultiplier());
        double finalPoints = points * Math.max(1.0, booster.getPointMultiplier());

        return new SellResult(finalPrice, finalPoints);
    }

    public void sellItems(Player player, Inventory inv, List<Integer> sellSlots) {
        double totalCoins = 0;
        double totalPoints = 0;

        for (int slot : sellSlots) {
            ItemStack itemStack = inv.getItem(slot);
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;


            Item item = itemManager.searchItem(itemStack);
            if (item == null) continue;


            double price = item.getPrice();
            ;
            double points = item.getPoints();

            int amount = itemStack.getAmount();

            if (price > 0) {
                totalCoins += price * amount;
            }

            if (points > 0) {
                totalPoints += points * amount;
            }

            if (price > 0 || points > 0) {
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
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("messages");
        if (sellResult.coins > 0 || sellResult.points > 0) {

            String coinsFormat = numberFormatManager.format("messages.coins", sellResult.coins);
            String pointsFormat = numberFormatManager.format("messages.points", sellResult.points);

            String message = Colorizer.color(sec.getString("sell-items")
                    .replace("{coins}", coinsFormat)
                    .replace("{points}", pointsFormat));
            player.sendMessage(message);
        } else {
            String message = Colorizer.color(sec.getString("no-sell-items"));
            player.sendMessage(message);
        }
    }


    public void autoSell(Player player) {
        Inventory inv = player.getInventory();
        PlayerData data = database.getPlayerData(player.getUniqueId());

        double totalCoins = 0;
        double totalPoints = 0;


        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack itemstack = inv.getItem(i);
            if (itemstack == null || itemstack.getType() == Material.AIR) continue;

            Item item = itemManager.searchItem(itemstack);
            if (item == null) continue;

            if (!data.isAutosell(item.getId())) continue;

            double price = item.getPrice();
            double points = item.getPoints();

            int amount = itemstack.getAmount();

            if (price > 0) {
                totalCoins += price * amount;
            }

            if (points > 0) {
                totalPoints += points * amount;
            }
            if (price > 0 || points > 0) {
                inv.setItem(i, null);
            }
        }

        Booster booster = plugin.getBoosterManager().getBoosterByPlayer(player);
        if (booster == null) {
            booster = new Booster(1.0, 1.0);
        }

        totalCoins *= Math.max(1.0, booster.getCoinMultiplier());
        totalPoints *= Math.max(1.0, booster.getPointMultiplier());

        if (totalCoins > 0) economy.depositPlayer(player, totalCoins);
        if (totalPoints > 0) data.addPoints(totalPoints);

        if (totalCoins > 0 || totalPoints > 0) {

            String coinsFormat = numberFormatManager.format("messages.coins", totalCoins);
            String pointsFormat = numberFormatManager.format("messages.points", totalPoints);

            String msg = Colorizer.color(plugin.getConfig().getString("messages.auto-sell")
                    .replace("{coins}", coinsFormat)
                    .replace("{points}", pointsFormat));
            player.sendMessage(msg);
        }
    }

    public SellResult calculateSellPreview(Player player, Inventory inv, List<Integer> sellSlots) {


        double totalCoins = 0;
        double totalPoints = 0;

        for (int slot : sellSlots) {
            ItemStack itemStack = inv.getItem(slot);
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;


            Item item = itemManager.searchItem(itemStack);
            if (item == null) continue;

            double price = item.getPrice();
            double points = item.getPoints();

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
