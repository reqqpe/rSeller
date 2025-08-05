package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.model.Item;
import my.reqqpe.rseller.model.ItemData;
import my.reqqpe.rseller.utils.Base64.Base64ItemStack;
import my.reqqpe.rseller.utils.ItemChecker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemManager {

    private ItemChecker itemChecker;
    private final List<Item> items = new ArrayList<>();
    private final Map<Material, Set<Item>> materialItems = new HashMap<>();

    private final Main plugin;

    public ItemManager(Main plugin) {
        this.plugin = plugin;
        load();
    }


    public void load() {

        FileConfiguration config = plugin.getItemsConfig().getConfig();
        if (config == null) return;

        var settingsSection = config.getConfigurationSection("settings-check");
        if (settingsSection != null) {
            boolean name = settingsSection.getBoolean("display-name", true);
            boolean lore = settingsSection.getBoolean("lore", true);
            boolean durability = settingsSection.getBoolean("durability", false);
            boolean enchants = settingsSection.getBoolean("enchants", true);
            boolean modelData = settingsSection.getBoolean("model-data", true);
            boolean nbtTags = settingsSection.getBoolean("nbt-tags", false);

            itemChecker = new ItemChecker(name, lore, durability, enchants, modelData, nbtTags);
        } else
            itemChecker = new ItemChecker(true,true , false, true, true, false);


        if (!items.isEmpty()) {
            items.clear();
        }
        if (!materialItems.isEmpty()) {
            materialItems.clear();
        }

        Map<String, ItemStack> customItems = new HashMap<>();
        var customItemsSection = config.getConfigurationSection("custom-items");
        if (customItemsSection != null) {
            for (String id : customItemsSection.getKeys(false)) {
                String base64 = customItemsSection.getString(id + ".itemstack");

                if (base64 == null) continue;

                ItemStack item = Base64ItemStack.decode(base64);
                if (item != null) {
                    customItems.put(id, item);
                }
            }
        }

        var itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String id : itemsSection.getKeys(false)) {
                double price = itemsSection.getDouble(id + ".price", 0);
                double points = itemsSection.getDouble(id + ".points", 0);
                String name = itemsSection.getString(id + ".name", id);

                ItemStack itemStack = null;

                if (customItems.containsKey(id)) {
                    itemStack = customItems.get(id);
                } else {
                    Material material = Material.matchMaterial(id);
                    if (material != null) {
                        itemStack = new ItemStack(material);
                    }
                }


                if (itemStack == null) continue;
                ItemData itemData = new ItemData(price, points, name);
                Item item = new Item(id, itemStack, itemData);
                items.add(item);
                materialItems.computeIfAbsent(itemStack.getType(), k -> new HashSet<>()).add(item);
            }
        }

        plugin.getLogger().info("Загружено предметов: " + items.size());
    }


    public Item searchItem(ItemStack itemStack) {
        Set<Item> items = getItemsByMaterial(itemStack.getType());
        for (Item item : items) {
            if (itemChecker.isSimilarCustom(itemStack, item.getItem())) {
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


    public boolean addConfigCustomItem(String id, double price, double points, String name) {

        var customConfig = plugin.getItemsConfig();
        FileConfiguration config = customConfig.getConfig();

        ConfigurationSection customItemsSection = config.getConfigurationSection("custom-items");
        if (customItemsSection == null) {
            plugin.getLogger().warning("Секция custom-items отсутствует в конфиге!");
            return false;
        }

        if (!customItemsSection.contains(id)) {
            plugin.getLogger().warning("Предмет с id '" + id + "' отсутствует в секции custom-items!");
            return false;
        }

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            itemsSection = config.createSection("items");
        }

        ConfigurationSection itemSection;
        if (!itemsSection.contains(id)) {
            itemSection = itemsSection.createSection(id);
        } else {
            itemSection = itemsSection.getConfigurationSection(id);
        }

        itemSection.set("price", price);
        itemSection.set("points", points);
        if (name != null && !name.isEmpty()) {
            itemSection.set("name", name);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, customConfig::saveConfig);

        return true;
    }


    public boolean addCustomItem(String id, ItemStack itemStack) {
        var customConfig = plugin.getItemsConfig();
        FileConfiguration config = customConfig.getConfig();
        ConfigurationSection customItemsSection = config.getConfigurationSection("custom-items");

        if (customItemsSection == null) {
            plugin.getLogger().warning("Секция custom-items отсутствует в конфиге! Пытаюсь создать");
            customItemsSection = config.createSection("custom-items");
        }

        if (customItemsSection.contains(id)) {
            plugin.getLogger().warning("Предмет с id '" + id + "' уже существует в custom-items!");
            return false;
        }

        customItemsSection.set(id + ".itemstack", Base64ItemStack.encode(itemStack));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, customConfig::saveConfig);
        return true;
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
