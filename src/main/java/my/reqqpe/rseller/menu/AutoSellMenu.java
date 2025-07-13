package my.reqqpe.rseller.menu;


import me.clip.placeholderapi.PlaceholderAPI;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.managers.NumberFormatManager;
import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.SyntaxParser;
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
    public String getMenuId() {
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
        updatePlaceholders(player, inv);

    }
    private void parseAutoSellItems(Player player, Inventory inv) {
        List<Integer> specialSlots = parseSlotList(guiConfig.getStringList("special-slots"));
        String category = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());

        List<Material> pageMaterials = getItemsByPage(category, getPlayerPage(player, category));


        ConfigurationSection itemSection = plugin.getItemsConfig().getConfig().getConfigurationSection("items");
        if (itemSection == null) return;

        var playerData = database.getPlayerData(player.getUniqueId());

        for (int i = 0; i < specialSlots.size(); i++) {
            int slot = specialSlots.get(i);

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

                meta.setDisplayName(Colorizer.color(name
                        .replace("{item_name}", itemName)
                        .replace("{state_autosell}", status)
                        .replace("{sell_price}", formattedPrice)
                        .replace("{sell_points}", formattedPoints)
                ));

                List<String> formattedLore = new ArrayList<>();
                for (String line : lore) {
                    formattedLore.add(Colorizer.color(line
                            .replace("{item_name}", itemName)
                            .replace("{state_autosell}", status)
                            .replace("{sell_price}", formattedPrice)
                            .replace("{sell_points}", formattedPoints)
                    ));
                }

                meta.setLore(formattedLore);
                guiItem.setItemMeta(meta);
            }

            inv.setItem(slot, guiItem);
        }
    }

    public void updatePlaceholders(Player player, Inventory inv) {
        String currentCategoryId = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        String currentCategoryName = autoSellManager.getCategoryName(currentCategoryId);

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            boolean changed = false;

            if (meta.hasDisplayName()) {
                String displayName = meta.getDisplayName()
                        .replace("{current_category}", currentCategoryName);
                displayName = PlaceholderAPI.setPlaceholders(player, displayName);
                meta.setDisplayName(displayName);
                changed = true;
            }

            if (meta.hasLore()) {
                List<String> newLore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    if (line.contains("{categories}")) {
                        String prefixInLine = line.substring(0, line.indexOf("{categories}"));
                        String suffixInLine = line.substring(line.indexOf("{categories}") + "{categories}".length());

                        for (String categoryId : autoSellManager.getCategories().keySet()) {
                            if (!categoryId.equals(currentCategoryId)) {
                                String displayName = autoSellManager.getCategoryName(categoryId);
                                String processedLine = Colorizer.color(prefixInLine + displayName);
                                processedLine = PlaceholderAPI.setPlaceholders(player, processedLine);
                                newLore.add(processedLine);
                            }
                        }
                        if (!suffixInLine.isEmpty()) {
                            String suffixLine = Colorizer.color(suffixInLine.replace("{current_category}", currentCategoryName));
                            suffixLine = PlaceholderAPI.setPlaceholders(player, suffixLine);
                            newLore.add(suffixLine);
                        }
                    } else {
                        String processedLine = Colorizer.color(line.replace("{current_category}", currentCategoryName));
                        processedLine = PlaceholderAPI.setPlaceholders(player, processedLine);
                        newLore.add(processedLine);
                    }
                }
                meta.setLore(newLore);
                changed = true;
            }

            if (changed) {
                item.setItemMeta(meta);
                inv.setItem(i, item);
            }
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
    private void InventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        int rawSlot = e.getRawSlot();
        Inventory clickedInv = e.getClickedInventory();
        Inventory menuInv = e.getInventory();

        e.setCancelled(true);



        if (clickedInv == menuInv && rawSlot < menuInv.getSize()) {
            if (special_slots.contains(rawSlot)) {
                ItemStack clickedItem = menuInv.getItem(rawSlot);
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    Material material = clickedItem.getType();
                    PlayerData playerData = database.getPlayerData(player.getUniqueId());

                    boolean currentState = playerData.isAutosell(material);
                    playerData.setAutosell(material, !currentState);

                    parseAutoSellItems(player, menuInv);
                    return;
                }
            }

            String itemId = slotToItemId.get(rawSlot);
            if (itemId == null) return;

            ConfigurationSection itemSection = guiConfig.getConfigurationSection("items." + itemId);
            if (itemSection == null) return;

            boolean isLeftClick = e.isLeftClick();
            boolean isRightClick = e.isRightClick();

            ConfigurationSection reqSection = itemSection.getConfigurationSection(
                    isLeftClick ? "left_click_requaments" :
                            isRightClick ? "right_click_requaments" : null);

            boolean allRequirementsPassed = true;

            if (reqSection != null) {
                ConfigurationSection sub = reqSection.getConfigurationSection("requaments");
                if (sub != null && !sub.getKeys(false).isEmpty()) {
                    for (String key : sub.getKeys(false)) {
                        ConfigurationSection entry = sub.getConfigurationSection(key);
                        if (entry == null) continue;

                        String syntax = entry.getString("syntax");
                        String typeRaw = entry.getString("type", "AUTO");
                        SyntaxParser.Type type = SyntaxParser.Type.valueOf(typeRaw.toUpperCase());

                        if (!SyntaxParser.parse(syntax, type)) {
                            player.sendMessage("§cУсловие не выполнено: " + syntax);
                            allRequirementsPassed = false;
                            break;
                        }
                    }
                }
            }

            if (!allRequirementsPassed) {
                List<String> denyCommands = reqSection != null ? reqSection.getStringList("deny_commands") : Collections.emptyList();
                for (String cmd : denyCommands) {
                    runMainActions(player, cmd);
                }
                return;
            }

            List<String> actions = itemSection.getStringList(
                    isLeftClick ? "left_click_actions" :
                            isRightClick ? "right_click_actions" : null);

            for (String action : actions) {
                executeAction(player, action);
            }
        }
    }

    private void executeAction(Player player, String action) {

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
            if (!materials.isEmpty()) {
                for (Material material : materials) {
                    PlayerData playerData = database.getPlayerData(player.getUniqueId());

                    boolean currentState = playerData.isAutosell(material);
                    playerData.setAutosell(material, !currentState);

                    openMenu(player);
                }
            }
        }

        else {
            runMainActions(player, action);
        }
    }
}
