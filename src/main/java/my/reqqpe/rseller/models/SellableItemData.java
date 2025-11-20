package my.reqqpe.rseller.models;

import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

public record SellableItemData(
        String name,
        List<String> lore,
        int modelData,
        Map<Enchantment, Integer> enchants
) {
}
