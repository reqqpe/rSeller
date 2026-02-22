package my.reqqpe.rseller.managers;

import lombok.Getter;
import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.models.AutoSellCategory;
import my.reqqpe.rseller.models.AutoSellSort;
import my.reqqpe.rseller.models.SellableItem;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class AutoSellManager {

    private final RSeller plugin;
    private final ItemManager itemManager;
    private Map<String, AutoSellCategory> categories = new LinkedHashMap<>();;
    private Set<String> idsItemsInCategories = new HashSet<>();
    @Getter
    private String defaultCategory;
    @Getter
    private AutoSellSort defaultSort;
    @Getter
    private boolean enabled;

    public AutoSellManager(RSeller plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;

        loadAutoSell();
    }

    public void loadAutoSell() {
        categories.clear();
        idsItemsInCategories.clear();

        plugin.getLogger().info("Загрузка авто-продажи...");

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("auto-sell");
        if (section == null) {
            plugin.getLogger().warning("Отсутствует секция авто-продажи. Загрузка прекращена");
            return;
        }

        this.enabled = section.getBoolean("enable", true);
        if (!this.enabled) {
            plugin.getLogger().info("Авто-продажа отключена. Загрузка прекращена");
            return;
        }

        ConfigurationSection categoriesSection = section.getConfigurationSection("categories");
        if (categoriesSection == null) {
            plugin.getLogger().warning("Отсутствуют категории авто-продажи. Загрузка прекращена");
            return;
        }
        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);
            if (categorySection == null) {
                plugin.getLogger().warning(String.format("Ошибка загрузки категории: %s. Отсутствует её конфигурация", categoryId));
                continue;
            }
            String confDisplayName = categorySection.getString("display_name");
            String displayName = confDisplayName != null ? confDisplayName : categoryId;

            List<String> confListItems = categorySection.getStringList("items");
            List<String> listItems = new ArrayList<>();
            for (String itemId : confListItems) {
                SellableItem item = itemManager.getSellableItemById(itemId);
                if (item == null) {
                    plugin.getLogger().warning(String.format("Ошибка загрузки предмета: '%s', в категории: '%s'", itemId, categoryId));
                    continue;
                }
                listItems.add(itemId);
            }
            if (listItems.isEmpty()) {
                plugin.getLogger().warning(String.format("В категории '%s', нет ни одного предмета.", categoryId));
                continue;
            }

            AutoSellCategory autoSellCategory = new AutoSellCategory(
                    displayName,
                    listItems
            );

            categories.put(categoryId, autoSellCategory);
            idsItemsInCategories.addAll(listItems);
        }

        if (categories.isEmpty()) {
            plugin.getLogger().warning("Отсутствуют категории авто-продажи. Загрузка прекращена");
            return;
        }

        this.defaultCategory = section.getString("default_category");
        String sortName = section.getString("default_sort", "NONE");
        try {
            this.defaultSort = AutoSellSort.valueOf(sortName.toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().warning("Неверный default_sort, используется NONE");
            this.defaultSort = AutoSellSort.NONE;
        }
    }
    public AutoSellCategory getAutoSellCategory(String id) {
        return categories.get(id);
    }

    public List<String> getCategoryIds() {
        return new ArrayList<>(categories.keySet());
    }
}
