package my.reqqpe.rseller.models;

import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.HeadUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record MenuItem (
        String name,
        int modelData,
        Material material,
        List<String> lore,
        Map<Enchantment, Integer> enchants,
        String baseHead,
        List<String> itemFlags,
        List<String> rightActions,
        List<String> leftActions,
        boolean updatable

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
            meta.displayName(Colorizer.color(name));
        }

        if (lore != null && !lore.isEmpty()) {
            List<Component> coloredLore = Colorizer.colorizeAll(lore);
            meta.lore(coloredLore);
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


    public ItemStack toItemStack(Function<String, String> placeholder) {
        ItemStack item;

        if (baseHead != null && material == Material.PLAYER_HEAD) {
            item = HeadUtil.getCustomHead(baseHead);
        } else {
            item = new ItemStack(material);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(Colorizer.colorLegacy(
                    placeholder.apply(name)
            ));
        }

        if (lore != null && !lore.isEmpty()) {
            List<String> processed = lore.stream()
                    .map(placeholder)
                    .map(Colorizer::colorLegacy)
                    .toList();
            meta.setLore(processed);
        }

        if (modelData != -1) meta.setCustomModelData(modelData);

        if (enchants != null && !enchants.isEmpty()) {
            enchants.forEach((ench, lvl) ->
                    meta.addEnchant(ench, lvl, true)
            );
        }

        if (itemFlags != null && !itemFlags.isEmpty()) {
            for (String flagName : itemFlags) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("[MenuItem] Неизвестный item_flag: " + flagName);
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }
}
