package my.reqqpe.rseller;

import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.NumberFormatManager;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class AutoSellTask {

    private final long delay;
    private final Main plugin;
    private final Database db;
    private final Economy economy;
    private final NumberFormatManager numberFormatManager;

    public AutoSellTask(long delay, Main plugin, Database db, NumberFormatManager numberFormatManager) {
        this.delay = delay;
        this.plugin = plugin;
        this.db = db;
        this.economy = EconomySetup.getEconomy();
        this.numberFormatManager = numberFormatManager;
    }


    public void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.isBlockedWorld(player.getWorld().getName())) return;
                    if (!player.hasPermission("rseller.autosell")) return;

                    PlayerData playerData = db.getPlayerData(player.getUniqueId());
                    if (playerData == null) return;
                    if (!playerData.hasEnabledAutosell()) return;

                    PlayerInventory inv = player.getInventory();
                    for (int slot = 0; slot < inv.getSize(); slot++) {
                        ItemStack itemStack = inv.getItem(slot);
                        if (itemStack == null) continue;

                        if (sellItem(player, itemStack)) {
                            inv.setItem(slot, null);
                        }
                    }


                }
            }
        }.runTaskTimer(plugin, 0L, delay * 20L);
    }


    public boolean sellItem(Player player, ItemStack itemStack) {
        Item item = plugin.getItemManager().searchItem(itemStack);
        if (item == null) return false;

        PlayerData playerData = db.getPlayerData(player.getUniqueId());
        if (!playerData.isAutosell(item.id())) return false;

        double price = item.price();
        double points = item.points();

        if (price <= 0 || points <= 0) return false;

        int amount = itemStack.getAmount();

        price *= amount;
        points *= amount;

        Booster booster = plugin.getBoosterManager().getBoosterByPlayer(player);
        if (booster == null) {
            booster = new Booster(1.0, 1.0);
        }

        double totalCoins = price * Math.max(1.0, booster.coinMultiplier());
        double totalPoints = points * Math.max(1.0, booster.pointMultiplier());

        if (totalCoins > 0) economy.depositPlayer(player, totalCoins);
        if (totalPoints > 0) playerData.addPoints(totalPoints);

        if (totalCoins > 0 || totalPoints > 0) {
            String coinsFormat = numberFormatManager.format("messages.coins", totalCoins);
            String pointsFormat = numberFormatManager.format("messages.points", totalPoints);

            String msg = Colorizer.color(plugin.getConfig().getString("messages.auto-sell")
                    .replace("{coins}", coinsFormat)
                    .replace("{points}", pointsFormat)
                    .replace("{item_name}", item.getDisplayName(plugin))
                    .replace("{amount}", String.valueOf(amount)));
            player.sendMessage(msg);
        }

        return true;
    }
}
