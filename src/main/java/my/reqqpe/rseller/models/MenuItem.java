package my.reqqpe.rseller.models;

import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public record MenuItem (
        String name,
        int modelData,
        Material material,
        List<String> lore,
        Map<Enchantment, Integer> enchants,
        String baseHead,
        List<String> itemFlags,
        List<String> rightActions,
        List<String> leftActions

) {
    public ItemStack toItemStack() {
        ItemStack item;

        if (baseHead != null && material == Material.PLAYER_HEAD) {
            item = HeadUtil.getCustomHead(baseHead);
        } else {
            item = new ItemStack(material);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(Colorizer.color(name));
        }

        if (lore != null && !lore.isEmpty()) {
            List<String> coloredLore = Colorizer.colorizeAll(lore);
            meta.setLore(coloredLore);
        }

        if (modelData != -1) {
            meta.setCustomModelData(modelData);
        }

        if (enchants != null && !enchants.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> e : enchants.entrySet()) {
                meta.addEnchant(e.getKey(), e.getValue(), true);
            }
        }

        if (itemFlags != null && !itemFlags.isEmpty()) {
            for (String flagName : itemFlags) {
                try {
                    ItemFlag flag = ItemFlag.valueOf(flagName.toUpperCase());
                    meta.addItemFlags(flag);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("[MenuItem] Неизвестный item_flag: " + flagName);
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }
}
