package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.models.SearchItemSettings;
import my.reqqpe.rseller.models.SellableItem;
import my.reqqpe.rseller.models.SellableItemData;
import my.reqqpe.rseller.utils.CustomConfig;
import my.reqqpe.rseller.utils.LoggerUtil;
import my.reqqpe.rseller.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.*;


// TODO: Сделать поддержку ntb
public class ItemManager {

    private final RSeller plugin;
    private final CustomConfig itemsConfigFile;
    private FileConfiguration itemsConfig;
    private List<SellableItem> items =  new ArrayList<>();
    private Map<Material, Set<SellableItem>> materialItems = new EnumMap<>(Material.class);
    private SearchItemSettings searchItemSettings;

    public ItemManager(RSeller plugin) {
        this.plugin = plugin;
        this.itemsConfigFile = new CustomConfig(plugin, "items.yml");
        this.itemsConfig = this.itemsConfigFile.getConfig();

        loadItems();
    }

    public void loadItems() {
        items.clear();
        materialItems.clear();

        this.searchItemSettings = loadSearchItemSettings(itemsConfig);

        LoggerUtil.info(MessageUtil.getString("log.items-loading"));

        ConfigurationSection itemsSection = this.itemsConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            LoggerUtil.warn(MessageUtil.getString("log.items-empty"));
            return;
        }

        ConfigurationSection itemsSettingsSection = itemsConfig.getConfigurationSection("items-settings");
        if (itemsSettingsSection == null) {
            LoggerUtil.warn(MessageUtil.getString("log.items-no-settings"));
            return;
        }

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(id);
            if (itemSection == null) {
                errorLoadItem(id, "отсутствует конфигурация предмета");
                continue;
            }

            String matString = itemSection.getString("material").toUpperCase();
            Material material = Material.getMaterial(matString);
            if (material == null) {
                errorLoadItem(id, "Материал не найден");
                continue;
            }

            String name = itemSection.getString("name");
            List<String> lore = itemSection.getStringList("lore");
            int modelData = itemSection.getInt("model-data");

            Map<Enchantment, Integer> enchants = new HashMap<>();

            ConfigurationSection enchantmentsSection = itemSection.getConfigurationSection("enchantments");
            if (enchantmentsSection != null) {
                for (String enchantmentString : enchantmentsSection.getKeys(false)) {
                    Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentString));
                    if (enchantment == null) {
                        errorLoadItem(id, "Недопустимое зачарование: " + enchantmentString);
                        break;
                    }
                    int level = enchantmentsSection.getInt(enchantmentString);
                    if (level <= 0) {
                        errorLoadItem(id, "Недопустимый уровень зачарования для " + enchantmentString + ": " + level);
                        break;
                    }
                    enchants.put(enchantment, level);
                }
            }


            ConfigurationSection itemSettingsSection = itemsSettingsSection.getConfigurationSection(id);
            if (itemSettingsSection == null) {
                errorLoadItem(id, "Отсутствует настройка предмета");
                return;
            }

            double points = itemSettingsSection.getDouble("points", 1);
            double money = itemSettingsSection.getDouble("money", 1);
            String displayName = itemSettingsSection.getString("display-name");

            String finalDisplayName;
            if (name != null && !name.isEmpty()) {
                finalDisplayName = name;
            } else if (displayName != null && !displayName.isEmpty()) {
                finalDisplayName = displayName;
            } else {
                finalDisplayName = material.name().toLowerCase().replace("_", " ");
            }

            SellableItemData sellableItemData = new SellableItemData(
                    name,
                    lore,
                    modelData,
                    enchants
            );

            SellableItem sellableItem = new SellableItem(
                    id,
                    material,
                    sellableItemData,
                    money,
                    points,
                    finalDisplayName
            );

            items.add(sellableItem);
            materialItems.computeIfAbsent(material, k -> new HashSet<>()).add(sellableItem);
        }
        LoggerUtil.info(MessageUtil.getString("log.items-loaded")
                        .replace("{amount}", String.valueOf(items.size()))
        );
    }

    private SearchItemSettings loadSearchItemSettings(FileConfiguration config) {
        var settingsSection = config.getConfigurationSection("settings-check");
        if (settingsSection == null) {
            return new SearchItemSettings(true, true, true, true, false, false);
        }
        boolean name = settingsSection.getBoolean("display-name", true);
        boolean lore = settingsSection.getBoolean("lore", true);
        boolean enchants = settingsSection.getBoolean("enchants", true);
        boolean modelData = settingsSection.getBoolean("model-data", true);
        boolean strictMode = settingsSection.getBoolean("strict-mode", false);
        boolean nbtTags = settingsSection.getBoolean("nbt-tags", false);
        return new SearchItemSettings(name, lore, enchants, modelData, nbtTags, strictMode);
    }


    private void errorLoadItem(String id, String errorMessage) {
        LoggerUtil.warn(
                MessageUtil.getString("log.items-error")
                        .replace("{id}", id)
                        .replace("{error}", errorMessage)
        );
    }

    public void reload() {
        this.itemsConfigFile.reloadConfig();
        this.itemsConfig = this.itemsConfigFile.getConfig();

        loadItems();
    }


    public SellableItem getSellableItem(ItemStack itemStack) {
        Set<SellableItem> items = getItemsByMaterial(itemStack.getType());
        for (SellableItem item : items) {
            if (item.equalsItemStack(itemStack, searchItemSettings)) {
                return item;
            }
        }
        return null;
    }

    public SellableItem getSellableItemById(String id) {
        for (SellableItem item : items) {
            if (item.id().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public Set<SellableItem> getItemsByMaterial(Material material) {
        Set<SellableItem> items = materialItems.get(material);
        return items == null ? new HashSet<>() : new HashSet<>(items);
    }
}
