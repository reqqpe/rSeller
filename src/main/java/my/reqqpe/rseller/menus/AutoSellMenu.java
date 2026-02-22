package my.reqqpe.rseller.menus;

import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.database.DataBase;
import my.reqqpe.rseller.managers.AutoSellManager;
import my.reqqpe.rseller.managers.ItemManager;
import my.reqqpe.rseller.managers.MultiplierManager;
import my.reqqpe.rseller.models.*;
import my.reqqpe.rseller.utils.Colorizer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;


public class AutoSellMenu extends AbstractMenu{

    private final RSeller plugin;
    private final AutoSellManager autoSellManager;
    private final ItemManager itemManager;
    private final DataBase dataBase;
    private final MultiplierManager multiplierManager;

    private Map<UUID, String> playerCategory = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> playerCategoryPages = new HashMap<>();
    private Map<UUID, AutoSellSort> playerSortCategory = new HashMap<>();
    private AutoSellItem autoSellItem;

    public AutoSellMenu(FileConfiguration guiConfig, RSeller plugin, AutoSellManager autoSellManager, ItemManager itemManager, DataBase dataBase, MultiplierManager multiplierManager) {
        super(guiConfig, plugin);
        this.plugin = plugin;
        this.autoSellManager = autoSellManager;
        this.itemManager = itemManager;
        this.dataBase = dataBase;
        this.multiplierManager = multiplierManager;
    }


    @Override
    public String getMenuId() {
        return "AUTO_SELL_MENU";
    }


    @Override
    public void closeMenu(InventoryCloseEvent e) {
        super.closeMenu(e);
    }

    @Override
    protected void loadMenu() {
        ConfigurationSection section = guiConfig.getConfigurationSection("menu-settings");
        String displayName = section.getString("display_name", "&f{item_name}");
        List<String> lore = section.getStringList("lore");
        boolean autoSellEnchanted = section.getBoolean("autosell_enchanted", true);
        String enabled = section.getString("enabled");
        String disabled = section.getString("disabled");

        this.autoSellItem = new AutoSellItem(
                displayName,
                lore,
                autoSellEnchanted,
                enabled,
                disabled
        );

        super.loadMenu();
    }

    @Override
    public void openMenu(Player player) {
        super.openMenu(player);

        Bukkit.getScheduler().runTask(plugin, () -> {
            Inventory inv = player.getOpenInventory().getTopInventory();
            parseAutoSellItems(player, inv);
        });
    }


    private void parseAutoSellItems(Player player, Inventory inv) {
        String category = playerCategory.getOrDefault(player.getUniqueId(), autoSellManager.getDefaultCategory());
        List<SellableItem> pageItems = getItemsByPage(player, category, getPlayerPage(player, category));

        if (pageItems == null || pageItems.isEmpty()) return;

        PlayerData playerData = dataBase.getPlayerData(player.getUniqueId());

        for (int i = 0; i < menu.specialSlots().size(); i++) {
            int slot = menu.specialSlots().get(i);

            if (i >= pageItems.size()) {
                inv.setItem(slot, null);
                continue;
            }

            SellableItem item = pageItems.get(i);
            if (item == null) {
                inv.setItem(slot, null);
                continue;
            }

            ItemStack itemStack = item.toItemStack();
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;

            String itemName = item.displayName();

            double price = item.money();
            double points = item.points();

            String id = item.id();

            boolean autosell = playerData.isAutosell(id);

            String status = autosell ? autoSellItem.enabled : autoSellItem.disabled;

            String name = autoSellItem.name;
            boolean autosellEnchanted = autoSellItem.AutoSellEnchanted;
            List<String> lore = autoSellItem.lore;

            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {

                name = name
                        .replace("{item_name}", itemName)
                        .replace("{state_autosell}", status)
                        .replace("{sell_price}", String.valueOf(price))
                        .replace("{sell_points}", String.valueOf(points));

                meta.setDisplayName(Colorizer.colorLegacy(name));

                Enchantment glowEnchant = Enchantment.DURABILITY;
                if (autosellEnchanted && autosell) {
                    meta.addEnchant(glowEnchant, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    meta.removeEnchant(glowEnchant);
                    meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                Multiplier multiplier = multiplierManager.getMultiplierForPlayer(player);
                double multiplierPrice = price * multiplier.money();
                double multiplierPoints = points * multiplier.points();

                List<String> formattedLore = new ArrayList<>();
                for (String line : lore) {
                    line = line
                            .replace("{item_name}", itemName)
                            .replace("{state_autosell}", status)
                            .replace("{sell_price}", String.valueOf(price))
                            .replace("{sell_points}", String.valueOf(points))
                            .replace("{multiplier_price}", String.valueOf(multiplierPrice))
                            .replace("{multiplier_points}", String.valueOf(multiplierPoints));

                    line = replacePlaceholders(line, player, inv);
                    formattedLore.add(Colorizer.colorLegacy(line));
                }

                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "id"), PersistentDataType.STRING, id);

                meta.setLore(formattedLore);
                itemStack.setItemMeta(meta);
            }

            inv.setItem(slot, itemStack);
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


    private List<SellableItem> getItemsByPage(Player player,String category, int page) {
        AutoSellCategory autoSellCategory = autoSellManager.getAutoSellCategory(category);
        List<SellableItem> allItems = new ArrayList<>();
        if (autoSellCategory != null) {
            allItems = autoSellCategory.getItems(itemManager);
        }

        // СОРТИРОВКА
        allItems = applySort(allItems, getPlayerSort(player));

        int startIndex = (page - 1) * menu.specialSlots().size();
        int endIndex = Math.min(startIndex + menu.specialSlots().size(), allItems.size());

        if (startIndex >= allItems.size()) return List.of();

        return allItems.subList(startIndex, endIndex);
    }


    private int getTotalPages(String category) {
        AutoSellCategory autoSellCategory = autoSellManager.getAutoSellCategory(category);
        List<SellableItem> allItems = autoSellCategory.getItems(itemManager);
        if (allItems.isEmpty()) return 1;
        return (int) Math.ceil((double) allItems.size() / menu.specialSlots().size());
    }



    @Override
    protected void handleSpecialSlotsClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.id().equals(getMenuId())) return;

        int rawSlot = e.getRawSlot();
        Inventory menuInv = e.getInventory();

        if (menu.specialSlots().contains(rawSlot)) {
            e.setCancelled(true);
            ItemStack clickedItem = menuInv.getItem(rawSlot);
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                PlayerData playerData = dataBase.getPlayerData(e.getWhoClicked().getUniqueId());


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

                    parseAutoSellItems((Player) e.getWhoClicked(), menuInv);
                }
            }
        }
    }

    @Override
    protected void runCustomActions(Player player, ParsedAction pc) {
        if (pc == null) return;

        UUID uuid = player.getUniqueId();
        String currentCategory = playerCategory.getOrDefault(uuid, autoSellManager.getDefaultCategory());

        switch (pc.action()) {
            case "set_category": {
                String category = pc.data();
                if (category == null) break;
                if (category.equals(currentCategory)) break;

                AutoSellCategory autoSellCategory = autoSellManager.getAutoSellCategory(category);
                if (autoSellCategory == null) break;

                playerCategory.put(player.getUniqueId(), category);
                setPlayerPage(player, category, 1);

                openMenu(player);
                break;
            }
            case "switch_category": {
                int delta = 1;

                if (pc.data() != null) {
                    try {
                        delta = Integer.parseInt(pc.data());
                    } catch (NumberFormatException ex) {
                        //
                    }
                }

                String target = switchCategory(currentCategory, delta);
                playerCategory.put(uuid, target);
                setPlayerPage(player, target, 1);
                openMenu(player);
                break;
            }
            case "set_page": {
                int page = 1;

                if (pc.data() != null) {
                    try {
                        page = Integer.parseInt(pc.data());
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("set_page: Неверное значение страницы: " + pc.data());
                        break;
                    }
                }

                int total = getTotalPages(currentCategory);

                if (page < 1) page = 1;
                if (page > total) page = total;

                setPlayerPage(player, currentCategory, page);
                openMenu(player);
                break;
            }
            case "switch_page": {
                int delta = 1;

                if (pc.data() != null) {
                    try {
                        delta = Integer.parseInt(pc.data());
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("switch_page: Неверное значение сдвига страницы: " + pc.data());
                        break;
                    }
                }

                int total = getTotalPages(currentCategory);
                int currentPage = getPlayerPage(player, currentCategory);

                if (total <= 1) break;

                int nextPage = (currentPage - 1 + delta) % total;
                if (nextPage < 0) nextPage += total;

                nextPage += 1;

                setPlayerPage(player, currentCategory, nextPage);
                openMenu(player);
                break;
            }

            case "sort": {
                if (pc.data() == null) break;

                try {
                    AutoSellSort sort = AutoSellSort.valueOf(pc.data().toUpperCase());
                    setPlayerSort(player, sort);

                    setPlayerPage(player, currentCategory, 1);
                    openMenu(player);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Unknown sort: " + pc.data());
                }
                break;
            }

            case "switch_sort": {
                int delta = 1;

                if (pc.data() != null) {
                    try {
                        delta = Integer.parseInt(pc.data());
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("switch_sort: Неверное значение сдвига сортировки: " + pc.data());
                        break;
                    }
                }

                AutoSellSort[] values = AutoSellSort.values();
                if (values.length == 0) break;

                AutoSellSort current = getPlayerSort(player);
                int index = current.ordinal();

                int next = (index + delta) % values.length;
                if (next < 0) next += values.length;

                AutoSellSort nextSort = values[next];

                setPlayerSort(player, nextSort);

                setPlayerPage(player, currentCategory, 1);

                openMenu(player);
                break;
            }

            case "toggle_autosell_category": {
                AutoSellCategory category = autoSellManager.getAutoSellCategory(currentCategory);
                List<SellableItem> sellableItems = new ArrayList<>();
                if (category != null) {
                    sellableItems = category.getItems(itemManager);
                }
                boolean stateAutoSellCategory = getStateAutoSellCategory(player);

                if (!sellableItems.isEmpty()) {
                    PlayerData playerData = dataBase.getPlayerData(player.getUniqueId());
                    for (SellableItem sellableItem : sellableItems) {
                        playerData.setAutosell(sellableItem.id(), !stateAutoSellCategory);
                    }
                    openMenu(player);
                }
            }
        }

    }


    private boolean getStateAutoSellCategory(Player player) {
        UUID uuid = player.getUniqueId();
        String currentCategory = playerCategory.getOrDefault(uuid, autoSellManager.getDefaultCategory());
        AutoSellCategory category = autoSellManager.getAutoSellCategory(currentCategory);
        List<SellableItem> sellableItems = new ArrayList<>();

        if (category != null) {
            sellableItems = category.getItems(itemManager);
        }

        if (sellableItems.isEmpty()) {
            return false;
        }
        for (SellableItem sellableItem : sellableItems) {
            PlayerData pd = dataBase.getPlayerData(uuid);
            if (!pd.isAutosell(sellableItem.id())) {
                return false;
            }
        }
        return true;
    }



    private String switchCategory(String currentCategory, int delta) {
        List<String> list = autoSellManager.getCategoryIds();
        if (list.isEmpty()) return currentCategory;

        int index = list.indexOf(currentCategory);
        if (index == -1) return currentCategory;

        int size = list.size();

        index = (index + delta) % size;
        if (index < 0) index += size;

        return list.get(index);
    }


    @Override
    protected Map<String, String> buildLocalPlaceholders(Player player, Inventory inv) {
        Map<String, String> map = new HashMap<>(super.buildLocalPlaceholders(player, inv));

        UUID uuid = player.getUniqueId();
        String currentId = playerCategory.getOrDefault(uuid, autoSellManager.getDefaultCategory());

        List<String> categories = autoSellManager.getCategoryIds();
        if (categories == null || categories.isEmpty()) return map;

        int size = categories.size();
        int currentIndex = categories.indexOf(currentId);
        if (currentIndex == -1) currentIndex = 0;

        AutoSellCategory currentCategory = autoSellManager.getAutoSellCategory(currentId);
        map.put("current_category", currentCategory != null ? currentCategory.displayName() : currentId);

        map.put("current_sort", getPlayerSort(player).name());

        boolean state = getStateAutoSellCategory(player);
        map.put("autosell_category_state",
                state ? autoSellItem.enabled : autoSellItem.disabled);


        for (int i = -size; i <= size; i++) {
            if (i == 0) continue;
            int targetIndex = (currentIndex + i) % size;
            if (targetIndex < 0) targetIndex += size;

            String targetId = categories.get(targetIndex);
            AutoSellCategory targetCategory = autoSellManager.getAutoSellCategory(targetId);

            map.put("category_index:" + i, targetCategory != null ? targetCategory.displayName() : targetId);
        }

        for (String categoryId : categories) {
            AutoSellCategory cat = autoSellManager.getAutoSellCategory(categoryId);
            if (cat != null) {
                map.put("category_name:" + categoryId, cat.displayName());
            }
        }

        return map;
    }

    private AutoSellSort getPlayerSort(Player player) {
        return playerSortCategory.getOrDefault(
                player.getUniqueId(),
                AutoSellSort.NONE
        );
    }

    private void setPlayerSort(Player player, AutoSellSort sort) {
        playerSortCategory.put(player.getUniqueId(), sort);
    }

    private List<SellableItem> applySort(List<SellableItem> items, AutoSellSort sort) {
        if (sort == null || sort == AutoSellSort.NONE) {
            return items;
        }

        List<SellableItem> sorted = new ArrayList<>(items);

        switch (sort) {
            case MONEY_LOW_TO_HIGH ->
                    sorted.sort(Comparator.comparingDouble(SellableItem::money));

            case MONEY_HIGH_TO_LOW ->
                    sorted.sort(Comparator.comparingDouble(SellableItem::money).reversed());

            case POINTS_LOW_TO_HIGH ->
                    sorted.sort(Comparator.comparingDouble(SellableItem::points));

            case POINTS_HIGH_TO_LOW ->
                    sorted.sort(Comparator.comparingDouble(SellableItem::points).reversed());
        }

        return sorted;
    }


    private record AutoSellItem(String name, List<String> lore , boolean AutoSellEnchanted, String enabled, String disabled) {
        //
    }
}
