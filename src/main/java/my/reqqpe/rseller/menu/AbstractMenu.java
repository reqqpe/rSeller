package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public abstract class AbstractMenu {

    protected final Main plugin;
    protected FileConfiguration guiConfig;
    protected Map<Integer, String> slotToItemId;
    protected Set<Integer> special_slots = new HashSet<>();



    public AbstractMenu(Main plugin) {
        this.plugin = plugin;
        this.slotToItemId = new HashMap<>();
    }
    protected abstract FileConfiguration getGuiConfig();
    public abstract String getMenuId();



    public void openMenu(Player player) {
        this.guiConfig = getGuiConfig();

        String title = Colorizer.color(guiConfig.getString("title", "Скупщик"));
        int size = guiConfig.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 54;
        }
        Inventory inv = Bukkit.createInventory(new CustomInventoryHolder(getMenuId()), size, title);

        loadItems(inv);

        player.openInventory(inv);
    }

    protected void loadItems(Inventory inv) {
        Set<Integer> usedSlots = new HashSet<>();
        List<Integer> specialSlots = parseSlotList(guiConfig.getStringList("special-slots"));
        this.special_slots = new HashSet<>(specialSlots);
        usedSlots.addAll(specialSlots);
        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");

        if (itemsSection != null) {
            for (String itemId : itemsSection.getKeys(false)) {
                String path = "items." + itemId;
                List<Integer> itemSlots = slotORslots(path);
                List<Integer> slotsToRemove = new ArrayList<>();
                List<Integer> validSlots = new ArrayList<>();

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
                    meta.setDisplayName(Colorizer.color(name));
                    List<String> lore = guiConfig.getStringList(path + ".lore");
                    if (lore != null && !lore.isEmpty()) {
                        meta.setLore(Colorizer.colorizeAll(lore));
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
                }
                usedSlots.addAll(validSlots);
            }
        }
    }




    protected List<Integer> slotORslots(String path) {
        List<Integer> slots = new ArrayList<>();


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

    protected static List<Integer> parseSlotList(List<String> list) {
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
}
