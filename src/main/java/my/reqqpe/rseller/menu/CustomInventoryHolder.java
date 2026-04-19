package my.reqqpe.rseller.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public record CustomInventoryHolder(String id) implements InventoryHolder {

    @Override
    public Inventory getInventory() {
        return null;
    }
}