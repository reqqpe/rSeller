package my.reqqpe.rseller.menu;


import it.unimi.dsi.fastutil.ints.IntList;
import me.clip.placeholderapi.PlaceholderAPI;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.managers.NumberFormatManager;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.logging.LogRecord;

public class AutoSellMenu extends AbstractMenu implements Listener {
    private final Database database;
    private final AutoSellManager autoSellManager;
    private final Map<UUID, String> playerCategory = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> playerCategoryPages = new HashMap<>();
    private int totalSlots;
    private final NumberFormatManager numberFormatManager;
    private autoSellItem autoSellItem;


    public AutoSellMenu(Main plugin, Database database) {
        super(plugin);
        this.database = database;
        this.autoSellManager = plugin.getAutoSellManager();
        this.numberFormatManager = plugin.getFormatManager();


        String enabled = Colorizer.color(plugin.getConfig().getString("messages.autosell-enable", "&aВключено"));
        String disabled = Colorizer.color(plugin.getConfig().getString("messages.autosell-disable", "&cВыключено"));

        String name = getGuiConfig().getString("autosell_item.name", "&f{item_name}");
        boolean autosellEnchanted = getGuiConfig().getBoolean("autosell_item.autosell-enchanted", true);
        List<String> lore = getGuiConfig().getStringList("autosell_item.lore");

        this.autoSellItem = new autoSellItem(enabled, disabled, name, lore, autosellEnchanted);
    }

    private record autoSellItem(String enabled, String disable, String name, List<String> lore, boolean autosellEnchanted) {
    }

    @Override
    protected FileConfiguration getGuiConfig() {
        return plugin.getAutoSellGUIConfig().getConfig();
    }


    @Override
    protected String getMenuId() {
        return "autoSellGUI";
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

        IntList specialSlots = parseSlotList(guiConfig.getStringList("special-slots"));
        this.totalSlots = specialSlots.size();


        parseAutoSellItems(player, inv);

    }


    private void parseAutoSellItems(Player player, Inventory inv) {
        String category = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        List<Item> pageItems = getItemsByPage(category, getPlayerPage(player, category));

        if (pageItems == null || pageItems.isEmpty()) return;

        PlayerData playerData = database.getPlayerData(player.getUniqueId());

        for (int i = 0; i < specialSlots.size(); i++) {
            int slot = specialSlots.get(i);

            if (i >= pageItems.size()) {
                inv.setItem(slot, null);
                continue;
            }

            Item item = pageItems.get(i);
            if (item == null) {
                inv.setItem(slot, null);
                continue;
            }

            ItemStack itemStack = item.getItemStack(plugin);
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;

            String itemName = item.getDisplayName(plugin);

            double price = item.price();
            double points = item.points();

            String id = item.id();

            boolean autosell = playerData.isAutosell(id);

            String status = autosell ? autoSellItem.enabled : autoSellItem.disable;

            String name = autoSellItem.name;
            boolean autosellEnchanted = autoSellItem.autosellEnchanted;
            List<String> lore = autoSellItem.lore;

            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                String formattedPrice = numberFormatManager.format("autoSellGUI.sell_price", price);
                String formattedPoints = numberFormatManager.format("autoSellGUI.sell_points", points);

                name = name
                        .replace("{item_name}", itemName)
                        .replace("{state_autosell}", status)
                        .replace("{sell_price}", formattedPrice)
                        .replace("{sell_points}", formattedPoints);

                meta.setDisplayName(Colorizer.color(replacePlaceholders(player, name, inv)));

                Enchantment glowEnchant = Enchantment.DURABILITY;
                if (autosellEnchanted && autosell) {
                    meta.addEnchant(glowEnchant, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    meta.removeEnchant(glowEnchant);
                    meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                List<String> formattedLore = new ArrayList<>();
                for (String line : lore) {
                    line = line
                            .replace("{item_name}", itemName)
                            .replace("{state_autosell}", status)
                            .replace("{sell_price}", formattedPrice)
                            .replace("{sell_points}", formattedPoints);

                    formattedLore.add(Colorizer.color(replacePlaceholders(player, line, inv)));
                }

                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "id"), PersistentDataType.STRING, id);

                meta.setLore(formattedLore);
                itemStack.setItemMeta(meta);
            }

            inv.setItem(slot, itemStack);
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

            String status = state ? autoSellItem.enabled : autoSellItem.disable;

            text = text.replace("{state_autosell_category}", status);
        }

        text = PlaceholderAPI.setPlaceholders(player, text);

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


    private List<Item> getItemsByPage(String category, int page) {
        List<Item> allItems = autoSellManager.getCategoryItems(category);

        int startIndex = (page - 1) * totalSlots;
        int endIndex = Math.min(startIndex + totalSlots, allItems.size());

        if (startIndex >= allItems.size()) return List.of();

        return allItems.subList(startIndex, endIndex);
    }


    private int getTotalPages(String category) {
        List<Item> items = autoSellManager.getCategoryItems(category);
        if (items.isEmpty()) return 1;
        return (int) Math.ceil((double) items.size() / totalSlots);
    }


    @Override
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        super.handleClick(player, e);
    }


    @Override
    protected boolean handleSpecialSlotClick(Player player, InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return false;
        if (!holder.id().equals(getMenuId())) return false;

        int rawSlot = e.getRawSlot();
        Inventory menuInv = e.getInventory();

        if (specialSlots.contains(rawSlot)) {
            e.setCancelled(true);
            ItemStack clickedItem = menuInv.getItem(rawSlot);
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                PlayerData playerData = database.getPlayerData(player.getUniqueId());


                String id = "";
                if (clickedItem.hasItemMeta()) {
                    id = clickedItem
                            .getItemMeta()
                            .getPersistentDataContainer()
                            .get(new NamespacedKey(plugin, "id"),
                                    PersistentDataType.STRING);
                }

                if (id != null && !id.isEmpty()) {
                    boolean currentState = playerData.isAutosell(id);
                    playerData.setAutosell(id, !currentState);

                    parseAutoSellItems(player, menuInv);
                    return true;
                }
            }
        }
        return false;
    }


    protected void executeAction(Player player, String action) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.id().equals(getMenuId())) return;


        switch (action.toLowerCase()) {
            case "[next_page]" -> switchPage(player, 1);
            case "[prev_page]" -> switchPage(player, -1);
            case "[next_category]" -> switchCategory(player, 1);
            case "[prev_category]" -> switchCategory(player, -1);

            case "[toggle_autosell_category]" -> {
                String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
                List<Item> items = autoSellManager.getCategoryItems(currentCategory);

                boolean autosellCategory = getCategoryAutosellState(player);

                if (!items.isEmpty()) {
                    PlayerData playerData = database.getPlayerData(player.getUniqueId());
                    for (Item item : items) {
                        playerData.setAutosell(item.id(), !autosellCategory);
                    }
                    openMenu(player);
                }
            }

            default -> runMainActions(player, action);
        }
    }

    private boolean getCategoryAutosellState(Player player) {
        String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        List<Item> items = autoSellManager.getCategoryItems(currentCategory);
        if (items.isEmpty()) return false;


        PlayerData playerData = database.getPlayerData(player.getUniqueId());

        for (Item item : items) {
            if (!playerData.isAutosell(item.id())) {
                return false;
            }
        }
        return true;
    }

    private void switchCategory(Player player, int direction) {
        List<String> categoryIds = new ArrayList<>(autoSellManager.getCategories().keySet());
        if (categoryIds.isEmpty()) return;

        String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        int currentIndex = categoryIds.indexOf(currentCategory);
        if (currentIndex == -1) currentIndex = 0;

        int nextIndex = (currentIndex + direction + categoryIds.size()) % categoryIds.size();
        String newCategory = categoryIds.get(nextIndex);

        playerCategory.put(player.getUniqueId(), newCategory);
        setPlayerPage(player, newCategory, 1);
        openMenu(player);
    }

    private void switchPage(Player player, int direction) {
        String currentCategory = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getFirstCategory());
        int currentPage = getPlayerPage(player, currentCategory);
        int totalPages = getTotalPages(currentCategory);

        int newPage = currentPage + direction;

        if (newPage >= 1 && newPage <= totalPages) {
            setPlayerPage(player, currentCategory, newPage);
            openMenu(player);
        }
    }
}
