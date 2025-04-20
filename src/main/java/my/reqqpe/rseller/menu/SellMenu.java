package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.managers.SellManager;
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
import java.util.stream.Collectors;

public class SellMenu implements Listener {

    private final Main plugin;
    private final SellManager sellManager;
    public SellMenu(Main plugin, SellManager sellManager) {
        this.plugin = plugin;
        this.sellManager = sellManager;
    }

    public void openMenu(Player player) {
        FileConfiguration guiConfig = plugin.getMainGUI().getConfig();
        String title = ChatColor.translateAlternateColorCodes('&', guiConfig.getString("title", "Скупщик"));
        Inventory inv = Bukkit.createInventory(new CustomInventoryHolder("SELL_MENU"), 54, title);

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
                    plugin.getLogger().warning("Пропущен предмет '" + key + "' из-за конфликта слота: " + slot);
                    continue;
                }

                Material material = Material.matchMaterial(guiConfig.getString(path + ".material", "STONE"));
                if (material == null) {
                    plugin.getLogger().warning("Неизвестный материал для предмета '" + key + "'");
                    continue;
                }

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', guiConfig.getString(path + ".name", "")));

                    List<String> lore = guiConfig.getStringList(path + ".lore");
                    if (lore != null && !lore.isEmpty()) {
                        List<String> finalLore = lore.stream()
                                .map(line -> ChatColor.translateAlternateColorCodes('&',
                                        line.replace("%sell_price%", String.format("%.2f", preview.totalCoins))
                                                .replace("%sell_points%", String.valueOf(preview.totalPoints))))
                                .collect(Collectors.toList());
                        meta.setLore(finalLore);
                    }

                    item.setItemMeta(meta);
                }

                inv.setItem(slot, item);
                usedSlots.add(slot); // Больше этот слот не трогать
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
                        int currentPoints = plugin.getLevelManager().getPlayerPoints(player);
                        int nextLevelPoints = plugin.getLevelManager().getPointsForNextLevel(currentLevel);
                        int pointsToNext = Math.max(0, nextLevelPoints - currentPoints);

                        infoMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                                guiConfig.getString(levelPath + ".name", "")));

                        List<String> lore = guiConfig.getStringList(levelPath + ".lore");
                        if (lore != null && !lore.isEmpty()) {
                            List<String> finalLore = lore.stream()
                                    .map(line -> ChatColor.translateAlternateColorCodes('&',
                                            line.replace("%level%", String.valueOf(currentLevel))
                                                    .replace("%points_needed%", String.valueOf(pointsToNext))
                                                    .replace("%coin_multiplier%", String.format("%.2f", levelInfo.coinMultiplier()))
                                                    .replace("%point_multiplier%", String.valueOf(levelInfo.pointMultiplier()))))
                                    .collect(Collectors.toList());
                            infoMeta.setLore(finalLore);
                        }

                        infoItem.setItemMeta(infoMeta);
                    }

                    inv.setItem(levelSlot, infoItem);
                }
            }
        }

        player.openInventory(inv);
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
    private int calculateTotalSellPrice(Inventory inv) {
        FileConfiguration itemsConfig = plugin.getItemsConfig().getConfig();
        List<Integer> sellSlots = parseSlotList(plugin.getMainGUI().getConfig().getStringList("sell-slots"));

        int total = 0;
        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            String path = "items." + item.getType().name();
            if (!itemsConfig.contains(path + ".price")) continue;

            int pricePerItem = itemsConfig.getInt(path + ".price");
            total += pricePerItem * item.getAmount();
        }
        return total;
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
            // Получаем игрока (через owner инвентаря, это костыльно, лучше передавать явно, но пока ок)
            Player player = null;
            for (HumanEntity viewer : inv.getViewers()) {
                if (viewer instanceof Player p) {
                    player = p;
                    break;
                }
            }
            if (player == null) return;

            SellPreview preview = calculatePreviewInfo(player, inv);

            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    guiConfig.getString(path + ".name", "")));

            List<String> lore = guiConfig.getStringList(path + ".lore");
            if (lore != null && !lore.isEmpty()) {
                List<String> finalLore = lore.stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&',
                                line.replace("%sell_price%", String.format("%.2f", preview.totalCoins))
                                        .replace("%sell_points%", String.valueOf(preview.totalPoints))))
                        .collect(Collectors.toList());
                meta.setLore(finalLore);
            }

            item.setItemMeta(meta);
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

        // Блокировка декоративных слотов
        if (protectedSlots.contains(rawSlot) && clickedInv == inv) {
            e.setCancelled(true);
        }

        // Проверка нажатия на кнопку продажи
        int sellButtonSlot = guiConfig.getConfigurationSection("items").getInt("sell_item.slot");
        if (rawSlot == sellButtonSlot) {
            e.setCancelled(true);
            sellManager.sellItems(player, inv, sellSlots);
            return;
        }

        // Обновление кнопки при клике по sell-слотам или Shift-клике
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
        int totalPoints;

        public SellPreview(double coins, int points) {
            this.totalCoins = coins;
            this.totalPoints = points;
        }
    }
    private SellPreview calculatePreviewInfo(Player player, Inventory inv) {
        FileConfiguration itemsConfig = plugin.getItemsConfig().getConfig();
        List<Integer> sellSlots = parseSlotList(plugin.getMainGUI().getConfig().getStringList("sell-slots"));

        double totalCoins = 0;
        int totalPoints = 0;

        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;

            String key = "items." + item.getType().name();
            if (!itemsConfig.contains(key)) continue;

            double price = itemsConfig.getDouble(key + ".price", 0);
            int points = itemsConfig.getInt(key + ".points", 0);

            int amount = item.getAmount();
            totalCoins += price * amount;
            totalPoints += points * amount;
        }

        // Применим бустеры
        var levelInfo = plugin.getLevelManager().getLevelInfo(player);
        totalCoins *= levelInfo.coinMultiplier();
        totalPoints *= levelInfo.pointMultiplier();

        return new SellPreview(totalCoins, totalPoints);
    }
}
