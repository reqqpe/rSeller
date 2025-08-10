package my.reqqpe.rseller.listeners;

import my.reqqpe.rseller.EconomySetup;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.NumberFormatManager;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerPickupItem implements Listener {
    private final Main plugin;
    private final Database database;
    private final Economy economy;
    private final NumberFormatManager numberFormatManager;

    public PlayerPickupItem(Main plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.economy = EconomySetup.getEconomy();
        this.numberFormatManager = plugin.getFormatManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.hasPermission("rseller.autosell")) return;

        ItemStack itemStack = event.getItem().getItemStack();

        Item item = plugin.getItemManager().searchItem(itemStack);
        if (item == null) return;


        PlayerData playerData = database.getPlayerData(player.getUniqueId());
        if (!playerData.isAutosell(item.id())) return;

        double price = item.price();
        double points = item.points();

        if (price <= 0 || points <= 0) return;

        int amount = itemStack.getAmount();

        price *= amount;
        points *= amount;

        Booster booster = plugin.getBoosterManager().getBoosterByPlayer(player);
        if (booster == null) {
            booster = new Booster(1.0, 1.0);
        }

        double totalCoins = price * Math.max(1.0, booster.coinMultiplier());
        double totalPoints = points * Math.max(1.0, booster.pointMultiplier());

        event.setCancelled(true);
        event.getItem().remove();

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
    }
}
