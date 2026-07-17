package my.reqqpe.rseller.managers;

import it.unimi.dsi.fastutil.ints.IntList;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.cache.SellDataCache;
import my.reqqpe.rseller.configs.impl.MainConfig;
import my.reqqpe.rseller.database.repositories.SellLogRepository;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.economy.EconomyProvider;
import my.reqqpe.rseller.events.PointsUpdateEvent;
import my.reqqpe.rseller.events.SellEvent;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.SellData;
import my.reqqpe.rseller.models.SellLog;
import my.reqqpe.rseller.models.SellSource;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


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


    public void menuSellItems(Player player, Inventory inv, IntList sellSlots) {
        double totalCoins = 0;
        double totalPoints = 0;

        int finalAmount = 0;
        List<Item> sellItems = new ArrayList<>();

        Map<String, long[]> logAccum = new LinkedHashMap<>();

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

                // accumulate per item-id for logging
                logAccum.computeIfAbsent(item.id(), k -> new long[]{0, 0, 0});
                long[] acc = logAccum.get(item.id());
                acc[0] += amount;
                acc[1] += (long) (price * amount * 1_000_000);
                acc[2] += (long) (points * amount * 1_000_000);
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

            Bukkit.getPluginManager().callEvent(new SellEvent(player, sellItems, sellResult.coins, sellResult.points, finalAmount, SellSource.GUI));

            SellLogRepository repo = plugin.getSellLogRepository();
            if (repo != null && !logAccum.isEmpty()) {
                double coinRatio = totalCoins > 0 ? sellResult.coins / totalCoins : 1.0;
                double pointRatio = totalPoints > 0 ? sellResult.points / totalPoints : 1.0;

                int playerLevel = plugin.getLevelManager().getLevel(player);
                double coinBooster = plugin.getBoosterManager().getBoosterByPlayer(player).coinMultiplier();
                double pointBooster = plugin.getBoosterManager().getBoosterByPlayer(player).pointMultiplier();
                String worldName = player.getWorld().getName();

                List<SellLog> logs = new ArrayList<>();
                for (Map.Entry<String, long[]> entry : logAccum.entrySet()) {
                    long[] acc = entry.getValue();
                    int amount = (int) acc[0];
                    double rawCoins = acc[1] / 1_000_000.0;
                    double rawPoints = acc[2] / 1_000_000.0;
                    logs.add(SellLog.of(
                            player.getUniqueId(),
                            player.getName(),
                            entry.getKey(),
                            amount,
                            rawCoins * coinRatio,
                            rawPoints * pointRatio,
                            SellSource.GUI,
                            playerLevel,
                            coinBooster,
                            pointBooster,
                            worldName
                    ));
                }
                repo.logBatchAsync(logs);
            }

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

    public boolean autoSellItem(Player player, ItemStack itemStack) {
        Item item = plugin.getItemManager().search(itemStack);
        if (item == null) return false;

        PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());
        if (!playerData.isAutosell(item.id())) return false;

        double price = item.price();
        double points = item.points();

        if (price <= 0 && points <= 0) return false;

        int amount = itemStack.getAmount();

        price *= amount;
        points *= amount;

        Booster booster = plugin.getBoosterManager().getBoosterByPlayer(player);
        if (booster == null) {
            booster = new Booster(1.0, 1.0);
        }

        double totalCoins = price * Math.max(1.0, booster.coinMultiplier());
        double totalPoints = points * Math.max(1.0, booster.pointMultiplier());

        if (totalCoins > 0) economy.deposit(player, totalCoins);
        if (totalPoints > 0) playerData.addPoints(totalPoints);

        if (totalCoins > 0 || totalPoints > 0) {
            SellLogRepository repo = plugin.getSellLogRepository();
            if (repo != null) {
                repo.logAsync(SellLog.of(
                        player.getUniqueId(),
                        player.getName(),
                        item.id(),
                        amount,
                        totalCoins,
                        totalPoints,
                        SellSource.AUTOSELL,
                        plugin.getLevelManager().getLevel(player),
                        booster.coinMultiplier(),
                        booster.pointMultiplier(),
                        player.getWorld().getName()
                ));
            }

            if (playerData.isAutosellMessage()) {

                String typeMessage = plugin.getMainConfig().getAutosell().getTypeMessage();
                if (typeMessage.equalsIgnoreCase("task")) {
                    SellData sellData = SellDataCache.getOrCreate(player.getUniqueId());
                    sellData.addMoney(totalCoins);
                    sellData.addPoints(totalPoints);
                    sellData.addCount(amount);
                } else {

                    MainConfig.SoundsSection sounds = plugin.getMainConfig().getSounds();
                    plugin.playSound(player, sounds.getAutosell(), sounds.getAutosellVolume(), sounds.getAutosellPitch());

                    String coinsFormat = numberFormatManager.format("messages.coins", totalCoins);
                    String pointsFormat = numberFormatManager.format("messages.points", totalPoints);

                    String msg = Colorizer.color(plugin.getMessageConfig().getAutoSell()
                            .replace("{coins}", coinsFormat)
                            .replace("{points}", pointsFormat)
                            .replace("{item_name}", item.getDisplayName(plugin))
                            .replace("{amount}", String.valueOf(amount)));
                    if (msg != null && !msg.isEmpty()) {
                        String sendType = plugin.getMainConfig().getAutosell().getSendType();
                        if (sendType.equalsIgnoreCase("actionbar")) {
                            player.sendActionBar(msg);
                        } else {
                            player.sendMessage(msg);
                        }

                    }
                }
            }
        }

        return true;
    }


    public boolean sellItem(Player player, String id, int amount) {
        Inventory inv = player.getInventory();

        if (amount != Integer.MAX_VALUE) {
            int available = 0;
            for (int slot = 0; slot < inv.getSize(); slot++) {
                ItemStack stack = inv.getItem(slot);
                if (stack == null || stack.getType() == Material.AIR) continue;
                Item item = itemManager.search(stack);
                if (item == null || !item.id().equalsIgnoreCase(id)) continue;
                available += stack.getAmount();
            }
            if (available < amount) {
                String msg = Colorizer.color(plugin.getMessageConfig().getNotEnoughSellItems()
                        .replace("{required}",  String.valueOf(amount))
                        .replace("{available}", String.valueOf(available)));
                player.sendMessage(msg);
                MainConfig.SoundsSection sounds = plugin.getMainConfig().getSounds();
                plugin.playSound(player, sounds.getNoSell(), sounds.getNoSellVolume(), sounds.getNoSellPitch());
                return false;
            }
        }

        int left = amount;
        double coins = 0;
        double points = 0;
        int sold = 0;
        List<Item> sellItems = new ArrayList<>();

        for (int slot = 0; slot < inv.getSize() && left > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR) continue;

            Item item = itemManager.search(stack);
            if (item == null || !item.id().equalsIgnoreCase(id)) continue;

            int sell = Math.min(stack.getAmount(), left);

            if (item.price() > 0) {
                coins += item.price() * sell;
            }

            if (item.points() > 0) {
                points += item.points() * sell;
            }

            if (stack.getAmount() == sell) {
                inv.setItem(slot, null);
            } else {
                stack.setAmount(stack.getAmount() - sell);
            }

            left -= sell;
            sold += sell;
            sellItems.add(item);
        }

        if (sold == 0) {
            player.sendMessage(Colorizer.color(plugin.getMessageConfig().getNoSellItems()));

            MainConfig.SoundsSection sounds = plugin.getMainConfig().getSounds();
            plugin.playSound(player, sounds.getNoSell(), sounds.getNoSellVolume(), sounds.getNoSellPitch());

            return false;
        }

        SellResult result = sellResult(player, coins, points);

        if (result.coins() > 0) {
            economy.deposit(player, result.coins());
        }

        if (result.points() > 0) {
            PlayerData playerData = PlayerDataCache.getOrCreate(player.getUniqueId());
            playerData.addPoints(result.points());

            Bukkit.getPluginManager().callEvent(
                    new PointsUpdateEvent(player, result.points(), false, "add")
            );
        }

        String coinsFormat = numberFormatManager.format("messages.coins", result.coins());
        String pointsFormat = numberFormatManager.format("messages.points", result.points());

        String message = Colorizer.color(plugin.getMessageConfig().getSellItems()
                .replace("{coins}", coinsFormat)
                .replace("{points}", pointsFormat)
                .replace("{amount}", String.valueOf(sold)));

        player.sendMessage(message);

        MainConfig.SoundsSection sounds = plugin.getMainConfig().getSounds();
        plugin.playSound(player, sounds.getSell(), sounds.getSellVolume(), sounds.getSellPitch());

        Bukkit.getPluginManager().callEvent(new SellEvent(player, sellItems, result.coins(), result.points(), sold, SellSource.MENU_ACTION));

        SellLogRepository repo = plugin.getSellLogRepository();
        if (repo != null) {
            repo.logAsync(SellLog.of(
                    player.getUniqueId(),
                    player.getName(),
                    id,
                    sold,
                    result.coins(),
                    result.points(),
                    SellSource.MENU_ACTION,
                    plugin.getLevelManager().getLevel(player),
                    plugin.getBoosterManager().getBoosterByPlayer(player).coinMultiplier(),
                    plugin.getBoosterManager().getBoosterByPlayer(player).pointMultiplier(),
                    player.getWorld().getName()
            ));
        }

        return true;
    }

    public record SellResult(double coins, double points) {
    }
}
