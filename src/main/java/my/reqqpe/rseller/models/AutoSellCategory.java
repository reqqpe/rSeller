package my.reqqpe.rseller.models;

import my.reqqpe.rseller.managers.ItemManager;

import java.util.ArrayList;
import java.util.List;

public record AutoSellCategory(String displayName, List<String> idItems) {

    public List<SellableItem> getItems(ItemManager itemManager) {
        List<SellableItem> items = new ArrayList<>();
        for (String idItem : idItems) {
            SellableItem item = itemManager.getSellableItemById(idItem);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }
}
