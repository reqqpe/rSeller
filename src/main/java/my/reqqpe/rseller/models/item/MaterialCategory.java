package my.reqqpe.rseller.models.item;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public enum MaterialCategory {

    ORES,
    INGOTS,
    GEMS,
    LOGS,
    PLANKS,
    FOOD,
    TOOLS,
    WEAPONS,
    ARMOR,
    BLOCKS,
    MISC;

    public static final List<String> NAMES = Arrays.stream(values())
            .map(c -> c.name().toLowerCase())
            .toList();

    public static MaterialCategory from(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static MaterialCategory of(Material material) {
        String name = material.name();

        if (name.endsWith("_ORE") || name.startsWith("RAW_")) return ORES;

        if (name.endsWith("_INGOT") || name.endsWith("_NUGGET")
                || name.equals("NETHERITE_SCRAP")) return INGOTS;

        if (name.equals("DIAMOND") || name.equals("EMERALD")
                || name.equals("LAPIS_LAZULI") || name.equals("QUARTZ")
                || name.equals("AMETHYST_SHARD") || name.equals("PRISMARINE_CRYSTALS")
                || name.equals("PRISMARINE_SHARD") || name.equals("ECHO_SHARD")
                || name.equals("NETHER_STAR")) return GEMS;

        if (name.endsWith("_LOG") || name.endsWith("_WOOD")
                || name.startsWith("STRIPPED_")) return LOGS;

        if (name.endsWith("_PLANKS")) return PLANKS;

        if (material.isEdible()) return FOOD;

        if (name.endsWith("_SWORD") || name.equals("BOW") || name.equals("CROSSBOW")
                || name.equals("TRIDENT") || name.equals("MACE")) return WEAPONS;

        if (name.endsWith("_PICKAXE") || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.endsWith("_SHEARS") || name.equals("SHEARS")
                || name.equals("FLINT_AND_STEEL") || name.equals("FISHING_ROD")
                || name.equals("CARROT_ON_A_STICK") || name.equals("WARPED_FUNGUS_ON_A_STICK")) return TOOLS;

        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.endsWith("_HORSE_ARMOR")) return ARMOR;

        if (material.isBlock()) return BLOCKS;

        if (material.isItem()) return MISC;

        return null;
    }

    public Set<Material> materials() {
        Set<Material> result = EnumSet.noneOf(Material.class);
        for (Material material : Material.values()) {
            if (!material.isItem() && !material.isBlock()) continue;
            if (material.isAir() || material.isLegacy()) continue;
            if (of(material) == this) result.add(material);
        }
        return result;
    }
}
