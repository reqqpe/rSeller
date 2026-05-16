package my.reqqpe.rseller.models.item;

import lombok.Builder;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

@Builder
public record ItemData(
        String name,
        List<String> lore,
        Integer modelData,
        Map<Enchantment, Integer> enchantments,
        Map<NamespacedKey, Object> nbtTags
) {

    public ItemData {
        lore = lore == null ? List.of() : lore;
        enchantments = enchantments == null ? Map.of() : enchantments;
        nbtTags = nbtTags == null ? Map.of() : nbtTags;
    }
    public boolean hasModelData() {
        return modelData != null;
    }
}
