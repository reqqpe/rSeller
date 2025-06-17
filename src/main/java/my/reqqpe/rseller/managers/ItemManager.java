package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.model.ItemData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemManager {

    private final Map<String, ItemData> items = new HashMap<>();
    private final Main plugin;

    public ItemManager(Main plugin) {
        this.plugin = plugin;
    }

    public void load() {
        items.clear();

        FileConfiguration config = plugin.getItemsConfig().getConfig();
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("Секция 'items' не найдена в конфигурации items.yml!");
            return;
        }

        plugin.getLogger().info("Обнаружено " + itemsSection.getKeys(false).size() + " предметов в items.yml.");

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection section = itemsSection.getConfigurationSection(id);
            if (section == null) {
                plugin.getLogger().warning("Секция для предмета '" + id + "' не является ConfigurationSection. Пропускаем.");
                continue;
            }

            double points = section.getDouble("points", 0.0);
            double price = section.getDouble("price", 0.0);

            ConfigurationSection itemStackSection = section.getConfigurationSection("itemstack");
            if (itemStackSection == null) {
                plugin.getLogger().warning("Секция 'itemstack' не найдена для предмета с id: " + id);
                continue;
            }

            ItemStack item;
            try {
                item = ItemStack.deserialize(itemStackSection.getValues(false));
                if (item == null || item.getType() == Material.AIR) {
                    plugin.getLogger().warning("Невалидный ItemStack для предмета с id: " + id);
                    continue;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Не удалось десериализовать предмет для id: " + id + ". Ошибка: " + e.getMessage());
                continue;
            }

            ItemData data = new ItemData(id, points, price, item);
            items.put(id.toLowerCase(), data);
            plugin.getLogger().info("Загружен предмет: " + id + ", price: " + price + ", points: " + points);
        }

        plugin.getLogger().info("Загружено " + items.size() + " предметов из items.yml.");
    }

    public ItemData getById(String id) {
        return items.get(id.toLowerCase());
    }

    public ItemData getByItemStack(ItemStack stack) {
        for (ItemData data : items.values()) {
            if (data.matches(stack)) return data;
        }
        return null;
    }

    public Collection<ItemData> getAllItems() {
        return items.values();
    }

    public boolean createItem(String id, double points, double price, ItemStack item) {
        String lowerId = id.toLowerCase();
        if (items.containsKey(lowerId)) {
            plugin.getLogger().warning("Предмет с id " + id + " уже существует!");
            return false;
        }

        if (item == null || item.getType() == Material.AIR) {
            plugin.getLogger().warning("Нельзя создать предмет с пустым ItemStack для id: " + id);
            return false;
        }

        ItemData data = new ItemData(lowerId, points, price, item.clone());
        items.put(lowerId, data);

        FileConfiguration config = plugin.getItemsConfig().getConfig();

        String path = "items." + lowerId;
        config.set(path + ".points", points);
        config.set(path + ".price", price);
        config.set(path + ".itemstack", item.serialize());

        try {
            plugin.getItemsConfig().saveConfig();
            plugin.getLogger().info("Предмет с id " + lowerId + " успешно создан и сохранен в items.yml.");
            load();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при сохранении конфигурации items.yml для предмета с id: " + lowerId + ". Ошибка: " + e.getMessage());
            items.remove(lowerId);
            return false;
        }
    }

    public boolean removeItem(String id) {
        String lowerId = id.toLowerCase();
        if (!items.containsKey(lowerId)) {
            plugin.getLogger().warning("Предмет с id " + id + " не найден для удаления!");
            return false;
        }

        items.remove(lowerId);

        FileConfiguration config = plugin.getItemsConfig().getConfig();

        String path = "items." + lowerId;
        config.set(path, null);

        try {
            plugin.getItemsConfig().saveConfig();
            plugin.getLogger().info("Предмет с id " + lowerId + " успешно удален из items.yml.");
            load();
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при сохранении конфигурации items.yml после удаления предмета с id: " + lowerId + ". Ошибка: " + e.getMessage());
            load();
            return false;
        }
    }
}