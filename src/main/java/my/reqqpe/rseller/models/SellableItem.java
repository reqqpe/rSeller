package my.reqqpe.rseller.models;

import org.bukkit.Material;

public record SellableItem(
        String id,
        Material material,
        SellableItemData itemData,
        double price,
        double points,
        String displayName
) {

}
