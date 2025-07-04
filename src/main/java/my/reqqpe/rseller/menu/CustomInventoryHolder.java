package my.reqqpe.rseller.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CustomInventoryHolder implements InventoryHolder {
    private final String id;

    public CustomInventoryHolder(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}