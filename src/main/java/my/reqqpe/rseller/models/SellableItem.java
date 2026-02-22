package my.reqqpe.rseller.models;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record SellableItem(
        String id,
        Material material,
        SellableItemData itemData,
        double money,
        double points,
        String displayName
) {

    public boolean equalsItemStack(ItemStack itemStack, SearchItemSettings settings) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return false;
        }

        if (itemStack.getType() != material) {
            return false;
        }

        if (!settings.hasAnyEnabled()) {
            return true;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return false;
        }

        if (itemData == null) {
            if (settings.strictMode()) {
                return false;
            }
            return true;
        }

        if (settings.name()) {
            String expected = itemData.name();
            String actual = meta.hasDisplayName() ? meta.getDisplayName() : null;

            if (!Objects.equals(expected, actual)) {
                return false;
            }
        }

        if (settings.lore()) {
            List<String> expected = itemData.lore() != null ? itemData.lore() : List.of();
            List<String> actual = meta.hasLore() ? meta.getLore() : List.of();

            if (!Objects.equals(expected, actual)) {
                return false;
            }
        }

        if (settings.modelData()) {
            int expected = itemData.modelData();
            int actual = meta.hasCustomModelData() ? meta.getCustomModelData() : 0;

            if (expected != actual) {
                return false;
            }
        }

        if (settings.enchants()) {
            Map<Enchantment, Integer> expected = itemData.enchants();
            Map<Enchantment, Integer> actual = meta.getEnchants();

            if (!Objects.equals(expected, actual)) {
                return false;
            }
        }

        if (settings.nbtTags()) {
            // Если нужен — могу реализовать сравнение NBT через PersistentDataContainer
            // Сейчас просто оставить "всегда true"
        }

        return true;
    }

    public ItemStack toItemStack() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Имя
        if (itemData != null) {
            if (itemData.name() != null) {
                meta.setDisplayName(itemData.name());
            } else if (displayName != null) {
                meta.setDisplayName(displayName);
            }

            // Лор
            if (itemData.lore() != null) {
                meta.setLore(itemData.lore());
            }

            // CustomModelData
            if (itemData.modelData() != 0) {
                meta.setCustomModelData(itemData.modelData());
            }

            // Энчанты
            if (itemData.enchants() != null && !itemData.enchants().isEmpty()) {
                itemData.enchants().forEach((ench, level) ->
                        meta.addEnchant(ench, level, true));
            }
        } else if (displayName != null) {
            meta.setDisplayName(displayName);
        }

        item.setItemMeta(meta);
        return item;
    }
}
