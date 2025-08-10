package my.reqqpe.rseller.models.item;


import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.models.SearchItemSettings;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Logger;

import static de.tr7zw.nbtapi.NBTType.NBTTagDouble;


@Getter
@AllArgsConstructor
public class Item {
    private final String id;
    private final Material material;
    private final ItemData itemData;
    private final double price;
    private final double points;


    // Знаю кастыльно и лучше сделать это загружаемым в ItemData, но мне лень
    public String getDisplayName(Main plugin) {
        if (itemData != null && itemData.getName() != null && !itemData.getName().isEmpty()) {
            return itemData.getName();
        }
        String configName = plugin.getItemsConfig().getConfig().getString("items." + id + ".name");
        if (configName != null && !configName.isEmpty()) {
            return configName;
        }

        return material.name().toLowerCase().replace("_", " ");
    }


    public ItemStack getItemStack(Main plugin) {
        Logger logger = plugin.getLogger();
        ItemStack itemStack = new ItemStack(material);
        if (itemData == null) {
            return itemStack;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return itemStack;
        }

        if (itemData.getName() != null) {
            itemMeta.setDisplayName(itemData.getName());
        }
        if (itemData.getLore() != null && !itemData.getLore().isEmpty()) {
            itemMeta.setLore(itemData.getLore());
        }
        if (itemData.getModelData() != 0) {
            itemMeta.setCustomModelData(itemData.getModelData());
        }

        Map<Enchantment, Integer> enchantments = itemData.getEnchantments();
        if (enchantments != null && !enchantments.isEmpty()) {
            enchantments.forEach((enchantment, level) -> {
                if (level > 0) {
                    itemMeta.addEnchant(enchantment, level, true);
                }
            });
        }

        itemStack.setItemMeta(itemMeta);
        if (Main.useNBTAPI && (itemData.getNbtTags() != null && !itemData.getNbtTags().isEmpty())) {
            try {
                NBTItem nbtItem = new NBTItem(itemStack);
                for (Map.Entry<NamespacedKey, Object> entry : itemData.getNbtTags().entrySet()) {
                    NamespacedKey key = entry.getKey();
                    Object value = entry.getValue();
                    String keyString = key.toString();
                    if (value instanceof String) {
                        nbtItem.setString(keyString, (String) value);
                    } else if (value instanceof Integer) {
                        nbtItem.setInteger(keyString, (Integer) value);
                    } else if (value instanceof Double) {
                        nbtItem.setDouble(keyString, (Double) value);
                    } else if (value instanceof List) {
                        nbtItem.setObject(keyString, value);
                    } else {
                        logger.warning("Неподдерживаемый тип NBT-тега для ключа " + key + " в предмете " + id);
                    }
                }
                itemStack = nbtItem.getItem();
            } catch (Exception e) {
                logger.warning("Ошибка при применении NBT-тегов для предмета " + id + ": " + e.getMessage());
            }
        }

        return itemStack;
    }

    public boolean matches(ItemStack itemStack, SearchItemSettings searchItemSettings, Main plugin) {
        Logger logger = plugin.getLogger();

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }

        if (itemStack.getType() != material) {
            return false;
        }

        if (!searchItemSettings.hasAnyEnabled()) {
            return true;
        }

        if (itemData == null) {
            return true;
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if ((itemData != null) && (itemMeta == null)) {
            return false;
        }

        if (searchItemSettings.name() && !Objects.equals(itemData.getName(), itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : null)) {
            return false;
        }

        if (searchItemSettings.lore()) {
            List<String> expectedLore = itemData.getLore() != null ? itemData.getLore() : Collections.emptyList();
            List<String> actualLore = itemMeta.hasLore() ? itemMeta.getLore() : Collections.emptyList();
            if (!Objects.equals(expectedLore, actualLore)) {
                return false;
            }
        }

        if (searchItemSettings.modelData() && itemData.getModelData() != (itemMeta.hasCustomModelData() ? itemMeta.getCustomModelData() : 0)) {
            return false;
        }

        if (searchItemSettings.enchants() && !Objects.equals(itemData.getEnchantments(), itemMeta.getEnchants())) {
            return false;
        }

        if (searchItemSettings.nbtTags() && Main.useNBTAPI) {
            try {
                NBTItem nbtItem = new NBTItem(itemStack);
                Map<NamespacedKey, Object> expectedNbtTags = itemData.getNbtTags();
                Set<String> actualKeys = nbtItem.getKeys();

                if (expectedNbtTags != null && !expectedNbtTags.isEmpty()) {
                    for (Map.Entry<NamespacedKey, Object> entry : expectedNbtTags.entrySet()) {
                        NamespacedKey key = entry.getKey();
                        String keyString = key.toString();
                        Object expectedValue = entry.getValue();

                        if (!nbtItem.hasKey(keyString)) {
                            return false;
                        }

                        Object actualValue;
                        if (expectedValue instanceof String) {
                            actualValue = nbtItem.getString(keyString);
                        } else if (expectedValue instanceof Integer) {
                            actualValue = nbtItem.getInteger(keyString);
                        } else if (expectedValue instanceof Double) {
                            actualValue = nbtItem.getDouble(keyString);
                        } else if (expectedValue instanceof List) {
                            actualValue = nbtItem.getObject(keyString, List.class);
                        } else {
                            logger.warning("Неподдерживаемый тип NBT-тега для ключа " + keyString + " в предмете " + id);
                            return false;
                        }

                        if (!Objects.equals(expectedValue, actualValue)) {
                            return false;
                        }
                    }

                    if (actualKeys.size() > expectedNbtTags.size()) {
                        return false;
                    }
                } else if (!actualKeys.isEmpty()) {
                    return false;
                }
            } catch (Exception e) {
                logger.severe("Ошибка при проверке NBT-тегов: " + e.getMessage());
                return false;
            }
        }

        return true;
    }

    public static Item fromItemStack(String id, ItemStack itemStack, double price, double points, Main plugin) {
        Logger logger = plugin.getLogger();

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        Material material = itemStack.getType();
        ItemData itemData = null;

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            String name = itemMeta.hasDisplayName() ? itemMeta.getDisplayName() : null;
            List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : null;
            int modelData = itemMeta.hasCustomModelData() ? itemMeta.getCustomModelData() : 0;
            Map<Enchantment, Integer> enchantments = itemMeta.getEnchants();

            Map<NamespacedKey, Object> nbtTags = new HashMap<>();
            if (Main.useNBTAPI) {
                try {
                    NBTItem nbtItem = new NBTItem(itemStack);
                    Set<String> keys = nbtItem.getKeys();
                    for (String keyString : keys) {
                        NamespacedKey key = NamespacedKey.fromString(keyString);
                        if (key == null) {
                            logger.warning("Недопустимый ключ NBT: " + keyString + " для предмета " + id);
                            continue;
                        }

                        if (nbtItem.hasTag(keyString, NBTType.NBTTagString)) {
                            nbtTags.put(key, nbtItem.getString(keyString));
                        } else if (nbtItem.hasTag(keyString, NBTType.NBTTagInt)) {
                            nbtTags.put(key, nbtItem.getInteger(keyString));
                        } else if (nbtItem.hasTag(keyString, NBTTagDouble)) {
                            nbtTags.put(key, nbtItem.getDouble(keyString));
                        } else if (nbtItem.hasTag(keyString, NBTType.NBTTagList)) {
                            nbtTags.put(key, nbtItem.getObject(keyString, List.class));
                        } else {
                            logger.warning("Неподдерживаемый тип NBT-тега для ключа " + keyString + " в предмете " + id);
                        }
                    }
                } catch (Exception e) {
                    logger.warning("Ошибка при получении NBT-тегов для предмета " + id + ": " + e.getMessage());
                }
            }
            if (name != null || lore != null || modelData != 0 || !enchantments.isEmpty() || !nbtTags.isEmpty()) {
                itemData = new ItemData(name, lore, modelData, enchantments, nbtTags);
            }
        }

        if (price < 0 || points < 0) {
            logger.warning("Недопустимые значения: price=" + price + ", points=" + points + " для предмета " + id);
            return null;
        }

        return new Item(id, material, itemData, price, points);
    }
}
