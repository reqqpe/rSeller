package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.SellManager;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class SellMenu implements Listener {

    private final Main plugin;
    private final SellManager sellManager;
    private final Database database;

    public SellMenu(Main plugin, SellManager sellManager, Database database) {
        this.plugin = plugin;
        this.sellManager = sellManager;
        this.database = database;
    }

    public void openMenu(Player player) {
        FileConfiguration guiConfig = plugin.getMainGUI().getConfig();
        String title = Colorizer.color(guiConfig.getString("title", "Скупщик"));
        int size = guiConfig.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 54;
        }
        Inventory inv = Bukkit.createInventory(new CustomInventoryHolder("SELL_MENU"), size, title);

        List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("sell-slots"));
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");

        Set<Integer> usedSlots = new HashSet<>(sellSlots);

        SellPreview preview = calculatePreviewInfo(player, inv);
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                String path = "items." + key;

                int slot = guiConfig.getInt(path + ".slot", -1);
                if (slot < 0 || slot >= inv.getSize()) continue;

                if (usedSlots.contains(slot)) {
                    plugin.getLogger().warning("[mainGUI] Пропущен предмет '" + key + "' из-за конфликта слота: " + slot);
                    continue;
                }

                Material material = Material.matchMaterial(guiConfig.getString(path + ".material", "STONE"));
                if (material == null) {
                    plugin.getLogger().warning("[mainGUI] Неизвестный материал для предмета '" + key + "'");
                    continue;
                }

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    unDuplicate(guiConfig, preview, path, item, meta);
                }

                inv.setItem(slot, item);
                usedSlots.add(slot);
            }
            String levelPath = "items.level_info";
            if (guiConfig.contains(levelPath)) {
                int levelSlot = guiConfig.getInt(levelPath + ".slot", -1);
                if (levelSlot >= 0 && levelSlot < inv.getSize()) {
                    Material levelMaterial = Material.getMaterial(guiConfig.getString(levelPath + ".material", "BOOK"));
                    if (levelMaterial == null) levelMaterial = Material.BOOK;

                    ItemStack infoItem = new ItemStack(levelMaterial);
                    ItemMeta infoMeta = infoItem.getItemMeta();

                    if (infoMeta != null) {
                        var levelInfo = plugin.getLevelManager().getLevelInfo(player);
                        int currentLevel = levelInfo.level();
                        PlayerData data = database.getPlayerData(player.getUniqueId());
                        double currentPoints = data.getPoints();
                        double nextLevelPoints = plugin.getLevelManager().getPointsForNextLevel(currentLevel);
                        double pointsToNext = Math.max(0, nextLevelPoints - currentPoints);

                        String name = guiConfig.getString(levelPath + ".name", "");
                        infoMeta.setDisplayName(Colorizer.color(name));

                        List<String> lore = guiConfig.getStringList(levelPath + ".lore");
                        if (lore != null && !lore.isEmpty()) {
                            ConfigurationSection formats = plugin.getConfig().getConfigurationSection("numbers_format.mainGUI");
                            List<String> finalLore = lore.stream().map(line -> line.replace("%level%", String.valueOf(currentLevel))
                                            .replace("%points_needed%", String.format(formats.getString("points_needed", "%.2f"), pointsToNext))
                                            .replace("%coin_multiplier%", String.format(formats.getString("coin_multiplier", "%.2f"), levelInfo.coinMultiplier()))
                                            .replace("%point_multiplier%", String.format(formats.getString("point_multiplier", "%.2f"), levelInfo.pointMultiplier())))
                                    .toList();
                            infoMeta.setLore(Colorizer.colorizeAll(finalLore));
                        }

                        infoItem.setItemMeta(infoMeta);
                    }

                    inv.setItem(levelSlot, infoItem);
                }
            }
        }

        player.openInventory(inv);
    }

    private void unDuplicate(FileConfiguration guiConfig, SellPreview preview, String path, ItemStack item, ItemMeta meta) {
        String name = guiConfig.getString(path + ".name", "");
        meta.setDisplayName(Colorizer.color(name));

        List<String> lore = guiConfig.getStringList(path + ".lore");
        ConfigurationSection formats = plugin.getConfig().getConfigurationSection("numbers_format.mainGUI");
        if (lore != null && !lore.isEmpty()) {
            List<String> finalLore = lore.stream().map(line ->
                            line.replace("%sell_price%", String.format(formats.getString("sell_price"), preview.totalCoins))
                                    .replace("%sell_points%", String.format(formats.getString("sell_points"), preview.totalPoints)))
                    .toList();
            meta.setLore(Colorizer.colorizeAll(finalLore));
        }

        item.setItemMeta(meta);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals("SELL_MENU")) return;

        FileConfiguration guiConfig = plugin.getMainGUI().getConfig();
        List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("sell-slots"));
        Inventory inv = e.getInventory();

        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }
    }
    private static List<Integer> parseSlotList(List<String> list) {
        List<Integer> result = new ArrayList<>();
        for (String str : list) {
            if (str.contains("-")) {
                String[] parts = str.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                for (int i = start; i <= end; i++) {
                    result.add(i);
                }
            } else {
                result.add(Integer.parseInt(str));
            }
        }
        return result;
    }
    private void updateSellButton(Inventory inv) {
        FileConfiguration guiConfig = plugin.getMainGUI().getConfig();
        String path = "items.sell_item";

        if (!guiConfig.contains(path)) return;

        Material material = Material.getMaterial(guiConfig.getString(path + ".material", "STONE"));
        if (material == null) material = Material.STONE;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Player player = null;
            for (HumanEntity viewer : inv.getViewers()) {
                if (viewer instanceof Player p) {
                    player = p;
                    break;
                }
            }
            if (player == null) return;

            SellPreview preview = calculatePreviewInfo(player, inv);

            unDuplicate(guiConfig, preview, path, item, meta);
        }

        int slot = guiConfig.getInt(path + ".slot", 49);
        inv.setItem(slot, item);
    }
    private Set<Integer> getConfiguredItemSlots() {
        Set<Integer> slots = new HashSet<>();
        ConfigurationSection section = plugin.getMainGUI().getConfig().getConfigurationSection("items");
        if (section == null) return slots;

        for (String key : section.getKeys(false)) {
            slots.add(plugin.getMainGUI().getConfig().getInt("items." + key + ".slot"));
        }
        return slots;
    }



    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals("SELL_MENU")) return;

        FileConfiguration guiConfig = plugin.getMainGUI().getConfig();

        List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("sell-slots"));
        Set<Integer> protectedSlots = getConfiguredItemSlots();

        int rawSlot = e.getRawSlot();
        Inventory clickedInv = e.getClickedInventory();
        Inventory inv = e.getInventory();

        if (protectedSlots.contains(rawSlot) && clickedInv == inv) {
            e.setCancelled(true);
        }


        int sellButtonSlot = guiConfig.getConfigurationSection("items").getInt("sell_item.slot");
        if (rawSlot == sellButtonSlot) {
            e.setCancelled(true);
            sellManager.sellItems(player, inv, sellSlots);
            return;
        }

        int autosellButtonSlot = guiConfig.getConfigurationSection("items").getInt("autosell.slot");
        if (rawSlot == autosellButtonSlot) {
            e.setCancelled(true);
            plugin.getAutoSellMenu().openMenu(player);
            return;
        }

        boolean shouldUpdate = false;
        if (sellSlots.contains(rawSlot)) {
            shouldUpdate = true;
        } else if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            shouldUpdate = true;
        }

        if (shouldUpdate) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateSellButton(inv);
            }, 1L);
        }
    }
    private class SellPreview {
        double totalCoins;
        double totalPoints;

        public SellPreview(double coins, double points) {
            this.totalCoins = coins;
            this.totalPoints = points;
        }
    }
    private SellPreview calculatePreviewInfo(Player player, Inventory inv) {
        FileConfiguration itemsConfig = plugin.getItemsConfig().getConfig();
        List<Integer> sellSlots = parseSlotList(plugin.getMainGUI().getConfig().getStringList("sell-slots"));

        double totalCoins = 0;
        double totalPoints = 0;

        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            String key = "items." + item.getType().name();
            if (!itemsConfig.contains(key)) continue;

            double price = itemsConfig.getDouble(key + ".price", 0);
            double points = itemsConfig.getInt(key + ".points", 0);

            int amount = item.getAmount();
            totalCoins += price * amount;
            totalPoints += points * amount;
        }

        var levelInfo = plugin.getLevelManager().getLevelInfo(player);
        totalCoins *= levelInfo.coinMultiplier();
        totalPoints *= levelInfo.pointMultiplier();

        return new SellPreview(totalCoins, totalPoints);
    }
}