package my.reqqpe.rseller.models.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;


@AllArgsConstructor
@Getter
public class ItemData {
    private final String name;
    private final List<String> lore;
    private final int modelData;
    private final Map<Enchantment, Integer> enchantments;
    private final Map<NamespacedKey, Object> nbtTags;
}
