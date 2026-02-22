package my.reqqpe.rseller.menus;

import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.managers.ItemManager;
import my.reqqpe.rseller.managers.MultiplierManager;
import my.reqqpe.rseller.managers.SellManager;
import my.reqqpe.rseller.models.Multiplier;
import my.reqqpe.rseller.models.ParsedAction;
import my.reqqpe.rseller.models.SellableItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AllSellMenu extends AbstractMenu {
    private final RSeller plugin;
    private final SellManager sellManager;
    private final ItemManager itemManager;
    private final MultiplierManager multiplierManager;

    public AllSellMenu(FileConfiguration guiConfig, RSeller plugin, SellManager sellManager, ItemManager itemManager, MultiplierManager multiplierManager) {
        super(guiConfig, plugin);
        this.plugin = plugin;
        this.sellManager = sellManager;
        this.itemManager = itemManager;
        this.multiplierManager = multiplierManager;
    }

    @Override
    public String getMenuId() {
        return "ALL_SELL_MENU";
    }



    @Override
    protected void handlePlayerInventoryClick(InventoryClickEvent e) {

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType().isAir()) return;

        e.setCancelled(true);

        Inventory top = e.getView().getTopInventory();
        Inventory bottom = e.getView().getBottomInventory();

        for (int slot : menu.specialSlots()) {
            if (top.getItem(slot) == null || top.getItem(slot).getType().isAir()) {
                top.setItem(slot, item.clone());
                bottom.setItem(e.getSlot(), null);
                return;
            }
        }
    }

    @Override
    protected void handleSpecialSlotsClick(InventoryClickEvent e) {

        if (e.getClick() == ClickType.DROP || e.getClick() == ClickType.CONTROL_DROP) {
            return;
        }

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType().isAir()) return;

        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        Inventory playerInv = player.getInventory();

        int free = playerInv.firstEmpty();

        if (free != -1) {
            playerInv.setItem(free, item.clone());
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
        }
        e.getClickedInventory().setItem(e.getSlot(), null);
    }

    @Override
    public void closeMenu(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();

        Inventory top = e.getView().getTopInventory();
        Inventory playerInv = player.getInventory();

        for (int slot : menu.specialSlots()) {

            ItemStack item = top.getItem(slot);
            if (item == null || item.getType().isAir()) continue;

            int free = playerInv.firstEmpty();

            if (free != -1) {
                playerInv.setItem(free, item.clone());
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
            }

            top.setItem(slot, null);
        }
    }


    @Override
    protected Map<String, String> buildLocalPlaceholders(Player player, Inventory inv) {
        Map<String, String> map = new HashMap<>(super.buildLocalPlaceholders(player, inv));
        double finalMoney = 0;
        double finalPoints = 0;

        for (int slot : menu.specialSlots()) {
            if (inv.getItem(slot) == null) {
                continue;
            }

            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            SellableItem sellableItem = itemManager.getSellableItem(item);
            if (sellableItem == null) {
                continue;
            }
            finalPoints += sellableItem.points() * item.getAmount();
            finalMoney += sellableItem.money() * item.getAmount();
        }

        Multiplier multiplier = multiplierManager.getMultiplierForPlayer(player);
        finalMoney *= multiplier.money();
        finalMoney *= multiplier.points();

        map.put("sell_money", String.valueOf(finalMoney));
        map.put("sell_points", String.valueOf(finalPoints));
        return map;
    }


    @Override
    protected void runCustomActions(Player player, ParsedAction pc) {
        switch (pc.action()) {
            case "sell": {
                Inventory inv = player.getOpenInventory().getTopInventory();
                sellManager.sell(player, inv, menu.specialSlots());
            }
        }
    }
}
