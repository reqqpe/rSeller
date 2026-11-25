package my.reqqpe.rseller.models;

public record SearchItemSettings(boolean name, boolean lore, boolean enchants, boolean modelData, boolean nbtTags) {

    public boolean hasAnyEnabled() {
        return name || lore || modelData || enchants || nbtTags;
    }
}
