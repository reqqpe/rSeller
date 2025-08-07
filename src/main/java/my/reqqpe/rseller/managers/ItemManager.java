package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.models.SearchItemSettings;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.models.item.ItemData;
import my.reqqpe.rseller.utils.Base64.Base64ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.logging.Logger;

public class ItemManager {


    private SearchItemSettings searchItemSettings;
    private final List<Item> items = new ArrayList<>();
    private final Map<Material, Set<Item>> materialItems = new HashMap<>();

    private final Main plugin;

    public ItemManager(Main plugin) {
        this.plugin = plugin;
        load();
    }







    public void load() {
        FileConfiguration config = plugin.getItemsConfig().getConfig();
        if (config == null) {
            plugin.getLogger().warning("Конфигурация предметов отсутствует. Загрузка пропущена.");
            return;
        }

        searchItemSettings = loadSearchItemSettings(config);

        items.clear();
        materialItems.clear();

        loadItems(config);
        plugin.getLogger().info("Загружено предметов: " + items.size());
    }

    private SearchItemSettings loadSearchItemSettings(FileConfiguration config) {
        var settingsSection = config.getConfigurationSection("settings-check");
        if (settingsSection == null) {
            return new SearchItemSettings(true, true, true, true, false);
        }
        boolean name = settingsSection.getBoolean("display-name", true);
        boolean lore = settingsSection.getBoolean("lore", true);
        boolean enchants = settingsSection.getBoolean("enchants", true);
        boolean modelData = settingsSection.getBoolean("model-data", true);
        boolean nbtTags = settingsSection.getBoolean("nbt-tags", false);
        if (nbtTags) {
            if (!Main.useNBTAPI) {
                plugin.getLogger().info("[ItemManager] не удалось найти nbt-api для параметра \"nbt-tags\"");
                nbtTags = false;
            }
        }
        return new SearchItemSettings(name, lore, enchants, modelData, nbtTags);
    }

    private void loadItems(FileConfiguration config) {
        var itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("Секция предметов отсутствует в конфигурации.");
            return;
        }

        var customItemsSection = config.getConfigurationSection("custom-items");

        for (String id : itemsSection.getKeys(false)) {
            double price = itemsSection.getDouble(id + ".price");
            double points = itemsSection.getDouble(id + ".points");
            if (price <= 0 && points <= 0) {
                plugin.getLogger().warning("Недопустимая цена или очки для предмета: " + id);
                continue;
            }

            Material material = Material.getMaterial(id);
            ItemData itemData = null;

            if (material == null && customItemsSection != null) {
                var customItem = customItemsSection.getConfigurationSection(id);
                if (customItem == null) {
                    plugin.getLogger().warning("Кастомный предмет не найден для ID: " + id);
                    continue;
                }

                material = Material.getMaterial(customItem.getString("material"));
                if (material == null) {
                    plugin.getLogger().warning("Недопустимый материал для кастомного предмета: " + id);
                    continue;
                }

                var itemDataSection = customItem.getConfigurationSection("item-data");
                if (itemDataSection != null) {
                    itemData = loadItemData(itemDataSection);
                }
            }

            if (material == null) {
                plugin.getLogger().warning("Нет допустимого материала для предмета: " + id);
                continue;
            }

            Item item = new Item(id, material, itemData, price, points);
            items.add(item);
            materialItems.computeIfAbsent(item.getMaterial(), k -> new HashSet<>()).add(item);
        }
    }

    private ItemData loadItemData(ConfigurationSection itemDataSection) {
        String name = itemDataSection.getString("name");
        List<String> lore = itemDataSection.getStringList("lore");
        int modelData = itemDataSection.getInt("model-data");

        Map<Enchantment, Integer> enchants = new HashMap<>();
        var enchantmentsSection = itemDataSection.getConfigurationSection("enchantments");
        if (enchantmentsSection != null) {
            for (String enchantmentString : enchantmentsSection.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentString));
                if (enchantment == null) {
                    plugin.getLogger().warning("Недопустимое зачарование: " + enchantmentString);
                    continue;
                }
                int level = enchantmentsSection.getInt(enchantmentString);
                if (level <= 0) {
                    plugin.getLogger().warning("Недопустимый уровень зачарования для " + enchantmentString + ": " + level);
                    continue;
                }
                enchants.put(enchantment, level);
            }
        }

        Map<NamespacedKey, Object> nbtTags = new HashMap<>();
        var nbtTagsSection = itemDataSection.getConfigurationSection("nbt-tags");
        if (nbtTagsSection != null) {
            for (String keyString : nbtTagsSection.getKeys(false)) {
                try {
                    NamespacedKey key = NamespacedKey.fromString(keyString);
                    if (key == null) {
                        plugin.getLogger().warning("Недопустимый ключ NBT: " + keyString);
                        continue;
                    }
                    Object value = nbtTagsSection.get(keyString);
                    if (value == null) {
                        plugin.getLogger().warning("Значение для ключа NBT " + keyString + " отсутствует");
                        continue;
                    }
                    nbtTags.put(key, value);
                } catch (Exception e) {
                    plugin.getLogger().warning("Ошибка при загрузке NBT-тега " + keyString + ": " + e.getMessage());
                }
            }
        }

        if (name == null && (lore == null || lore.isEmpty()) && modelData == 0 && enchants.isEmpty() && nbtTags.isEmpty()) {
            return null;
        }

        return new ItemData(name, lore, modelData, enchants, nbtTags);
    }


    public boolean addCustomItem(Item item) {
        Logger logger = plugin.getLogger();
        FileConfiguration config = plugin.getItemsConfig().getConfig();
        if (config == null) {
            logger.warning("[ItemManager] Не удалось создать предмет, отсутствует конфигурация");
            return false;
        }

        ConfigurationSection itemSection = config.getConfigurationSection("items");
        if (itemSection == null) {
            itemSection = config.createSection("items");
        }

        ConfigurationSection customItemSection = config.getConfigurationSection("custom-items");
        if (customItemSection == null) {
            customItemSection = config.createSection("custom-items");
        }

        String itemId = item.getId();
        if (itemSection.contains(itemId) || customItemSection.contains(itemId)) {
            logger.warning("[ItemManager] Не удалось создать предмет, предмет с ID " + itemId + " уже существует");
            return false;
        }

        if (item.getMaterial() == null) {
            logger.warning("[ItemManager] Не удалось создать предмет, материал не указан для ID " + itemId);
            return false;
        }
        if (item.getPrice() <= 0 && item.getPoints() <= 0) {
            logger.warning("[ItemManager] Не удалось создать предмет, недопустимая цена или очки для ID " + itemId);
            return false;
        }

        itemSection.set(itemId + ".price", item.getPrice());
        itemSection.set(itemId + ".points", item.getPoints());

        ConfigurationSection itemDataSection = customItemSection.createSection(itemId);
        itemDataSection.set("material", item.getMaterial().name());

        ItemData itemData = item.getItemData();
        if (itemData != null) {
            ConfigurationSection dataSection = itemDataSection.createSection("item-data");

            if (itemData.getName() != null) {
                dataSection.set("name", itemData.getName());
            }

            if (itemData.getLore() != null && !itemData.getLore().isEmpty()) {
                dataSection.set("lore", itemData.getLore());
            }

            if (itemData.getModelData() != 0) {
                dataSection.set("model-data", itemData.getModelData());
            }

            Map<Enchantment, Integer> enchantments = itemData.getEnchantments();
            if (enchantments != null && !enchantments.isEmpty()) {
                ConfigurationSection enchantsSection = dataSection.createSection("enchantments");
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    Enchantment enchantment = entry.getKey();
                    int level = entry.getValue();
                    if (level > 0) {
                        enchantsSection.set(enchantment.getKey().getKey(), level);
                    } else {
                        logger.warning("[ItemManager] Недопустимый уровень зачарования для " + enchantment.getKey().getKey() + " в предмете " + itemId);
                    }
                }
            }

            Map<NamespacedKey, Object> nbtTags = itemData.getNbtTags();
            if (nbtTags != null && !nbtTags.isEmpty()) {
                ConfigurationSection nbtTagsSection = dataSection.createSection("nbt-tags");
                for (Map.Entry<NamespacedKey, Object> entry : nbtTags.entrySet()) {
                    NamespacedKey key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof String || value instanceof Integer || value instanceof Double || value instanceof List) {
                        nbtTagsSection.set(key.toString(), value);
                    } else {
                        logger.warning("[ItemManager] Неподдерживаемый тип NBT-тега для ключа " + key + " в предмете " + itemId);
                    }
                }
            }
        }

        items.add(item);
        materialItems.computeIfAbsent(item.getMaterial(), k -> new HashSet<>()).add(item);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getItemsConfig().saveConfig();
                logger.info("[ItemManager] Предмет " + itemId + " успешно добавлен и сохранен");
            } catch (Exception e) {
                logger.severe("[ItemManager] Ошибка при сохранении конфигурации для предмета " + itemId + ": " + e.getMessage());
            }
        });

        return true;
    }



    public Item searchItem(ItemStack itemStack) {
        Set<Item> items = getItemsByMaterial(itemStack.getType());
        for (Item item : items) {
            if (item.matches(itemStack, searchItemSettings, plugin)) {
                return item;
            }
        }
        return null;
    }


    public Item getItemById(String id) {
        for (Item item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public Item getItemByMaterialAndId(Material material, String id) {
        Set<Item> items = materialItems.get(material);
        for (Item item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }


    public List<Item> getItems() {
        return new ArrayList<>(items);
    }


    public Set<Item> getItemsByMaterial(Material material) {
        Set<Item> items = materialItems.get(material);
        return items == null ? new HashSet<>() : new HashSet<>(items);
    }

    public boolean removeCustomItem(String id) {
        var customConfig = plugin.getItemsConfig();
        FileConfiguration config = customConfig.getConfig();
        ConfigurationSection customItemsSection = config.getConfigurationSection("custom-items");

        if (customItemsSection == null) {
            plugin.getLogger().warning("Секция custom-items отсутствует в конфиге!");
            return false;
        }

        if (!customItemsSection.contains(id)) {
            plugin.getLogger().warning("Предмет с id '" + id + "' отсутствует в custom-items!");
            return false;
        }

        customItemsSection.set(id, null);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, customConfig::saveConfig);
        return true;
    }
}
