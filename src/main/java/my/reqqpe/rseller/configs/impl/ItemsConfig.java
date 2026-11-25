package my.reqqpe.rseller.configs.impl;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.configs.CustomConfig;
import my.reqqpe.rseller.models.SearchItemSettings;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.models.item.ItemData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

import java.util.*;

public class ItemsConfig extends CustomConfig {

    public ItemsConfig(Main plugin) {
        super(plugin, "items.yml");
    }

    private final List<Item> items = new ArrayList<>();
    private SearchItemSettings searchSettings;

    @Override
    protected void load() {
        items.clear();
        searchSettings = loadSettings();
        loadItems();
    }

    private SearchItemSettings loadSettings() {
        ConfigurationSection sec = config.getConfigurationSection("settings-check");
        if (sec == null) return new SearchItemSettings(true, true, true, true, false);

        return new SearchItemSettings(
                sec.getBoolean("display-name", true),
                sec.getBoolean("lore", true),
                sec.getBoolean("enchants", true),
                sec.getBoolean("model-data", true),
                sec.getBoolean("nbt-tags", false)
        );
    }

    private void loadItems() {
        ConfigurationSection itemsSec = config.getConfigurationSection("items");
        if (itemsSec == null) return;

        for (String id : itemsSec.getKeys(false)) {
            ConfigurationSection sec = itemsSec.getConfigurationSection(id);
            if (sec == null) continue;

            String materialName = sec.getString("material", id);
            Material material = Material.getMaterial(materialName.toUpperCase());
            if (material == null) {
                plugin.getLogger().warning("[Items] Unknown material '" + materialName + "' for item '" + id + "', skipping.");
                continue;
            }

            double price = sec.getDouble("price", 0);
            double points = sec.getDouble("points", 0);
            ItemData data = loadItemData(sec);

            items.add(new Item(id, material, data, price, points));
        }
    }

    private ItemData loadItemData(ConfigurationSection sec) {
        String name = sec.getString("name");
        List<String> lore = sec.getStringList("lore");
        Integer modelData = sec.contains("model-data") ? sec.getInt("model-data") : null;

        Map<Enchantment, Integer> enchantments = new HashMap<>();
        ConfigurationSection enchSec = sec.getConfigurationSection("enchantments");
        if (enchSec != null) {
            for (String key : enchSec.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key));
                if (enchantment != null) enchantments.put(enchantment, enchSec.getInt(key));
            }
        }

        Map<NamespacedKey, Object> nbtTags = new HashMap<>();
        ConfigurationSection nbtSec = sec.getConfigurationSection("nbt-tags");
        if (nbtSec != null) {
            for (String key : nbtSec.getKeys(false)) {
                NamespacedKey nsKey = NamespacedKey.fromString(key);
                if (nsKey != null) nbtTags.put(nsKey, nbtSec.get(key));
            }
        }

        boolean hasData = (name != null && !name.isEmpty())
                || !lore.isEmpty()
                || modelData != null
                || !enchantments.isEmpty()
                || !nbtTags.isEmpty();

        return hasData ? new ItemData(name, lore, modelData, enchantments, nbtTags) : null;
    }

    public List<Item> getItems() {
        return items;
    }

    public SearchItemSettings getSearchSettings() {
        return searchSettings;
    }
}
