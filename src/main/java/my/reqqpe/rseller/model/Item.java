package my.reqqpe.rseller.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
@Getter
public class Item {
    private final String id;
    private final ItemStack item;
    private ItemData itemData;
}
