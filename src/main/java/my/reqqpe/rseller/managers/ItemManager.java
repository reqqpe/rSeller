package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.configs.impl.ItemsConfig;
import my.reqqpe.rseller.models.SearchItemSettings;
import my.reqqpe.rseller.models.item.Item;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemManager {

    private final Map<String, Item> byId = new HashMap<>();
    private final Map<Material, Set<Item>> byMaterial = new EnumMap<>(Material.class);

    private final ItemsConfig config;

    public ItemManager(ItemsConfig config) {
        this.config = config;
        reload();
    }

    public void reload() {

        byId.clear();
        byMaterial.clear();

        for (Item item : config.getItems()) {
            register(item);
        }
    }

    public void register(Item item) {
        byId.put(item.id(), item);
        byMaterial.computeIfAbsent(item.material(), k -> new HashSet<>()).add(item);
    }

    public Item getById(String id) {
        return byId.get(id);
    }

    public Set<Item> getByMaterial(Material material) {
        return byMaterial.getOrDefault(material, Set.of());
    }

    public List<Item> getAll() {
        return new ArrayList<>(byId.values());
    }

    public Item search(ItemStack stack) {

        Set<Item> items = getByMaterial(stack.getType());
        SearchItemSettings settings = config.getSearchSettings();

        for (Item item : items) {
            if (item.matches(stack, settings, null)) {
                return item;
            }
        }
        return null;
    }
}