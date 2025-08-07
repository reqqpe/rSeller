package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.models.item.Item;
import org.bukkit.configuration.ConfigurationSection;


import java.util.*;

public class AutoSellManager {

    private final Main plugin;

    private final Map<String, List<Item>> categories = new HashMap<>();
    private final Map<String, String> categoryNames = new HashMap<>();

    public AutoSellManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    public void loadConfig() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("autosell");
        if (config == null) {
            plugin.getLogger().warning("[AutoSellManager] Секция autosell отсутствует в config.yml!");
            return;
        }

        categories.clear();
        categoryNames.clear();

        var section = plugin.getConfig().getConfigurationSection("autosell.categories");
        if (section == null) return;

        for (String category : section.getKeys(false)) {
            var categorySection = section.getConfigurationSection(category);
            if (categorySection == null) continue;

            String categoryName = categorySection.getString("name", category);
            categoryNames.put(category, categoryName);

            List<String> itemsListConfig = new ArrayList<>();

            if (categorySection.contains("items")) {
                itemsListConfig.addAll(categorySection.getStringList("items"));
            } else {
                if (categorySection.contains("blocks")) {
                    itemsListConfig.addAll(categorySection.getStringList("blocks"));
                }
            }

            categories.put(category, parseItemList(itemsListConfig));
        }
    }


    private List<Item> parseItemList(List<String> list) {
        List<Item> items = new ArrayList<>();
        for (String id : list) {
            if (id.equalsIgnoreCase("all")) {
                items.addAll(plugin.getItemManager().getItems());
                break;
            } else {
                Item item = plugin.getItemManager().getItemById(id);
                if (item == null) {
                    plugin.getLogger().warning("[AutoSellManager] Неверный ID или Material в списке: " + id);
                    continue;
                }
                items.add(item);
            }

        }
        return items;
    }


    public Map<String, List<Item>> getCategories() {
        return Collections.unmodifiableMap(categories);
    }


    public String getCategoryName(String id) {
        return categoryNames.getOrDefault(id, id);
    }


    public String getFirstCategory() {
        String firstCategory = plugin.getConfig().getString("autosell.start_category", "all");
        if (categories.containsKey(firstCategory)) {
            return firstCategory;
        }
        return null;
    }


    public List<Item> getCategoryItems(String category) {
        return categories.getOrDefault(category, Collections.emptyList());
    }
}