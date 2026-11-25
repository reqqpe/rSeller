package my.reqqpe.rseller.configs.storage;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.models.item.ItemData;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemStorage {

    private final Main plugin;

    public ItemStorage(Main plugin) {
        this.plugin = plugin;
    }

    public boolean save(Item item) {
        FileConfiguration config = plugin.getItemsConfig().getConfig();
        String id = item.id();

        config.set("items." + id + ".price", item.price());
        config.set("items." + id + ".points", item.points());

        ItemData data = item.itemData();
        if (data != null) {
            String base = "custom-items." + id + ".";
            config.set(base + "material", item.material().name());

            String dataBase = base + "item-data.";

            if (data.name() != null && !data.name().isEmpty()) {
                config.set(dataBase + "name", data.name());
            }

            if (data.lore() != null && !data.lore().isEmpty()) {
                config.set(dataBase + "lore", data.lore());
            }

            if (data.hasModelData()) {
                config.set(dataBase + "model-data", data.modelData());
            }

            if (data.enchantments() != null && !data.enchantments().isEmpty()) {
                for (Map.Entry<Enchantment, Integer> entry : data.enchantments().entrySet()) {
                    config.set(dataBase + "enchantments." + entry.getKey().getKey().getKey(), entry.getValue());
                }
            }

            if (data.nbtTags() != null && !data.nbtTags().isEmpty()) {
                for (Map.Entry<NamespacedKey, Object> entry : data.nbtTags().entrySet()) {
                    config.set(dataBase + "nbt-tags." + entry.getKey().toString(), entry.getValue());
                }
            }
        }

        plugin.getItemsConfig().saveConfig();
        plugin.getLogger().info(plugin.getMessageConfig().getConsoleItemSaved().replace("{id}", id));
        return true;
    }

    public boolean delete(String id) {
        FileConfiguration config = plugin.getItemsConfig().getConfig();

        config.set("items." + id, null);
        config.set("custom-items." + id, null);

        plugin.getItemsConfig().saveConfig();
        plugin.getLogger().info(plugin.getMessageConfig().getConsoleItemDeleted().replace("{id}", id));
        return true;
    }
}
