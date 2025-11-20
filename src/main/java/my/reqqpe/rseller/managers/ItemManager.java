package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.models.SellableItem;
import my.reqqpe.rseller.models.SellableItemData;
import my.reqqpe.rseller.utils.CustomConfig;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.util.*;

public class ItemManager {

    private final RSeller plugin;
    private CustomConfig itemsConfigFile;
    private FileConfiguration itemsConfig;
    private List<SellableItem> items =  new ArrayList<>();
    private Map<Material, Set<SellableItem>> materialItems = new EnumMap<>(Material.class);


    public ItemManager(RSeller plugin) {
        this.plugin = plugin;
        this.itemsConfigFile = new CustomConfig(plugin, "items.yml");
        this.itemsConfig = this.itemsConfigFile.getConfig();
    }

    public void loadItems() {
        items.clear();
        materialItems.clear();


        ConfigurationSection itemsSection = this.itemsConfig.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("Не удалось загрузить ни одного предмета. Секция items пуста");
            return;
        }

        ConfigurationSection itemsSettingsSection = itemsSection.getConfigurationSection("items-settings");
        if (itemsSettingsSection == null) {
            plugin.getLogger().warning("Не удалось загрузить ни одного предмета. Не обнаружена настройка на предметы");
            return;
        }

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(id);
            if (itemSection == null) {
                errorLoadItem(id, "отсутствует конфигурация предмета");
                continue;
            }
            Material material = Material.getMaterial(itemSection.getString("material"));
            if (material == null) {
                errorLoadItem(id, "Материал не найден");
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
                    name != null ? name : "",
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
    }

    private void errorLoadItem(String id, String errorMessage) {
        plugin.getLogger().warning(String.format("Ошибка загрузки предмета: %s. Ошибка: %s", id, errorMessage));
    }
}
