package my.reqqpe.rseller.configs.storage;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.models.item.ItemData;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

public class ItemStorage {

    private final Main plugin;

    public ItemStorage(Main plugin) {
        this.plugin = plugin;
    }

    public boolean save(Item item) {
        FileConfiguration config = plugin.getItemsConfig().getConfig();
        String base = "items." + item.id() + ".";

        config.set(base + "material", item.material().name());
        config.set(base + "price", item.price());
        config.set(base + "points", item.points());

        ItemData data = item.itemData();
        if (data != null) {
            if (data.name() != null && !data.name().isEmpty()) {
                config.set(base + "name", data.name());
            }
            if (data.lore() != null && !data.lore().isEmpty()) {
                config.set(base + "lore", data.lore());
            }
            if (data.hasModelData()) {
                config.set(base + "model-data", data.modelData());
            }
            if (data.enchantments() != null && !data.enchantments().isEmpty()) {
                for (Map.Entry<Enchantment, Integer> entry : data.enchantments().entrySet()) {
                    config.set(base + "enchantments." + entry.getKey().getKey().getKey(), entry.getValue());
                }
            }
            if (data.nbtTags() != null && !data.nbtTags().isEmpty()) {
                for (Map.Entry<NamespacedKey, Object> entry : data.nbtTags().entrySet()) {
                    config.set(base + "nbt-tags." + entry.getKey().toString(), entry.getValue());
                }
            }
        }

        plugin.getItemsConfig().saveConfig();
        plugin.getLogger().info(plugin.getMessageConfig().getConsoleItemSaved().replace("{id}", item.id()));
        return true;
    }

    public int saveBatch(List<Item> items, boolean overwrite) {
        FileConfiguration config = plugin.getItemsConfig().getConfig();
        int count = 0;

        for (Item item : items) {
            String base = "items." + item.id() + ".";
            if (!overwrite && config.contains("items." + item.id())) continue;

            config.set(base + "material", item.material().name());
            config.set(base + "price", item.price());
            config.set(base + "points", item.points());
            count++;
        }

        if (count > 0) {
            plugin.getItemsConfig().saveConfig();
        }

        return count;
    }

    public boolean delete(String id) {
        FileConfiguration config = plugin.getItemsConfig().getConfig();
        config.set("items." + id, null);
        plugin.getItemsConfig().saveConfig();
        plugin.getLogger().info(plugin.getMessageConfig().getConsoleItemDeleted().replace("{id}", id));
        return true;
    }
}
