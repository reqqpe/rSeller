package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class AutoSellMenu implements Listener {

    private final Main plugin;
    private final Database database;
    private final AutoSellManager autoSellManager;
    private final Map<UUID, String> playerCategory = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> playerCategoryPages = new HashMap<>();
    private int totalSlots;

    public AutoSellMenu(Main plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.autoSellManager = plugin.getAutoSellManager();
    }
    public void openMenu(Player player) {

        if(!player.hasPermission("rSeller.autosell")) {
            String message = plugin.getConfig().getString("messages.no-permission");
            player.sendMessage(Colorizer.color(message));
            player.closeInventory();
            return;
        }



        FileConfiguration guiConfig = plugin.getAutoSellGUI().getConfig();
        String title = Colorizer.color(guiConfig.getString("title", "Скупщик"));
        int size = guiConfig.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 54;
        }
        Inventory inv = Bukkit.createInventory(new CustomInventoryHolder("AUTO_SELL_MENU"), size, title);

        List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("sell-slots"));
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        totalSlots = sellSlots.size();


        Set<Integer> usedSlots = new HashSet<>(sellSlots);


        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                String path = "items." + key;

                int slot = guiConfig.getInt(path + ".slot", -1);
                if (slot < 0 || slot >= inv.getSize()) continue;

                if (usedSlots.contains(slot)) {
                    plugin.getLogger().warning("[AutoSellGUI] Пропущен предмет '" + key + "' из-за конфликта слота: " + slot);
                    continue;
                }

                Material material = Material.matchMaterial(guiConfig.getString(path + ".material", "STONE"));
                if (material == null) {
                    plugin.getLogger().warning("[AutoSellGUI] Неизвестный материал для предмета '" + key + "'");
                    continue;
                }

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();

                if (meta != null) {
                    String name = guiConfig.getString(path + ".name", "");
                    meta.setDisplayName(Colorizer.color(name));
                    List<String> lore = guiConfig.getStringList(path + ".lore");
                    if (lore != null && !lore.isEmpty()) {
                        lore = Colorizer.colorizeAll(lore);
                        meta.setLore(lore);
                    }
                    item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
                usedSlots.add(slot);
            }
            String categoryPath = "items.category";
            if (guiConfig.contains(categoryPath)) {
                int slot = guiConfig.getInt(categoryPath + ".slot", -1);
                if (slot >= 0 && slot < inv.getSize()) {
                    Material material = Material.matchMaterial(guiConfig.getString(categoryPath + ".material"));
                    if (material == null) material = Material.BOOK;

                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();

                    if (meta != null) {
                        String currentCategoryId = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
                        String currentCategoryName = autoSellManager.getCategoryName(currentCategoryId);

                        String name = guiConfig.getString(categoryPath + ".name", "");
                        meta.setDisplayName(Colorizer.color(name.replace("{current_category}", currentCategoryName)));

                        List<String> lore = guiConfig.getStringList(categoryPath + ".lore");
                        List<String> finalLore = new ArrayList<>();

                        for (String line : lore) {
                            if (line.contains("{categories}")) {
                                String prefix = line.substring(0, line.indexOf("{categories}")).trim();
                                String suffix = line.substring(line.indexOf("{categories}") + "{categories}".length()).trim();

                                for (String categoryId : autoSellManager.getCategories().keySet()) {
                                    if (!categoryId.equals(currentCategoryId)) {
                                        String displayName = autoSellManager.getCategoryName(categoryId);
                                        finalLore.add(Colorizer.color(
                                                (prefix.isEmpty() ? "" : prefix) +
                                                        displayName +
                                                        (suffix.isEmpty() ? "" : suffix)
                                        ));
                                    }
                                }
                            } else {
                                finalLore.add(Colorizer.color(line.replace("{current_category}", currentCategoryName)));
                            }
                        }

                        meta.setLore(finalLore);
                        item.setItemMeta(meta);
                        inv.setItem(slot, item);
                    }
                }
            }
        }
        String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        int currentPage = getPlayerPage(player, currentCategory);
        List<Material> itemsPage = getItemsByPage(currentCategory, currentPage);

        for (int i = 0; i < itemsPage.size() && i < sellSlots.size(); i++) {
            Material material = itemsPage.get(i);
            if (material == null) continue;

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = guiConfig.getStringList("autosell_item.lore");
                if (lore != null && !lore.isEmpty()) {
                    PlayerData playerData = database.getPlayerData(player.getUniqueId());

                    boolean newState = playerData.isAutosell(material);

                    String path = "items." + material.name();
                    double price = plugin.getItemsConfig().getConfig().getDouble(path + ".price", 0.0);
                    double points = plugin.getItemsConfig().getConfig().getDouble(path + ".points", 0.0);

                    List<String> finalLore = new ArrayList<>();
                    for (String line : lore) {
                        String enabled = Colorizer.color(plugin.getConfig().getString("messages.autosell-enable"));
                        String disable = Colorizer.color(plugin.getConfig().getString("messages.autosell-disable"));

                        String status = newState ? enabled : disable;



                        ConfigurationSection formats = plugin.getConfig().getConfigurationSection("numbers_format.autoSellGUI");
                        finalLore.add(Colorizer.color(
                                line.replace("{state_autosell}", status)
                                        .replace("{sell_price}", String.format(formats.getString("sell_price"), price))
                                        .replace("{sell_points}", String.format(formats.getString("sell_points"), points))
                        ));
                    }
                    meta.setLore(finalLore);
                    item.setItemMeta(meta);
                }
            }
            inv.setItem(sellSlots.get(i), item);
        }
        player.openInventory(inv);
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

    public int getPlayerPage(Player player, String category) {
        return playerCategoryPages
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .getOrDefault(category, 1);
    }

    public void setPlayerPage(Player player, String category, int page) {
        playerCategoryPages
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(category, page);
    }

    private List<Material> getItemsByPage(String category, int page) {
        List<Material> allMaterials = autoSellManager.getCategory(category)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int startIndex = (page - 1) * totalSlots;
        int endIndex = Math.min(startIndex + totalSlots, allMaterials.size());

        if (startIndex >= allMaterials.size()) return Collections.emptyList();

        return allMaterials.subList(startIndex, endIndex);
    }

    private int getTotalPages(String category) {
        List<Material> materials = autoSellManager.getCategory(category);
        return (int) Math.ceil((double) materials.size() / totalSlots);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals("AUTO_SELL_MENU")) return;

        FileConfiguration guiConfig = plugin.getAutoSellGUI().getConfig();
        List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("sell-slots"));

        int rawSlot = e.getRawSlot();
        Inventory clickedInv = e.getClickedInventory();
        Inventory inv = e.getInventory();

        e.setCancelled(true);

        // Обработка кнопки категорий
        int sellButtonSlot = guiConfig.getInt("items.back.slot");
        if (rawSlot == sellButtonSlot) {
            plugin.getSellMenu().openMenu(player);
            return;
        }


        int categoryButtonSlot = guiConfig.getInt("items.category.slot");
        if (rawSlot == categoryButtonSlot) {
            String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
            List<String> categoryIds = new ArrayList<>(autoSellManager.getCategories().keySet());

            int currentIndex = categoryIds.indexOf(currentCategory);
            if (currentIndex == -1) currentIndex = 0;

            int nextIndex = currentIndex;
            if (e.isLeftClick()) {
                nextIndex = (currentIndex - 1 + categoryIds.size()) % categoryIds.size(); // ЛКМ — назад
            } else if (e.isRightClick()) {
                nextIndex = (currentIndex + 1) % categoryIds.size(); // ПКМ — вперёд
            }

            String newCategory = categoryIds.get(nextIndex);
            playerCategory.put(player.getUniqueId(), newCategory);
            setPlayerPage(player, newCategory, 1);
            openMenu(player);
            return;
        }

        // Обработка кликов по слотам с предметами
        if (sellSlots.contains(rawSlot)) {
            ItemStack clickedItem = inv.getItem(rawSlot);
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            Material material = clickedItem.getType();
            PlayerData playerData = database.getPlayerData(player.getUniqueId());

            boolean currentState = playerData.isAutosell(material);
            playerData.setAutosell(material, !currentState);

            openMenu(player); // обновляем меню после переключения
        }
        int nextPageSlot = guiConfig.getInt("items.next_page.slot");
        int prevPageSlot = guiConfig.getInt("items.prev_page.slot");

        UUID uuid = player.getUniqueId();
        String currentCategory = playerCategory.getOrDefault(uuid, autoSellManager.getFirstCategory());
        int currentPage = getPlayerPage(player, currentCategory);

        if (rawSlot == nextPageSlot) {
            int totalPages = getTotalPages(currentCategory);
            if (currentPage < totalPages) {
                setPlayerPage(player, currentCategory, currentPage + 1);
                openMenu(player);
            }
            return;
        }

        if (rawSlot == prevPageSlot) {
            if (currentPage > 1) {
                setPlayerPage(player, currentCategory, currentPage - 1);
                openMenu(player);
            }
        }
    }
}
