package my.reqqpe.rseller.configs;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.models.BoosterSource;
import my.reqqpe.rseller.models.PermissionBooster;
import my.reqqpe.rseller.models.SellSource;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class BoosterConfig extends CustomConfig {

    public BoosterConfig(Main plugin) {
        super(plugin, "boosters.yml");
    }

    @Override
    protected void load() {
    }

    public boolean isStackMode(SellSource source) {
        String path = "boosters.sell-sources." + source.name().toLowerCase() + ".stack-mode";
        return config.getBoolean(path, true);
    }

    public boolean isLevelApply(SellSource source) {
        String path = "boosters.levels." + source.name().toLowerCase() + ".apply";
        return config.getBoolean(path, true);
    }

    public List<PermissionBooster> getBoosters() {
        List<PermissionBooster> boosters = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("boosters.permission-boosters");
        
        if (section == null) return boosters;

        for (String id : section.getKeys(false)) {
            ConfigurationSection boosterSection = section.getConfigurationSection(id);
            if (boosterSection == null) continue;

            String permission = boosterSection.getString("permission");
            if (permission == null) continue;

            double coins = boosterSection.getDouble("coins", 1.0);
            double points = boosterSection.getDouble("points", 1.0);
            int priority = boosterSection.getInt("priority", 0);
            String sourceStr = boosterSection.getString("source", "all");
            BoosterSource source = BoosterSource.from(sourceStr);

            boosters.add(new PermissionBooster(id, permission, coins, points, priority, source));
        }

        return boosters;
    }
}
