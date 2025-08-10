package my.reqqpe.rseller.models.item;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;


public record ItemData(String name, List<String> lore, int modelData, Map<Enchantment, Integer> enchantments,
                       Map<NamespacedKey, Object> nbtTags) {
}
