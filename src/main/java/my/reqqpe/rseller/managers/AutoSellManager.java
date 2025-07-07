package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.stream.Collectors;

public class AutoSellManager {

    private final Main plugin;

    private boolean whitelistEnabled;
    private int whitelistPriority;
    private Set<Material> whitelist;

    private boolean blacklistEnabled;
    private int blacklistPriority;
    private Set<Material> blacklist;
    private final Map<String, List<Material>> categories = new HashMap<>();
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

        this.whitelistEnabled = config.getBoolean("whitelist.enable", false);
        this.whitelistPriority = config.getInt("whitelist.priority", 1);
        this.whitelist = parseMaterialList(config.getStringList("whitelist.list"));

        this.blacklistEnabled = config.getBoolean("blacklist.enable", false);
        this.blacklistPriority = config.getInt("blacklist.priority", 0);
        this.blacklist = parseMaterialList(config.getStringList("blacklist.list"));



        categories.clear();
        categoryNames.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("autosell.categories");
        if (section == null) {
            plugin.getLogger().warning("В конфиге отсутствует раздел 'autosell.categories'");
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection catSection = section.getConfigurationSection(key);
            if (catSection == null) continue;

            String name = catSection.getString("name", key);
            categoryNames.put(key, name);

            List<String> blockList = catSection.getStringList("blocks");
            List<Material> materials = new ArrayList<>();

            if (blockList.stream().anyMatch(b -> b.equalsIgnoreCase("all"))) {
                materials.addAll(getSellableMaterials());
            } else {
                for (String block : blockList) {
                    try {
                        Material mat = Material.valueOf(block.toUpperCase());
                        materials.add(mat);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Категория '" + key + "' содержит недопустимый материал: " + block);
                    }
                }
            }

            categories.put(key, materials);
        }

    }

    private Set<Material> parseMaterialList(List<String> list) {
        Set<Material> result = new HashSet<>();
        if (list.contains("ALL")) {
            result.addAll(Arrays.asList(Material.values()));
        } else {
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
        boolean inWhitelist = whitelist.contains(material);
        boolean inBlacklist = blacklist.contains(material);

        if (whitelistPriority > blacklistPriority) {
            return whitelistEnabled && inWhitelist && (!blacklistEnabled || !inBlacklist);
        } else {
            return blacklistEnabled && !inBlacklist && (!whitelistEnabled || inWhitelist);
        }
    }

    public boolean isSellable(Material material) {
        return isAllowed(material) &&
                plugin.getItemsConfig().getConfig().isConfigurationSection("items." + material.name());
    }

    public List<Material> getAllAllowedMaterials() {
        return Arrays.stream(Material.values())
                .filter(this::isAllowed)
                .collect(Collectors.toList());
    }

    public List<Material> getSellableMaterials() {
        return getAllAllowedMaterials().stream()
                .filter(this::isSellable)
                .collect(Collectors.toList());
    }

    public List<Material> getCategory(String category) {
        return categories.getOrDefault(category, Collections.emptyList());
    }

    public Map<String, List<Material>> getCategories() {
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
        return null;
    }


}