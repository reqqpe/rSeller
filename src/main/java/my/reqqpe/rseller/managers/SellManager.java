package my.reqqpe.rseller.managers;

import it.unimi.dsi.fastutil.ints.IntList;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.configs.impl.MainConfig;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.economy.EconomyProvider;
import my.reqqpe.rseller.events.PointsUpdateEvent;
import my.reqqpe.rseller.events.SellEvent;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;


public class SellManager {

    private final Main plugin;
    private final EconomyProvider economy;
    private final NumberFormatManager numberFormatManager;
    private final ItemManager itemManager;

    public SellManager(Main plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy();
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

            Item item = itemManager.search(itemStack);
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
            economy.deposit(player, sellResult.coins);
        }
        if (sellResult.points > 0) {
            PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());
            playerData.addPoints(sellResult.points);
            Bukkit.getPluginManager().callEvent(new PointsUpdateEvent(player, sellResult.points, false, "add"));
        }

        String msgSellItems = plugin.getMessageConfig().getSellItems();
        String msgNoSellItems = plugin.getMessageConfig().getNoSellItems();
        if (sellResult.coins > 0 || sellResult.points > 0) {

            String coinsFormat = numberFormatManager.format("messages.coins", sellResult.coins);
            String pointsFormat = numberFormatManager.format("messages.points", sellResult.points);

            String message = Colorizer.color(msgSellItems
                    .replace("{coins}", coinsFormat)
                    .replace("{points}", pointsFormat)
                    .replace("{amount}", String.valueOf(finalAmount)));
            player.sendMessage(message);

            MainConfig.SoundsSection sounds = plugin.getMainConfig().getSounds();
            plugin.playSound(player, sounds.getSell(), sounds.getSellVolume(), sounds.getSellPitch());

            Bukkit.getPluginManager().callEvent(new SellEvent(player, sellItems));

        } else {
            player.sendMessage(Colorizer.color(msgNoSellItems));

            MainConfig.SoundsSection sounds = plugin.getMainConfig().getSounds();
            plugin.playSound(player, sounds.getNoSell(), sounds.getNoSellVolume(), sounds.getNoSellPitch());
        }
    }

    public SellResult calculateSellPreview(Player player, Inventory inv, IntList sellSlots) {


        double totalCoins = 0;
        double totalPoints = 0;

        for (int slot : sellSlots) {
            ItemStack itemStack = inv.getItem(slot);
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;

            Item item = itemManager.search(itemStack);
            if (item == null) continue;

            double price = item.price();
            double points = item.points();

            if (price <= 0 && points <= 0) continue;
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
