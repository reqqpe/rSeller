package my.reqqpe.rseller.models;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Map;

public record Menu(
        String id,
        String title,
        int size,
        String openPermission,
        IntList specialSlots,
        Map<Integer, MenuItem> menuItems,
        IntList updatableSlots
) {

    public MenuItem getItem(int slot) {
        return menuItems().get(slot);
    }

}
