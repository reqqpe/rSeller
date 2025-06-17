package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.model.ItemData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AutoSellManager {

    private final Main plugin;
    private final ItemManager itemManager;

    private boolean whitelistEnabled;
    private int whitelistPriority;
    private Set<Material> whitelist;

    private boolean blacklistEnabled;
    private int blacklistPriority;
    private Set<Material> blacklist;

    private final Map<String, List<ItemStack>> categories = new HashMap<>();
    private final Map<String, String> categoryNames = new HashMap<>();

    public AutoSellManager(Main plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    public void loadConfig() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("autosell");
        if (config == null) {
            plugin.getLogger().warning("[AutoSellManager] Секция autosell отсутствует в config.yml!");
            return;
        }

        this.whitelistEnabled = config.getBoolean("whitelist.enable", false);
        this.whitelistPriority = config.getInt("whitelist.priority", 1);
        this.whitelist = parseMaterialList(config.getStringList("whitelist.list"));

        this.blacklistEnabled = config.getBoolean("blacklist.enable", false);
        this.blacklistPriority = config.getInt("blacklist.priority", 0);
        this.blacklist = parseMaterialList(config.getStringList("blacklist.list"));

        categories.clear();
        categoryNames.clear();

        ConfigurationSection section = config.getConfigurationSection("categories");
        if (section == null) {
            plugin.getLogger().warning("В конфиге отсутствует раздел 'autosell.categories'");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection catSection = section.getConfigurationSection(key);
            if (catSection == null) continue;

            String name = catSection.getString("name", key);
            categoryNames.put(key, name);

            List<String> itemList = catSection.getStringList("blocks");
            List<ItemStack> items = new ArrayList<>();

            if (itemList.contains("all")) {
                for (ItemData item : itemManager.getAllItems()) {
                    if (isAllowed(item.getItem().getType())) {
                        items.add(item.getItem().clone());
                    }
                }
            } else {
                for (String item : itemList) {
                    item = item.toLowerCase();
                    ItemData itemData = itemManager.getById(item);
                    if (itemData != null) {
                        items.add(itemData.getItem().clone());
                    } else {
                        try {
                            Material material = Material.valueOf(item.toUpperCase());
                            for (ItemData stack : itemManager.getAllItems()) {
                                if (stack.getItem().getType() == material && isAllowed(material)) {
                                    items.add(stack.getItem().clone());
                                }
                            }
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Недопустимый предмет или материал в категории '" + key + "': " + item);
                        }
                    }
                }
            }

            categories.put(key, items);
        }
    }

    private Set<Material> parseMaterialList(List<String> list) {
        Set<Material> result = new HashSet<>();
        if (list.contains("ALL")) {
            result.addAll(Arrays.asList(Material.values()));
        } else if (!list.contains("NONE")) {
            for (String name : list) {
                try {
                    result.add(Material.valueOf(name.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("[AutoSellManager] Неверное имя предмета в конфиге: " + name);
                }
            }
        }
        return result;
    }

    public boolean isAllowed(Material material) {
        boolean inWhitelist = whitelist.contains(material) || whitelist.contains(Material.valueOf("ALL"));
        boolean inBlacklist = blacklist.contains(material) || blacklist.contains(Material.valueOf("ALL"));

        if (whitelistPriority > blacklistPriority) {
            return whitelistEnabled && inWhitelist && (!blacklistEnabled || !inBlacklist);
        } else {
            return blacklistEnabled && !inBlacklist && (!whitelistEnabled || inWhitelist);
        }
    }

    public boolean isSellable(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (!isAllowed(itemStack.getType())) return false;

        ItemData item = itemManager.getByItemStack(itemStack);
        if (item == null) {
            plugin.getLogger().info("ItemManager.getByItemStack вернул null для ItemStack: " + itemStack.getType().name());
        }
        return item != null;
    }

    public List<ItemStack> getAllAllowedItems() {
        List<ItemStack> result = new ArrayList<>();
        for (ItemData item : itemManager.getAllItems()) {
            if (isAllowed(item.getItem().getType())) {
                result.add(item.getItem().clone());
            }
        }
        return result;
    }

    public List<ItemStack> getSellableItems() {
        return getAllAllowedItems();
    }

    public List<ItemStack> getCategory(String category) {
        return categories.getOrDefault(category, Collections.emptyList());
    }

    public Map<String, List<ItemStack>> getCategories() {
        return Collections.unmodifiableMap(categories);
    }

    public String getCategoryName(String id) {
        return categoryNames.getOrDefault(id, id);
    }

    public String getFirstCategory() {
        String firstCategory = plugin.getConfig().getString("autosell.start_category", "all");
        if (getCategories().containsKey(firstCategory)) {
            return firstCategory;
        }
        return getCategories().keySet().stream().findFirst().orElse("all");
    }
}
