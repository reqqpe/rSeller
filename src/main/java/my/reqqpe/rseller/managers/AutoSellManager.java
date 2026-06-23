package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.models.item.Item;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class AutoSellManager {

    private final Main plugin;

    private final Map<String, List<Item>> categories = new HashMap<>();
    private final Map<String, String> categoryNames = new HashMap<>();
    private final Set<String> categoriesItemIds = new HashSet<>();

    public AutoSellManager(Main plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection config = plugin.getMainConfig().getConfig().getConfigurationSection("autosell");
        if (config == null) {
            plugin.getLogger().warning(plugin.getMessageConfig().getConsoleAutosellSectionMissing());
            return;
        }

        categories.clear();
        categoryNames.clear();
        categoriesItemIds.clear();

        var section = config.getConfigurationSection("categories");
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

            List<Item> items = parseItemList(itemsListConfig);

            categories.put(category, items);
            for (Item item : items) {
                categoriesItemIds.add(item.id());
            }
        }
    }


    private List<Item> parseItemList(List<String> list) {
        List<Item> items = new ArrayList<>();
        for (String id : list) {
            if (id.equalsIgnoreCase("all")) {
                items.addAll(plugin.getItemManager().getAll());
                break;
            } else {
                Item item = plugin.getItemManager().getById(id);
                if (item == null) {
                    plugin.getLogger().warning(plugin.getMessageConfig().getConsoleAutosellInvalidItem().replace("{id}", id));
                    continue;
                }
                items.add(item);
            }

        }
        return items;
    }


    public Map<String, List<Item>> getCategories() {
        return new HashMap<>(categories);
    }


    public String getCategoryName(String id) {
        return categoryNames.getOrDefault(id, id);
    }


    public String getFirstCategory() {
        String firstCategory = plugin.getMainConfig().getAutosell().getStartCategory();
        if (categories.containsKey(firstCategory)) {
            return firstCategory;
        }
        return null;
    }

    public boolean hasCategory(String category) {
        return categories.containsKey(category);
    }

    public List<Item> getCategoryItems(String category) {
        return categories.getOrDefault(category, List.of());
    }

    public boolean isInAnyCategory(Item item) {
        return categoriesItemIds.contains(item.id());
    }

    public boolean isInAnyCategory(String itemId) {
        return categoriesItemIds.contains(itemId);
    }

}