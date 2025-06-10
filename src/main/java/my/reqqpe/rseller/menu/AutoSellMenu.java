package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.HeadUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class AutoSellMenu extends AbstractMenu implements Listener {

    private final Database database;
    private final AutoSellManager autoSellManager;
    private final Map<UUID, String> playerCategory = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> playerCategoryPages = new HashMap<>();
    private int totalSlots;

    public AutoSellMenu(Main plugin, Database database) {
        super(plugin);
        this.database = database;
        this.autoSellManager = plugin.getAutoSellManager();
    }

    @Override
    protected FileConfiguration getGuiConfig() {
        return plugin.getAutoSellGUI().getConfig();
    }

    @Override
    protected String getMenuId() {
        return "AUTO_SELL_MENU";
    }

    @Override
    public void openMenu(Player player) {
        if (!player.hasPermission("rSeller.autosell")) {
            String message = plugin.getConfig().getString("messages.no-permission");
            player.sendMessage(Colorizer.color(message));
            player.closeInventory();
            return;
        }

        super.openMenu(player);

        Inventory inv = player.getOpenInventory().getTopInventory();

        List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("sell-slots"));
        this.totalSlots = sellSlots.size();

        populateCategoryButton(player, inv);
        populateAutoSellItems(player, inv, sellSlots);
        populatePageButtons(player, inv);
    }

    private void populateCategoryButton(Player player, Inventory inv) {
        String categoryPath = "items.category";
        if (!guiConfig.contains(categoryPath)) return;

        int slot = guiConfig.getInt(categoryPath + ".slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;

        String matString = guiConfig.getString(categoryPath + ".material");
        ItemStack item;

        if (matString != null && matString.toLowerCase().startsWith("basehead-")) {
            String base64 = matString.substring("basehead-".length());
            item = HeadUtil.getCustomHead(base64);
        } else {
            Material material = Material.matchMaterial(matString);
            if (material == null) {
                plugin.getLogger().warning("[" + getMenuId() + "] Неизвестный материал для элемента категории: " + matString + ". Использование BOOK.");
                material = Material.BOOK;
            }
            item = new ItemStack(material);
        }

        int customModelData = guiConfig.getInt(categoryPath + ".custom-model-data", -1);

        String currentCategoryId = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        String currentCategoryName = autoSellManager.getCategoryName(currentCategoryId);

        List<String> loreTemplate = guiConfig.getStringList(categoryPath + ".lore");
        List<String> finalLore = new ArrayList<>();

        if (loreTemplate != null) {
            for (String line : loreTemplate) {
                if (line.contains("{categories}")) {
                    String prefixInLine = line.substring(0, line.indexOf("{categories}"));
                    String suffixInLine = line.substring(line.indexOf("{categories}") + "{categories}".length());

                    for (String categoryId : autoSellManager.getCategories().keySet()) {
                        if (!categoryId.equals(currentCategoryId)) {
                            String displayName = autoSellManager.getCategoryName(categoryId);
                            finalLore.add(Colorizer.color(prefixInLine + displayName));
                        }
                    }
                    if (!suffixInLine.isEmpty()) {
                        finalLore.add(Colorizer.color(suffixInLine.replace("{current_category}", currentCategoryName)));
                    }
                } else {
                    finalLore.add(Colorizer.color(line.replace("{current_category}", currentCategoryName)));
                }
            }
        }


        ItemMeta meta = item.getItemMeta();
        if(meta != null) {
            if (customModelData != -1) {
                meta.setCustomModelData(customModelData);
            }
            meta.setDisplayName(Colorizer.color(guiConfig.getString(categoryPath + ".name", "").replace("{current_category}", currentCategoryName)));
            meta.setLore(finalLore);
            item.setItemMeta(meta);
        }

        inv.setItem(slot, item);
    }

    private void populateAutoSellItems(Player player, Inventory inv, List<Integer> sellSlots) {
        String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        int currentPage = getPlayerPage(player, currentCategory);
        List<Material> itemsPage = getItemsByPage(currentCategory, currentPage);

        for (int slot : sellSlots) {
            inv.setItem(slot, null);
        }

        PlayerData playerData = database.getPlayerData(player.getUniqueId());
        FileConfiguration itemsPriceConfig = plugin.getItemsConfig().getConfig();
        ConfigurationSection formats = plugin.getConfig().getConfigurationSection("numbers_format.autoSellGUI");

        for (int i = 0; i < itemsPage.size() && i < sellSlots.size(); i++) {
            Material material = itemsPage.get(i);
            if (material == null) continue;

            boolean autosellState = playerData.isAutosell(material);
            String itemConfigPath = "items." + material.name();
            double price = itemsPriceConfig.getDouble(itemConfigPath + ".price", 0.0);
            double points = itemsPriceConfig.getDouble(itemConfigPath + ".points", 0.0);
            int itemCustomModelData = itemsPriceConfig.getInt(itemConfigPath + ".custom-model-data", -1);

            String enabled = Colorizer.color(plugin.getConfig().getString("messages.autosell-enable"));
            String disable = Colorizer.color(plugin.getConfig().getString("messages.autosell-disable"));
            String status = autosellState ? enabled : disable;

            ItemStack item = createGuiItem(
                    material,
                    "autosell_item.name",
                    "autosell_item.lore",
                    itemCustomModelData,
                    "{state_autosell}", status,
                    "{sell_price}", String.format(formats.getString("sell_price"), price),
                    "{sell_points}", String.format(formats.getString("sell_points"), points)
            );

            ItemMeta meta = item.getItemMeta();
            if (meta != null && guiConfig.contains("autosell_item.name")) {
                String itemNameTemplate = guiConfig.getString("autosell_item.name");
                if (itemNameTemplate != null && itemNameTemplate.contains("{item_name}")) {
                    String finalItemName = itemNameTemplate.replace("{item_name}", material.name().replace("_", " "));
                    meta.setDisplayName(Colorizer.color(finalItemName));
                    item.setItemMeta(meta);
                }
            } else if (meta != null && !guiConfig.contains("autosell_item.name")) {
                meta.setDisplayName(Colorizer.color("&f" + material.name().replace("_", " ")));
                item.setItemMeta(meta);
            }

            inv.setItem(sellSlots.get(i), item);
        }
    }

    private void populatePageButtons(Player player, Inventory inv) {
        String nextPagePath = "items.next_page";
        int nextPageSlot = guiConfig.getInt(nextPagePath + ".slot", -1);

        if (nextPageSlot >= 0 && nextPageSlot < inv.getSize()) {
            ItemStack item = createGuiItem(
                    Material.matchMaterial(guiConfig.getString(nextPagePath + ".material", "ARROW")),
                    nextPagePath + ".name",
                    nextPagePath + ".lore",
                    guiConfig.getInt(nextPagePath + ".custom-model-data", -1)
            );
            inv.setItem(nextPageSlot, item);
        }

        String prevPagePath = "items.prev_page";
        int prevPageSlot = guiConfig.getInt(prevPagePath + ".slot", -1);

        if (prevPageSlot >= 0 && prevPageSlot < inv.getSize()) {
            ItemStack item = createGuiItem(
                    Material.matchMaterial(guiConfig.getString(prevPagePath + ".material", "ARROW")),
                    prevPagePath + ".name",
                    prevPagePath + ".lore",
                    guiConfig.getInt(prevPagePath + ".custom-model-data", -1)
            );
            inv.setItem(prevPageSlot, item);
        }
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
        if (materials.isEmpty()) return 1;
        return (int) Math.ceil((double) materials.size() / totalSlots);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("sell-slots"));
        int rawSlot = e.getRawSlot();
        Inventory inv = e.getInventory();

        e.setCancelled(true);

        if (e.getClickedInventory() != null && e.getClickedInventory().equals(player.getInventory())) {
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(false);
            }
            if (e.getAction().name().startsWith("PICKUP") || e.getAction().name().startsWith("SWAP")) {
                e.setCancelled(false);
            }
            return;
        }

        if (e.getClickedInventory() != null && e.getClickedInventory().equals(inv)) {
            if (sellSlots.contains(rawSlot)) {
                ItemStack clickedItem = inv.getItem(rawSlot);
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    Material material = clickedItem.getType();
                    PlayerData playerData = database.getPlayerData(player.getUniqueId());

                    boolean currentState = playerData.isAutosell(material);
                    playerData.setAutosell(material, !currentState);

                    openMenu(player);
                    return;
                } else {
                    e.setCancelled(false);
                    return;
                }
            }
        }

        int backButtonSlot = guiConfig.getInt("items.back.slot");
        if (rawSlot == backButtonSlot) {
            plugin.getSellMenu().openMenu(player);
            return;
        }

        int categoryButtonSlot = guiConfig.getInt("items.category.slot");
        if (rawSlot == categoryButtonSlot) {
            String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
            List<String> categoryIds = new ArrayList<>(autoSellManager.getCategories().keySet());

            if (categoryIds.isEmpty()) return;

            int currentIndex = categoryIds.indexOf(currentCategory);
            if (currentIndex == -1) currentIndex = 0;

            int nextIndex = currentIndex;
            if (e.isRightClick()) {
                nextIndex = (currentIndex + 1) % categoryIds.size();
            } else if (e.isLeftClick()) {
                nextIndex = (currentIndex - 1 + categoryIds.size()) % categoryIds.size();
            }

            String newCategory = categoryIds.get(nextIndex);
            playerCategory.put(player.getUniqueId(), newCategory);
            setPlayerPage(player, newCategory, 1);
            openMenu(player);
            return;
        }

        int nextPageSlot = guiConfig.getInt("items.next_page.slot");
        int prevPageSlot = guiConfig.getInt("items.prev_page.slot");

        String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        int currentPage = getPlayerPage(player, currentCategory);
        int totalPages = getTotalPages(currentCategory);

        if (rawSlot == nextPageSlot) {
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
