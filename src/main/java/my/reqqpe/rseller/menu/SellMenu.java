package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.SellManager;
import my.reqqpe.rseller.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class SellMenu extends AbstractMenu implements Listener {

    private final SellManager sellManager;
    private final Database database;

    public SellMenu(Main plugin, SellManager sellManager, Database database) {
        super(plugin);
        this.sellManager = sellManager;
        this.database = database;
    }

    @Override
    protected FileConfiguration getGuiConfig() {
        return plugin.getMainGUI().getConfig();
    }

    @Override
    protected String getMenuId() {
        return "SELL_MENU";
    }

    @Override
    public void openMenu(Player player) {
        super.openMenu(player);

        Inventory inv = player.getOpenInventory().getTopInventory();
        populateSellPreviewAndLevelInfo(player, inv);
    }


    private void populateSellPreviewAndLevelInfo(Player player, Inventory inv) {
        SellPreview preview = calculatePreviewInfo(player, inv);

        String sellItemPath = "items.sell_item";
        if (guiConfig.contains(sellItemPath)) {
            String matString = guiConfig.getString(sellItemPath + ".material", "STONE");
            ItemStack sellItem;

            if (matString.toLowerCase().startsWith("basehead-")) {
                String base64 = matString.substring("basehead-".length());
                sellItem = HeadUtil.getCustomHead(base64);
            } else {
                Material material = Material.matchMaterial(matString);
                if (material == null) {
                    plugin.getLogger().warning("[" + getMenuId() + "] Неизвестный материал для sell_item: " + matString + ". Использование STONE.");
                    material = Material.STONE;
                }
                sellItem = new ItemStack(material);
            }

            int customModelData = guiConfig.getInt(sellItemPath + ".custom-model-data", -1);
            ConfigurationSection formats = plugin.getConfig().getConfigurationSection("numbers_format.mainGUI");

            sellItem = createGuiItem(
                    sellItem,
                    sellItemPath + ".name",
                    sellItemPath + ".lore",
                    customModelData,
                    "%sell_price%", String.format(formats.getString("sell_price"), preview.totalCoins),
                    "%sell_points%", String.format(formats.getString("sell_points"), preview.totalPoints)
            );
            int slot = guiConfig.getInt(sellItemPath + ".slot", 49);
            inv.setItem(slot, sellItem);
        }

        String levelPath = "items.level_info";
        if (guiConfig.contains(levelPath)) {
            int levelSlot = guiConfig.getInt(levelPath + ".slot", -1);
            if (levelSlot >= 0 && levelSlot < inv.getSize()) {
                String matString = guiConfig.getString(levelPath + ".material", "BOOK");
                ItemStack infoItem;

                if (matString.toLowerCase().startsWith("basehead-")) {
                    String base64 = matString.substring("basehead-".length());
                    infoItem = HeadUtil.getCustomHead(base64);
                } else {
                    Material levelMaterial = Material.matchMaterial(matString);
                    if (levelMaterial == null) {
                        plugin.getLogger().warning("[" + getMenuId() + "] Неизвестный материал для level_info: " + matString + ". Использование BOOK.");
                        levelMaterial = Material.BOOK;
                    }
                    infoItem = new ItemStack(levelMaterial);
                }

                int customModelData = guiConfig.getInt(levelPath + ".custom-model-data", -1);

                var levelInfo = plugin.getLevelManager().getLevelInfo(player);
                int currentLevel = levelInfo.level();
                PlayerData data = database.getPlayerData(player.getUniqueId());
                double currentPoints = data.getPoints();
                double nextLevelPoints = plugin.getLevelManager().getPointsForNextLevel(currentLevel);
                double pointsToNext = Math.max(0, nextLevelPoints - currentPoints);

                ConfigurationSection formats = plugin.getConfig().getConfigurationSection("numbers_format.mainGUI");

                // Передаем созданный infoItem в createGuiItem
                infoItem = createGuiItem(
                        infoItem, // Используем уже созданный ItemStack
                        levelPath + ".name",
                        levelPath + ".lore",
                        customModelData,
                        "%level%", String.valueOf(currentLevel),
                        "%points_needed%", String.format(formats.getString("points_needed", "%.2f"), pointsToNext),
                        "%coin_multiplier%", String.format(formats.getString("coin_multiplier", "%.2f"), levelInfo.coinMultiplier()),
                        "%point_multiplier%", String.format(formats.getString("point_multiplier", "%.2f"), levelInfo.pointMultiplier())
                );
                inv.setItem(levelSlot, infoItem);
            }
        }

        // --- Обработка элемента автопродажи (autosell) ---
        String autosellPath = "items.autosell";
        if (guiConfig.contains(autosellPath)) {
            int autosellSlot = guiConfig.getInt(autosellPath + ".slot", -1);
            if (autosellSlot >= 0 && autosellSlot < inv.getSize()) {
                String matString = guiConfig.getString(autosellPath + ".material", "COMPARATOR");
                ItemStack autosellItem;

                if (matString.toLowerCase().startsWith("basehead-")) {
                    String base64 = matString.substring("basehead-".length());
                    autosellItem = HeadUtil.getCustomHead(base64);
                } else {
                    Material autosellMaterial = Material.matchMaterial(matString);
                    if (autosellMaterial == null) {
                        plugin.getLogger().warning("[" + getMenuId() + "] Неизвестный материал для autosell: " + matString + ". Использование COMPARATOR.");
                        autosellMaterial = Material.COMPARATOR;
                    }
                    autosellItem = new ItemStack(autosellMaterial);
                }

                int customModelData = guiConfig.getInt(autosellPath + ".custom-model-data", -1);

                // Передаем созданный autosellItem в createGuiItem
                autosellItem = createGuiItem(
                        autosellItem, // Используем уже созданный ItemStack
                        autosellPath + ".name",
                        autosellPath + ".lore",
                        customModelData
                );
                inv.setItem(autosellSlot, autosellItem);
            }
        }
    }


    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

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

    private Set<Integer> getConfiguredItemSlots() {
        Set<Integer> slots = new HashSet<>();
        ConfigurationSection section = guiConfig.getConfigurationSection("items");
        if (section == null) return slots;

        for (String key : section.getKeys(false)) {
            slots.add(guiConfig.getInt("items." + key + ".slot"));
        }
        return slots;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("sell-slots"));
        Set<Integer> protectedSlots = getConfiguredItemSlots();
        int rawSlot = e.getRawSlot();
        Inventory clickedInv = e.getClickedInventory();
        Inventory menuInv = e.getInventory();

        if (protectedSlots.contains(rawSlot) && clickedInv == menuInv) {
            e.setCancelled(true);
        }

        int sellButtonSlot = guiConfig.getConfigurationSection("items").getInt("sell_item.slot");
        if (rawSlot == sellButtonSlot) {
            e.setCancelled(true);
            sellManager.sellItems(player, menuInv, sellSlots);
            openMenu(player);
            return;
        }

        int autosellButtonSlot = guiConfig.getConfigurationSection("items").getInt("autosell.slot");
        if (rawSlot == autosellButtonSlot) {
            e.setCancelled(true);
            plugin.getAutoSellMenu().openMenu(player);
            return;
        }

        boolean shouldUpdate = false;

        if (clickedInv == menuInv) {
            if (sellSlots.contains(rawSlot) ||
                    e.getAction().name().startsWith("PICKUP") ||
                    e.getAction().name().startsWith("PLACE") ||
                    e.getAction() == InventoryAction.SWAP_WITH_CURSOR ||
                    e.getAction() == InventoryAction.HOTBAR_SWAP)
            {
                if (!protectedSlots.contains(rawSlot)) {
                    shouldUpdate = true;
                }
            }
        } else {
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                    e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                shouldUpdate = true;
            }
        }

        if (shouldUpdate) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                populateSellPreviewAndLevelInfo(player, menuInv);
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