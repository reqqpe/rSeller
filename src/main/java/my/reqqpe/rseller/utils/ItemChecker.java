package my.reqqpe.rseller.utils;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;


public class ItemChecker {
    private final boolean display_name;
    private final boolean lore;
    private final boolean durability;
    private final boolean enchants;
    private final boolean model_data;
    private final boolean nbt_tags;
    private final boolean nbtApiAvailable;

    public ItemChecker(boolean displayName, boolean lore, boolean durability, boolean enchants, boolean modelData, boolean nbtTags) {
        this.display_name = displayName;
        this.lore = lore;
        this.durability = durability;
        this.enchants = enchants;
        this.model_data = modelData;
        this.nbt_tags = nbtTags;
        this.nbtApiAvailable = isNBTApiAvailable();
    }


    private boolean isNBTApiAvailable() {
        try {
            Class.forName("de.tr7zw.changeme.nbtapi.NBTItem");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }


    public boolean isSimilarCustom(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (!a.getType().equals(b.getType())) return false;

        ItemMeta metaA = a.getItemMeta();
        ItemMeta metaB = b.getItemMeta();

        if ((metaA == null) != (metaB == null)) return false;
        if (metaA == null) return true;

        if (display_name) {
            String displayNameA = metaA.hasDisplayName() ? metaA.getDisplayName() : null;
            String displayNameB = metaB.hasDisplayName() ? metaB.getDisplayName() : null;
            if ((displayNameA == null) != (displayNameB == null)) return false;
            if (displayNameA != null && !displayNameA.equals(displayNameB)) return false;
        }

        if (lore) {
            List<String> loreA = metaA.getLore();
            List<String> loreB = metaB.getLore();

            if ((loreA == null) != (loreB == null)) return false;
            if (loreA != null && !loreA.equals(loreB)) return false;
        }

        if (durability) {
            if ((metaA instanceof Damageable) != (metaB instanceof Damageable)) return false;
            if (metaA instanceof Damageable) {
                int damageA = ((Damageable) metaA).getDamage();
                int damageB = ((Damageable) metaB).getDamage();
                if (damageA != damageB) return false;
            }
        }

        if (enchants
                && !metaA.getEnchants().equals(metaB.getEnchants())) return false;

        if (model_data) {
            if (metaA.hasCustomModelData() != metaB.hasCustomModelData()) return false;
            if (metaA.hasCustomModelData() && metaA.getCustomModelData() != metaB.getCustomModelData()) return false;
        }

        if (nbt_tags && nbtApiAvailable) {
            try {
                NBTItem nbtA = new NBTItem(a);
                NBTItem nbtB = new NBTItem(b);
                if (!nbtA.toString().equals(nbtB.toString())) return false;
            } catch (Throwable ignored) {
            }
        }

        return true;
    }
}
