package my.reqqpe.rseller.menu;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.HeadUtil;
import my.reqqpe.rseller.utils.SyntaxParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public abstract class AbstractMenu {


    protected final Main plugin;
    protected FileConfiguration guiConfig;
    protected final Int2ObjectMap<String> slotToItemId = new Int2ObjectOpenHashMap<>();
    protected IntList specialSlots = new IntArrayList();
    protected final Int2ObjectMap<UpdatableItem> updatableItems = new Int2ObjectOpenHashMap<>();
    private final Map<UUID, List<BukkitTask>> updateTasks = new HashMap<>();


    public AbstractMenu(Main plugin) {
        this.plugin = plugin;
    }


    protected abstract FileConfiguration getGuiConfig();

    protected abstract String getMenuId();


    public void openMenu(Player player) {
        this.guiConfig = getGuiConfig();

        String title = Colorizer.color(guiConfig.getString("title", "Скупщик"));
        int size = guiConfig.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 54;
        }
        Inventory inv = Bukkit.createInventory(new CustomInventoryHolder(getMenuId()), size, title);

        loadItems(inv, player);

        player.openInventory(inv);
        startItemUpdates(player, inv);
    }


    protected void loadItems(Inventory inv, Player player) {
        IntList specialSlots = parseSlotList(guiConfig.getStringList("special-slots"));
        this.specialSlots = new IntArrayList(specialSlots);
        Set<Integer> usedSlots = new HashSet<>(specialSlots);
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");

        if (itemsSection != null) {
            for (String itemId : itemsSection.getKeys(false)) {
                String path = "items." + itemId;
                IntList itemSlots = slotORslots(path);
                IntList slotsToRemove = new IntArrayList();
                IntList validSlots = new IntArrayList();

                for (int slot : itemSlots) {
                    if (slot < 0 || slot >= inv.getSize()) {
                        slotsToRemove.add(slot);
                        plugin.getLogger().warning(String.format("[%s] Пропущен слот %d для предмета %s: слот выходит за пределы инвентаря (0-%d).",
                                getMenuId(), slot, itemId, inv.getSize() - 1));
                        continue;
                    }
                    if (specialSlots.contains(slot) || usedSlots.contains(slot)) {
                        slotsToRemove.add(slot);
                        plugin.getLogger().warning(String.format("[%s] Пропущен слот %d для предмета %s: слот уже занят специальным слотом или другим предметом.",
                                getMenuId(), slot, itemId));
                        continue;
                    }
                    validSlots.add(slot);
                }

                itemSlots.removeAll(slotsToRemove);

                if (validSlots.isEmpty()) {
                    plugin.getLogger().warning(String.format("[%s] Нет доступных слотов для предмета %s.", getMenuId(), itemId));
                    continue;
                }

                String matString = guiConfig.getString(path + ".material", "STONE");
                ItemStack item;

                if (matString.toLowerCase().startsWith("basehead-")) {
                    String base64 = matString.substring("basehead-".length());
                    item = HeadUtil.getCustomHead(base64);
                } else {
                    Material material = Material.matchMaterial(matString);
                    if (material == null) {
                        plugin.getLogger().warning(String.format("[%s] Неизвестный материал для предмета %s: %s", getMenuId(), itemId, matString));
                        continue;
                    }
                    item = new ItemStack(material);
                }

                int customModelData = -1;
                if (guiConfig.contains(path + ".model-data")) {
                    customModelData = guiConfig.getInt(path + ".model-data");
                }

                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String name = guiConfig.getString(path + ".name", "");
                    meta.setDisplayName(Colorizer
                            .color
                                    (replacePlaceholders(player, name, inv)));

                    List<String> lore = guiConfig.getStringList(path + ".lore");
                    if (!lore.isEmpty()) {
                        meta.setLore(Colorizer
                                .colorizeAll
                                        (replacePlaceholders(player, lore, inv)));
                    }
                    if (customModelData != -1) {
                        meta.setCustomModelData(customModelData);
                    }

                    item.setItemMeta(meta);
                }

                ConfigurationSection enchantsSection = guiConfig.getConfigurationSection(path + ".enchants");
                if (enchantsSection != null) {
                    for (String enchantName : enchantsSection.getKeys(false)) {
                        try {
                            int level = enchantsSection.getInt(enchantName);
                            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                            if (enchantment == null) {
                                plugin.getLogger().warning(String.format("[%s] Неизвестное зачарование для предмета %s: %s",
                                        getMenuId(), itemId, enchantName));
                                continue;
                            }
                            if (level <= 0) {
                                plugin.getLogger().warning(String.format("[%s] Некорректный уровень зачарования для предмета %s: %d (должен быть больше 0)",
                                        getMenuId(), itemId, level));
                                continue;
                            }
                            item.addUnsafeEnchantment(enchantment, level);
                        } catch (Exception e) {
                            plugin.getLogger().warning(String.format("[%s] Ошибка при добавлении зачарования %s:%d для предмета %s: %s",
                                    getMenuId(), enchantName, enchantsSection.getInt(enchantName, -1), itemId, e.getMessage()));
                        }
                    }
                }

                for (int slot : validSlots) {
                    inv.setItem(slot, item);
                    slotToItemId.put(slot, itemId);

                    if (guiConfig.getBoolean(path + ".update", false)) {
                        String name = guiConfig.getString(path + ".name", "");
                        List<String> lore = guiConfig.getStringList(path + ".lore");
                        updatableItems.put(slot, new UpdatableItem(slot, name, lore));
                    }
                }
                usedSlots.addAll(validSlots);
            }
        }
    }


    protected void runMainActions(Player player, String cmdLine) {
        String cmd = cmdLine.trim();
        if (cmd.startsWith("[console] ")) {
            String command = cmd.substring(10).replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else if (cmd.startsWith("[player] ")) {
            String command = cmd.substring(9).replace("%player%", player.getName());
            player.performCommand(command);
        } else if (cmd.startsWith("[text] ")) {
            String message = Colorizer.color(cmd.substring(7).replace("%player%", player.getName()));
            player.sendMessage(message);
        } else if (cmd.startsWith("[opengui] ")) {
            String guiId = cmd.substring(10).trim();
            if (guiId.equalsIgnoreCase("autoSellGUI")) {
                plugin.getAutoSellMenu().openMenu(player);
            } else if (guiId.equalsIgnoreCase("allSellGUI")) {
                plugin.getAllSellMenu().openMenu(player);
            } else if (guiId.equalsIgnoreCase("mainGUI")) {
                plugin.getMainMenu().openMenu(player);
            }
        } else if (cmd.startsWith("[sound] ")) {
            String[] parts = cmd.substring(8).split(";");
            if (parts.length >= 1) {
                try {
                    Sound sound = Sound.valueOf(parts[0].toUpperCase());
                    float volume = parts.length >= 2 ? Float.parseFloat(parts[1]) : 1.0f;
                    float pitch = parts.length >= 3 ? Float.parseFloat(parts[2]) : 1.0f;
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Неизвестный звук: " + parts[0]);
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при воспроизведении звука.");
                    e.printStackTrace();
                }
            }
        } else {
            player.sendMessage(Colorizer.color(cmd));
        }
    }


    protected String replacePlaceholders(Player player, String text, Inventory inventory) {
        return text;
    }


    protected List<String> replacePlaceholders(Player player, List<String> list, Inventory inventory) {

        List<String> result = new ArrayList<>();
        for (String text : list) {
            result.add(replacePlaceholders(player, text, inventory));
        }

        return result;
    }


    public void handleClick(Player player, InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.id().equals(getMenuId())) return;

        int rawSlot = e.getRawSlot();
        Inventory clickedInv = e.getClickedInventory();
        Inventory menuInv = e.getInventory();


        if (clickedInv == menuInv && rawSlot < menuInv.getSize()) {
            if (specialSlots.contains(rawSlot)) {
                if (handleSpecialSlotClick(player, e)) return;
            }

            e.setCancelled(true);

            String itemId = slotToItemId.get(rawSlot);
            if (itemId == null) return;

            ConfigurationSection itemSection = guiConfig.getConfigurationSection("items." + itemId);
            if (itemSection == null) return;

            boolean isLeftClick = e.isLeftClick();
            boolean isRightClick = e.isRightClick();

            if (!isLeftClick && !isRightClick) {
                return;
            }

            ConfigurationSection reqSection = itemSection.getConfigurationSection(
                    isLeftClick ? "left_click_requaments" :
                            "right_click_requaments"
            );

            boolean allRequirementsPassed = true;

            if (reqSection != null) {
                ConfigurationSection sub = reqSection.getConfigurationSection("requaments");
                if (sub != null) {
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
                List<String> denyCommands = reqSection.getStringList("deny_commands");
                for (String cmd : denyCommands) {
                    runMainActions(player, cmd);
                }
                return;
            }

            List<String> actions = itemSection.getStringList(
                    isLeftClick ? "left_click_actions" :
                            "right_click_actions"
            );

            for (String action : actions) {
                executeAction(player, action);
            }
        }
    }

    protected boolean handleSpecialSlotClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        return false;
    }


    protected void executeAction(Player player, String action) {

        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.id().equals(getMenuId())) return;

        runMainActions(player, action);
    }


    protected void startItemUpdates(Player player, Inventory inv) {
        int interval = guiConfig.getInt("update_interval", -1);
        if (interval <= 0 || updatableItems.isEmpty()) return;

        List<BukkitTask> tasks = new ArrayList<>();

        for (Map.Entry<Integer, UpdatableItem> entry : updatableItems.entrySet()) {
            int slot = entry.getKey();
            UpdatableItem itemData = entry.getValue();

            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) return;
                if (!player.getOpenInventory().getTopInventory().equals(inv)) return;

                ItemStack item = inv.getItem(slot);
                if (item == null || item.getType() == Material.AIR) return;

                ItemMeta meta = item.getItemMeta();
                boolean changed = false;

                String newName = Colorizer.color(replacePlaceholders(player, itemData.name(), inv));
                if (!newName.equals(meta.hasDisplayName() ? meta.getDisplayName() : "")) {
                    meta.setDisplayName(newName);
                    changed = true;
                }

                List<String> newLore = Colorizer.colorizeAll(replacePlaceholders(player, itemData.lore(), inv));
                if (!newLore.equals(meta.hasLore() ? meta.getLore() : new ArrayList<>())) {
                    meta.setLore(newLore);
                    changed = true;
                }

                if (changed) {
                    item.setItemMeta(meta);
                    inv.setItem(slot, item);
                    player.updateInventory();
                }

            }, interval, interval);

            tasks.add(task);
        }

        cancelItemUpdates(player);
        updateTasks.put(player.getUniqueId(), tasks);
    }


    protected void cancelItemUpdates(Player player) {
        List<BukkitTask> tasks = updateTasks.remove(player.getUniqueId());
        if (tasks != null) {
            tasks.forEach(BukkitTask::cancel);
        }
    }


    protected IntList slotORslots(String path) {
        IntList slots = new IntArrayList();


        int slot = guiConfig.getInt(path + ".slot", -1);
        if (slot != -1) {
            slots.add(slot);
            return slots;
        }

        if (guiConfig.isList(path + ".slots")) {
            List<?> slotsConfig = guiConfig.getList(path + ".slots");
            if (!slotsConfig.isEmpty() && slotsConfig.get(0) instanceof String) {

                slots.addAll(parseSlotList((List<String>) slotsConfig));
            } else {
                slots.addAll(guiConfig.getIntegerList(path + ".slots"));
            }
            return slots;
        }

        return slots;
    }


    protected static IntList parseSlotList(List<String> list) {
        IntList result = new IntArrayList();
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


    public record UpdatableItem(int slot, String name, List<String> lore) {
    }
}
