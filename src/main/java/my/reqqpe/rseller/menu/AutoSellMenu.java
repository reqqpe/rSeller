package my.reqqpe.rseller.menu;


import me.clip.placeholderapi.PlaceholderAPI;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.managers.NumberFormatManager;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
    private final NumberFormatManager numberFormatManager;


    public AutoSellMenu(Main plugin, Database database) {
        super(plugin);
        this.database = database;
        this.autoSellManager = plugin.getAutoSellManager();
        this.numberFormatManager = plugin.getFormatManager();
    }

    @Override
    protected FileConfiguration getGuiConfig() {
        return plugin.getAutoSellGUI().getConfig();
    }


    @Override
    protected String getMenuId() {
        return "AUTO_SELL_MENU";
    }


    public void openMenu(Player player) {
        if (!player.hasPermission("rSeller.autosell")) {
            String message = plugin.getConfig().getString("messages.no-permission", "&cУ вас нет прав для использования автопродажи!");
            player.sendMessage(Colorizer.color(message));
            player.closeInventory();
            return;
        }

        super.openMenu(player);

        Inventory inv = player.getOpenInventory().getTopInventory();

        List<Integer> specialSlots = parseSlotList(guiConfig.getStringList("special-slots"));
        this.totalSlots = specialSlots.size();


        parseAutoSellItems(player, inv);

    }


    private void parseAutoSellItems(Player player, Inventory inv) {
        String category = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());

        List<Material> pageMaterials = getItemsByPage(category, getPlayerPage(player, category));


        ConfigurationSection itemSection = plugin.getItemsConfig().getConfig().getConfigurationSection("items");
        if (itemSection == null) return;

        var playerData = database.getPlayerData(player.getUniqueId());

        for (int i = 0; i < special_slots.size(); i++) {
            int slot = special_slots.get(i);

            if (i >= pageMaterials.size()) {
                inv.setItem(slot, null);
                continue;
            }

            Material material = pageMaterials.get(i);
            if (!itemSection.isConfigurationSection(material.name())) continue;

            ConfigurationSection data = itemSection.getConfigurationSection(material.name());
            if (data == null) continue;

            double price = data.getDouble("price", 0.0);
            double points = data.getDouble("points", 0.0);

            String itemName = data.getString("name");

            if (itemName == null || itemName.isEmpty()) {
                itemName = material.name().replace("_", " ");
            }

            boolean autosell = playerData.isAutosell(material);

            String enabled = Colorizer.color(plugin.getConfig().getString("messages.autosell-enable", "&aВключено"));
            String disabled = Colorizer.color(plugin.getConfig().getString("messages.autosell-disable", "&cВыключено"));
            String status = autosell ? enabled : disabled;

            String name = plugin.getAutoSellGUI().getConfig().getString("autosell_item.name", "&f{item_name}");
            List<String> lore = plugin.getAutoSellGUI().getConfig().getStringList("autosell_item.lore");

            ItemStack guiItem = new ItemStack(material);
            ItemMeta meta = guiItem.getItemMeta();
            if (meta != null) {
                String formattedPrice = numberFormatManager.format("autoSellGUI.sell_price", price);
                String formattedPoints = numberFormatManager.format("autoSellGUI.sell_points", points);

                name = name
                        .replace("{item_name}", itemName)
                        .replace("{state_autosell}", status)
                        .replace("{sell_price}", formattedPrice)
                        .replace("{sell_points}", formattedPoints);

                meta.setDisplayName(Colorizer.color(replacePlaceholders(player, name, inv)));

                List<String> formattedLore = new ArrayList<>();
                for (String line : lore) {
                    line = line
                            .replace("{item_name}", itemName)
                            .replace("{state_autosell}", status)
                            .replace("{sell_price}", formattedPrice)
                            .replace("{sell_points}", formattedPoints);

                    formattedLore.add(Colorizer.color(replacePlaceholders(player, line, inv)));
                }


                meta.setLore(formattedLore);
                guiItem.setItemMeta(meta);
            }

            inv.setItem(slot, guiItem);
        }
    }

    @Override
    protected String replacePlaceholders(Player player, String text, Inventory inventory) {
        if (text == null || text.isEmpty()) return "";

        String currentCategoryId = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        String currentCategoryName = autoSellManager.getCategoryName(currentCategoryId);

        text = text.replace("{current_category}", currentCategoryName);

        if (text.contains("{state_autosell_category}")) {
            boolean state = getCategoryAutosellState(player);

            String enabled = Colorizer.color(plugin.getConfig().getString("messages.autosell-enable", "&aВключено"));
            String disabled = Colorizer.color(plugin.getConfig().getString("messages.autosell-disable", "&cВыключено"));
            String status = state ? enabled : disabled;

            text = text.replace("{state_autosell_category}", status);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }

        return text;
    }


    @Override
    protected List<String> replacePlaceholders(Player player, List<String> list, Inventory inventory) {
        List<String> replaced = new ArrayList<>();

        String currentCategoryId = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());

        for (String line : list) {
            if (line.contains("{categories}")) {
                String prefixInLine = line.substring(0, line.indexOf("{categories}"));
                String suffixInLine = line.substring(line.indexOf("{categories}") + "{categories}".length());

                for (String categoryId : autoSellManager.getCategories().keySet()) {
                    if (!categoryId.equals(currentCategoryId)) {
                        String displayName = autoSellManager.getCategoryName(categoryId);
                        String fullLine = prefixInLine + displayName + suffixInLine;
                        fullLine = replacePlaceholders(player, fullLine, inventory);
                        replaced.add(Colorizer.color(fullLine));
                    }
                }
            } else {
                replaced.add(Colorizer.color(replacePlaceholders(player, line, inventory)));
            }
        }

        return replaced;
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
        List<Material> allItems = autoSellManager.getCategory(category)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int startIndex = (page - 1) * totalSlots;
        int endIndex = Math.min(startIndex + totalSlots, allItems.size());

        if (startIndex >= allItems.size()) return Collections.emptyList();

        return allItems.subList(startIndex, endIndex);
    }


    private int getTotalPages(String category) {
        List<Material> items = autoSellManager.getCategory(category);
        if (items.isEmpty()) return 1;
        return (int) Math.ceil((double) items.size() / totalSlots);
    }



    @EventHandler
    public void onClickInventory(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        handleClick(player, e);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        cancelItemUpdates(player);
    }


    @Override
    protected boolean handleSpecialSlotClick(Player player, InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return false;
        if (!holder.getId().equals(getMenuId())) return false;


        int rawSlot = e.getRawSlot();
        Inventory menuInv = e.getInventory();

        if (special_slots.contains(rawSlot)) {
            e.setCancelled(true);
            ItemStack clickedItem = menuInv.getItem(rawSlot);
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                Material material = clickedItem.getType();
                PlayerData playerData = database.getPlayerData(player.getUniqueId());

                boolean currentState = playerData.isAutosell(material);
                playerData.setAutosell(material, !currentState);

                parseAutoSellItems(player, menuInv);
                return true;
            }
        }
        return false;
    }


    protected void executeAction(Player player, String action) {

        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        if (action.equalsIgnoreCase("[next_page]")) {

            String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
            int currentPage = getPlayerPage(player, currentCategory);
            int totalPages = getTotalPages(currentCategory);

            if (currentPage < totalPages) {
                setPlayerPage(player, currentCategory, currentPage + 1);
                openMenu(player);
            }
        }

        else if (action.equalsIgnoreCase("[prev_page]")) {

            String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
            int currentPage = getPlayerPage(player, currentCategory);

            if (currentPage > 1) {
                setPlayerPage(player, currentCategory, currentPage - 1);
                openMenu(player);
            }
        }

        else if (action.equalsIgnoreCase("[next_category]")) {
            String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
            List<String> categoryIds = new ArrayList<>(autoSellManager.getCategories().keySet());

            if (!categoryIds.isEmpty()) {
                int currentIndex = categoryIds.indexOf(currentCategory);
                if (currentIndex == -1) currentIndex = 0;
                int nextIndex = (currentIndex + 1) % categoryIds.size();
                String newCategory = categoryIds.get(nextIndex);
                playerCategory.put(player.getUniqueId(), newCategory);
                setPlayerPage(player, newCategory, 1);
                openMenu(player);
            }
        }

        else if (action.equalsIgnoreCase("[prev_category]")) {
            String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
            List<String> categoryIds = new ArrayList<>(autoSellManager.getCategories().keySet());

            if (!categoryIds.isEmpty()) {
                int currentIndex = categoryIds.indexOf(currentCategory);
                if (currentIndex == -1) currentIndex = 0;
                int nextIndex = (currentIndex - 1 + categoryIds.size()) % categoryIds.size();
                String newCategory = categoryIds.get(nextIndex);
                playerCategory.put(player.getUniqueId(), newCategory);
                setPlayerPage(player, newCategory, 1);
                openMenu(player);
            }
        }

        else if (action.equalsIgnoreCase("[toggle_autosell_category]")) {
            String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
            List<Material> materials = autoSellManager.getCategoryMaterials(currentCategory);

            boolean autosellCategory = getCategoryAutosellState(player);

            if (!materials.isEmpty()) {
                PlayerData playerData = database.getPlayerData(player.getUniqueId());
                for (Material material : materials) {
                    playerData.setAutosell(material, !autosellCategory);
                }
                openMenu(player);
            }
        }
        else {
            runMainActions(player, action);
        }
    }

    private boolean getCategoryAutosellState(Player player) {
        String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        List<Material> materials = autoSellManager.getCategoryMaterials(currentCategory);


        if (!materials.isEmpty()) {
            List<Material> enabled = new ArrayList<>();
            List<Material> disabled = new ArrayList<>();

            PlayerData playerData = database.getPlayerData(player.getUniqueId());

            for (Material material : materials) {
                if (playerData.isAutosell(material)) {
                    enabled.add(material);
                }
                else {
                    disabled.add(material);
                }
            }

            return disabled.isEmpty();
        }
        return false;
    }

}
