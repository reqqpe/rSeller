package my.reqqpe.rseller.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@Getter
@AllArgsConstructor
public class ItemData {
    private final String id;
    private final double points;
    private final double price;
    private final ItemStack item;

    public boolean matches(ItemStack other) {
        return other != null && item.isSimilar(other);
    }
}
