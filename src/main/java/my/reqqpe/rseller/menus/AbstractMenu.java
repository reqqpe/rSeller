package my.reqqpe.rseller.menus;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.clip.placeholderapi.PlaceholderAPI;
import my.reqqpe.rseller.managers.MenuManager;
import my.reqqpe.rseller.models.Menu;
import my.reqqpe.rseller.models.MenuItem;
import my.reqqpe.rseller.models.ParsedAction;
import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.MessageUtils;
import my.reqqpe.rseller.utils.Parse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// TODO: Сделать обновляемые предметы, а так же плейсхолдеры(как локальные, так и PlaceholderAPI)
public abstract class AbstractMenu {

    protected FileConfiguration guiConfig;
    protected final JavaPlugin plugin;
    protected Menu menu;
    protected final Map<Player, Inventory> viewers = new HashMap<>();
    private int updateTaskId = -1;

    public AbstractMenu(FileConfiguration guiConfig, JavaPlugin plugin) {
        this.guiConfig = guiConfig;
        this.plugin = plugin;
        loadMenu();
    }


    public abstract String getMenuId();

    protected void loadMenu() {
        String title = guiConfig.getString("title", "Меню");
        int size = guiConfig.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 54;
        }

        IntList specialSlots = (Parse.StringListToIntList(guiConfig.getStringList("special-slots")));
        String openPermission = guiConfig.getString("open-permission", null);


        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning(String.format("[%s] Не удалось найти предметы в секции items", getMenuId()));
            return;
        }

        Map<Integer, MenuItem> items = new HashMap<>();

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            Map<Integer, MenuItem> loadedItems = loadMenuItem(itemSection, specialSlots, size, items);
            items.putAll(loadedItems);
        }
        menu = new Menu(
                getMenuId(),
                title,
                size,
                openPermission,
                specialSlots,
                items,
                null
        );
    }

    public void closeMenu(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        viewers.remove(player);
        stopUpdateTaskIfNeeded();
    }

    public void openMenu(Player player) {
        if (menu == null) {
            plugin.getLogger().warning("Не удалось открыть меню %s, оно равняется null");
            player.sendMessage("Не удалось открыть меню, сообщите администратору");
            return;
        }

        Inventory inv = Bukkit.createInventory(
                new CustomInventoryHolder(getMenuId()),
                menu.size(),
                Colorizer.colorLegacy(menu.title())
        );

        for (Map.Entry<Integer, MenuItem> entry : menu.menuItems().entrySet()) {
            inv.setItem(
                    entry.getKey(),
                    entry.getValue().toItemStack(s -> replacePlaceholders(s, player, inv))
            );
        }

        viewers.put(player, inv);
        startUpdateTask();

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent e) {
        int rawSlot = e.getRawSlot();

        Inventory clickedInv = e.getClickedInventory();
        Inventory menuInv = e.getInventory();

        if (clickedInv == menuInv && rawSlot < menuInv.getSize()) {
            if (menu.specialSlots().contains(rawSlot)) {
                handleSpecialSlotsClick(e);
                return;
            }

            e.setCancelled(true);
            MenuItem item = menu.menuItems().get(rawSlot);
            if (item == null) return;

            boolean isLeftClick = e.isLeftClick();
            boolean isRightClick = e.isRightClick();

            if (!isLeftClick && !isRightClick) return;

            List<String> actions = isRightClick ? item.leftActions() : item.rightActions();

            Player player = (Player) e.getWhoClicked();

            for (String cmdLine : actions) {
                runDefaultActions(player, cmdLine);
            }
        } else {
            handlePlayerInventoryClick(e);
        }
    }

    protected void handleSpecialSlotsClick(InventoryClickEvent e) {
        e.setCancelled(true);
    }
    protected void handlePlayerInventoryClick(InventoryClickEvent e) {
        e.setCancelled(true);
    }

    protected Map<Integer, MenuItem> loadMenuItem(ConfigurationSection itemSection, IntList specialSlots, int size, Map<Integer, MenuItem> existingItems) {
        Map<Integer, MenuItem> result = new HashMap<>();

        if (itemSection == null) return result;

        String materialStr = itemSection.getString("material", "STONE").toUpperCase();
        Material material;
        String base64 = null;

        if (materialStr.toLowerCase().startsWith("basehead-")) {
            material = Material.PLAYER_HEAD;
            base64 = materialStr.substring("basehead-".length());
        } else {
            material = Material.getMaterial(materialStr);
            if (material == null) {
                plugin.getLogger().warning("Неизвестный материал: " + materialStr);
                return result;
            }
        }

        String name = itemSection.getString("name", null);
        List<String> lore = itemSection.getStringList("lore");
        int modelData = itemSection.getInt("model_data", -1);

        Map<Enchantment, Integer> enchants = new HashMap<>();
        ConfigurationSection enchantsSection = itemSection.getConfigurationSection("enchants");
        if (enchantsSection != null) {
            for (String enchantName : enchantsSection.getKeys(false)) {
                Enchantment enchant = Enchantment.getByName(enchantName.toUpperCase());
                if (enchant != null) {
                    enchants.put(enchant, enchantsSection.getInt(enchantName, 1));
                } else {
                    plugin.getLogger().warning("Неизвестный энчант '" + enchantName + "' для предмета " + itemSection.getName());
                }
            }
        }

        IntList slots = new IntArrayList();

        if (itemSection.contains("slots")) {
            IntList confSlots = Parse.StringListToIntList(itemSection.getStringList("slots"));
            for (int confSlot : confSlots) {
                if (confSlot < 0 || confSlot >= size) continue;
                if (specialSlots.contains(confSlot)) continue;
                if (existingItems.containsKey(confSlot)) {
                    plugin.getLogger().warning(String.format("Слот %d уже занят, предмет '%s' пропущен в этом слоте", confSlot, itemSection.getName()));
                    continue;
                }
                slots.add(confSlot);
            }
        }

        if (itemSection.contains("slot")) {
            int confSlot = itemSection.getInt("slot");
            if (confSlot >= 0 && confSlot < size &&
                    !specialSlots.contains(confSlot) &&
                    !existingItems.containsKey(confSlot)) {
                slots.add(confSlot);
            } else if (existingItems.containsKey(confSlot)) {
                plugin.getLogger().warning(String.format("Слот %d уже занят, предмет '%s' пропущен в этом слоте", confSlot, itemSection.getName()));
            }
        }

        List<String> rawFlags = itemSection.getStringList("item_flags");
        List<String> finalFlags = new ArrayList<>();

        for (String flagName : rawFlags) {
            String normalized = flagName.toUpperCase();
            try {
                ItemFlag flag = ItemFlag.valueOf(normalized);
                if (finalFlags.contains(normalized)) {
                    plugin.getLogger().warning(String.format("Флаг '%s' дублируется для предмета '%s', добавлен только один раз", normalized, itemSection.getName()));
                    continue;
                }
                finalFlags.add(normalized);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(String.format("Флаг '%s' не существует для предмета '%s', пропущен", normalized, itemSection.getName()));
            }
        }

        List<String> rightActions = itemSection.getStringList("right_click_actions");
        List<String> leftActions = itemSection.getStringList("left_click_actions");


        boolean updatable = itemSection.getBoolean("updatable", false);

        MenuItem menuItem = new MenuItem(
                name,
                modelData,
                material,
                lore,
                enchants,
                base64,
                finalFlags,
                rightActions,
                leftActions,
                updatable
        );

        for (int slot : slots) {
            result.put(slot, menuItem);
        }

        return result;
    }


    protected Map<String, String> buildLocalPlaceholders(Player player, Inventory inv) {
        return new HashMap<>();
    }

    protected String replacePlaceholders(String string, Player player, Inventory inv) {

        Map<String, String> local = buildLocalPlaceholders(player, inv);
        for (Map.Entry<String, String> entry : local.entrySet()) {
            string = string.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return PlaceholderAPI.setPlaceholders(player, string);
    }


    protected List<String> replacePlaceholders(List<String> lines, Player player, Inventory inv) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            line = replacePlaceholders(line, player, inv);
            result.add(line);
        }
        return result;
    }


    private void runDefaultActions(Player player, String cmdLine) {
        ParsedAction pc = ParsedAction.parse(cmdLine);
        String action = pc.action();
        String data = pc.data().replace("%player_name%", player.getName());
        switch (action) {
            case "console": {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), data);
                break;
            }
            case "player": {
                Bukkit.dispatchCommand(player, data);
                break;
            }
            case "openguimenu": {
                AbstractMenu openGUI = MenuManager.getMenu(data);
                if (openGUI == null) {
                    plugin.getLogger().warning("Меню не найдено: " + data);
                    return;
                }
                openGUI.openMenu(player);
                break;
            }
            case "message": {
                data = MessageUtils.replacePlaceholders(player, data, new HashMap<>());
                MessageUtils.sendMessage(player, data);
                break;
            }
            case "sound": {
                String[] parts = data.split(";");
                try {
                    Sound sound = Sound.valueOf(parts[0].toUpperCase());
                    float volume = parts.length >= 2 ? Float.parseFloat(parts[1]) : 1.0f;
                    float pitch  = parts.length >= 3 ? Float.parseFloat(parts[2]) : 1.0f;
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка звука: " + data);
                }
                break;
            }
            case "close": {
                player.closeInventory();
                break;
            }
            default: {
                runCustomActions(player, pc);
            }
        }
    }

    protected void startUpdateTask() {
        if (updateTaskId != -1) return;

        updateTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin,
                this::updateViewers,
                20L,
                20L
        );
    }

    protected void updateViewers() {
        if (viewers.isEmpty()) return;

        for (Map.Entry<Player, Inventory> entry : viewers.entrySet()) {
            Player player = entry.getKey();
            Inventory inv = entry.getValue();

            if (!player.isOnline()) continue;

            for (Map.Entry<Integer, MenuItem> itemEntry : menu.menuItems().entrySet()) {
                MenuItem menuItem = itemEntry.getValue();
                if (!menuItem.updatable()) continue;

                inv.setItem(
                        itemEntry.getKey(),
                        menuItem.toItemStack(s -> replacePlaceholders(s, player, inv))
                );
            }
        }
    }
    protected void stopUpdateTaskIfNeeded() {
        if (!viewers.isEmpty()) return;

        if (updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;
        }
    }

    protected void runCustomActions(Player player, ParsedAction pc) {
    }
}
