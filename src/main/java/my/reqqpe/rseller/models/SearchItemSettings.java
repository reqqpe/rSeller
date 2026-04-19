package my.reqqpe.rseller.models;

public record SearchItemSettings(boolean name, boolean lore, boolean enchants, boolean modelData, boolean nbtTags) {


    /**
     * Проверяет, включена ли хотя бы одна настройка поиска.
     * Возвращает true, если хотя бы один параметр true, и false, если все параметры false.
     *
     * @return true, если хотя бы одна настройка включена, иначе false
     */
    public boolean hasAnyEnabled() {
        return name || lore || modelData || enchants || nbtTags;
    }
}
