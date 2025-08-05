package my.reqqpe.rseller.menu;


import me.clip.placeholderapi.PlaceholderAPI;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.managers.NumberFormatManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SellMenu extends AbstractMenu implements Listener {
    private final NumberFormatManager numberFormatManager;


    public SellMenu(Main plugin) {
        super(plugin);
        numberFormatManager = plugin.getFormatManager();
    }


    @Override
    protected FileConfiguration getGuiConfig() { return plugin.getAllSellGUIConfig().getConfig(); }


    @Override
    protected String getMenuId() { return "SELL_MENU"; }


    @Override
    public void openMenu(Player player) {
        super.openMenu(player);
    }

    @Override
    protected String replacePlaceholders(Player player, String text, Inventory inventory) {
        if (text == null || text.isEmpty()) return "";


        if (!(inventory.getHolder() instanceof CustomInventoryHolder holder)) return text;
        if (!holder.getId().equals(getMenuId())) return text;



        var result = plugin.getSellManager().calculateSellPreview(player, inventory, new ArrayList<>(special_slots));
        String coinsFormatted = numberFormatManager.format("mainGUI.sell_price", result.coins());
        String pointsFormatted = numberFormatManager.format("mainGUI.sell_points", result.points());

        text = text.replace("{sell_price}", coinsFormatted);
        text = text.replace("{sell_points}", pointsFormatted);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }


    @Override
    protected List<String> replacePlaceholders(Player player, List<String> list, Inventory inventory) {
        List<String> replaced = new ArrayList<>();

        for (String s : list) {
            replaced.add(replacePlaceholders(player, s, inventory));
        }

        return replaced;
    }



    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        Inventory inv = e.getInventory();

        for (int slot : special_slots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }

        cancelItemUpdates(player);
    }


    @EventHandler
    public void onClickInventory(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        handleClick(player, e);
    }


    @Override
    protected boolean handleSpecialSlotClick(Player player, InventoryClickEvent e) {

        int rawSlot = e.getRawSlot();

        if (special_slots.contains(rawSlot)) {
            e.setCancelled(false);
            return true;
        }

        return false;
    }



    protected void executeAction(Player player, String action) {
        if (action.equalsIgnoreCase("[sell]")) {
            Inventory inv = player.getOpenInventory().getTopInventory();
            if (!(inv.getHolder() instanceof CustomInventoryHolder holder)) return;
            if (!holder.getId().equals(getMenuId())) return;

            List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("special-slots"));
            plugin.getSellManager().sellItems(player, inv, sellSlots);
        } else {
            runMainActions(player, action);
        }
    }
}