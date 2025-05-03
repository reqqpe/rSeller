package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ItemManager {
    private final Map<Material, ItemInfo> itemMap = new HashMap<>();

    public ItemManager(Main plugin) {
        FileConfiguration config = plugin.getItemsConfig().getConfig(); // items.yml
        for (String key : config.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) continue;

            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            double price = section.getDouble("price");
            int points = section.getInt("points");

            itemMap.put(material, new ItemInfo(price, points));
        }
    }

    public boolean isSellable(Material material) {
        return itemMap.containsKey(material);
    }

    public ItemInfo getInfo(Material material) {
        return itemMap.get(material);
    }

    public Set<Material> getAllSellableMaterials() {
        return itemMap.keySet();
    }
    public record ItemInfo(double price, int points) {}
}

