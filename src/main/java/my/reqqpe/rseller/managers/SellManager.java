package my.reqqpe.rseller.managers;

import lombok.Getter;
import my.reqqpe.rseller.EconomySetup;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.model.ItemData;
import my.reqqpe.rseller.utils.Colorizer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SellManager {

    private final Main plugin;
    private final Economy economy;
    private final Database database;
    private final LevelManager levelManager;
    private final ItemManager itemManager;

    public SellManager(Main plugin, Database database) {
        this.plugin = plugin;
        this.economy = EconomySetup.getEconomy();
        this.database = database;
        this.levelManager = plugin.getLevelManager();
        this.itemManager = plugin.getItemManager();
    }

    public void sellItems(Player player, Inventory inv, List<Integer> sellSlots) {
        SellPrice price = calculateAndRemoveItems(player, inv, sellSlots);

        if (price.getTotalCoins() > 0) economy.depositPlayer(player, price.getTotalCoins());
        if (price.getTotalPoints() > 0) {
            database.getPlayerData(player.getUniqueId()).addPoints(price.getTotalPoints());
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("messages");
        ConfigurationSection formats = plugin.getConfig().getConfigurationSection("numbers_format.messages");

        if (price.getTotalCoins() > 0 || price.getTotalPoints() > 0) {
            String msg = Colorizer.color(sec.getString("sell-items")
                    .replace("{coins}", String.format(Objects.requireNonNull(formats.getString("coins")), price.getTotalCoins()))
                    .replace("{points}", String.format(Objects.requireNonNull(formats.getString("points")), price.getTotalPoints())));
            player.sendMessage(msg);
        } else {
            player.sendMessage(Colorizer.color(sec.getString("no-sell-items")));
        }
    }

    public SellPrice calculateCostPreview(Player player, Inventory inv, List<Integer> sellSlots) {
        double totalCoins = 0;
        double totalPoints = 0;

        for (int slot : sellSlots) {
            ItemStack itemStack = inv.getItem(slot);
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;

            ItemData item = itemManager.getByItemStack(itemStack);
            if (item == null) continue;

            int amount = itemStack.getAmount();
            totalCoins += item.getPrice() * amount;
            totalPoints += item.getPoints() * amount;
        }

        return new SellPrice(
                totalCoins * levelManager.getCoinMultiplier(player),
                totalPoints * levelManager.getPointMultiplier(player)
        );
    }

    public void autoSell(Player player) {
        PlayerInventory inv = player.getInventory();
        PlayerData data = database.getPlayerData(player.getUniqueId());

        double totalCoins = 0;
        double totalPoints = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) continue;

            ItemData item = itemManager.getByItemStack(stack);
            if (item == null || !data.isAutosell(item.getId())) continue;

            int amount = stack.getAmount();
            totalCoins += item.getPrice() * amount;
            totalPoints += item.getPoints() * amount;

            inv.setItem(i, null);
        }

        if (totalCoins > 0) economy.depositPlayer(player, totalCoins);
        if (totalPoints > 0) data.addPoints(totalPoints);

        if (totalCoins > 0 || totalPoints > 0) {
            ConfigurationSection formats = plugin.getConfig().getConfigurationSection("numbers_format.messages");
            String msg = Colorizer.color(plugin.getConfig().getString("messages.auto-sell")
                    .replace("{coins}", String.format(formats.getString("coins"), totalCoins))
                    .replace("{points}", String.format(formats.getString("points"), totalPoints)));
            player.sendMessage(msg);
        }
    }

    public SellPrice calculateAndRemoveItems(Player player, Inventory inv, List<Integer> sellSlots) {
        double totalCoins = 0;
        double totalPoints = 0;

        for (int slot : sellSlots) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;

            ItemData item = itemManager.getByItemStack(stack);
            if (item == null) continue;

            int amount = stack.getAmount();
            totalCoins += item.getPrice() * amount;
            totalPoints += item.getPoints() * amount;

            inv.setItem(slot, null);
        }

        return new SellPrice(
                totalCoins * levelManager.getCoinMultiplier(player),
                totalPoints * levelManager.getPointMultiplier(player)
        );
    }

    public List<Integer> getFilledSlots(Player player) {
        List<Integer> result = new ArrayList<>();
        PlayerInventory inv = player.getInventory();

        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                result.add(i);
            }
        }

        return result;
    }

    public static class SellPrice {
        @Getter
        private final double totalCoins;
        @Getter
        private final double totalPoints;

        public SellPrice(double coins, double points) {
            this.totalCoins = coins;
            this.totalPoints = points;
        }
    }
}
