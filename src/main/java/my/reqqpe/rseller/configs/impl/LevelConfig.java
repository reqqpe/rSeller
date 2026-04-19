package my.reqqpe.rseller.configs.impl;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.configs.CustomConfig;
import my.reqqpe.rseller.models.Level;

public class LevelConfig extends CustomConfig {

    private final Int2ObjectSortedMap<Level> levels = new Int2ObjectAVLTreeMap<>();

    public LevelConfig(Main plugin) {
        super(plugin, "levels.yml");
    }

    @Override
    protected void load() {

        levels.clear();

        for (String key : config.getKeys(false)) {

            int id;
            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Invalid level id: " + key);
                continue;
            }

            double coin = getDouble(key + ".coin-multiplier", 1.0);
            double point = getDouble(key + ".point-multiplier", 1.0);
            double req = getDouble(key + ".required-points", 0);

            levels.put(id, new Level(id, coin, point, req));
        }

        plugin.getLogger().info("Loaded " + levels.size() + " levels");
    }

    public Level get(int level) {
        return levels.get(level);
    }

    public Int2ObjectSortedMap<Level> getLevels() {
        return levels;
    }
}