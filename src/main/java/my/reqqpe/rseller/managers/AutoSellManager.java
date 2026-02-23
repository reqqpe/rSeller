package my.reqqpe.rseller.managers;

import lombok.Getter;
import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.models.AutoSellCategory;
import my.reqqpe.rseller.models.AutoSellSort;
import my.reqqpe.rseller.models.SellableItem;
import my.reqqpe.rseller.utils.LoggerUtil;
import my.reqqpe.rseller.utils.MessageUtil;
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

        LoggerUtil.info(MessageUtil.getString("log.loading"));

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("auto-sell");
        if (section == null) {
            LoggerUtil.warn(MessageUtil.getString("log.section-missing"));
            return;
        }

        this.enabled = section.getBoolean("enable", true);
        if (!this.enabled) {
            LoggerUtil.info(MessageUtil.getString("log.disabled"));
            return;
        }

        ConfigurationSection categoriesSection = section.getConfigurationSection("categories");
        if (categoriesSection == null) {
            LoggerUtil.warn(MessageUtil.getString("log.categories-missing"));
            return;
        }

        for (String categoryId : categoriesSection.getKeys(false)) {
            ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);

            if (categorySection == null) {
                LoggerUtil.warn(
                        MessageUtil.getString("log.category-invalid")
                                .replace("{category}", categoryId)
                );
                continue;
            }

            String confDisplayName = categorySection.getString("display_name");
            String displayName = confDisplayName != null ? confDisplayName : categoryId;

            List<String> confListItems = categorySection.getStringList("items");
            List<String> listItems = new ArrayList<>();

            for (String itemId : confListItems) {
                SellableItem item = itemManager.getSellableItemById(itemId);

                if (item == null) {
                    LoggerUtil.warn(
                            MessageUtil.getString("log.item-invalid")
                                    .replace("{item}", itemId)
                                    .replace("{category}", categoryId)
                    );
                    continue;
                }

                listItems.add(itemId);
            }

            if (listItems.isEmpty()) {
                LoggerUtil.warn(
                        MessageUtil.getString("log.category-empty")
                                .replace("{category}", categoryId)
                );
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
            LoggerUtil.warn(MessageUtil.getString("log.no-categories"));
            return;
        }

        this.defaultCategory = section.getString("default_category");

        String sortName = section.getString("default_sort", "NONE");
        try {
            this.defaultSort = AutoSellSort.valueOf(sortName.toUpperCase());
        } catch (Exception e) {
            LoggerUtil.warn(
                    MessageUtil.getString("log.invalid-sort")
                            .replace("{value}", sortName)
            );
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
