package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.configs.BoosterConfig;
import my.reqqpe.rseller.models.Booster;
import my.reqqpe.rseller.models.PermissionBooster;
import my.reqqpe.rseller.models.SellSource;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

public class BoosterManager {
    private final Main plugin;
    private final BoosterConfig boosterConfig;

    public BoosterManager(Main plugin) {
        this.plugin = plugin;
        this.boosterConfig = plugin.getBoosterConfig();
    }

    public Booster getMultiplier(Player player, SellSource source) {
        List<PermissionBooster> activeBoosters = boosterConfig.getBoosters().stream()
                .filter(b -> b.source().appliesTo(source))
                .filter(b -> player.hasPermission(b.permission()))
                .toList();

        double coins;
        double points;

        if (boosterConfig.isStackMode(source)) {
            coins = 1.0;
            points = 1.0;
            for (PermissionBooster booster : activeBoosters) {
                coins += booster.coinMultiplier() - 1.0;
                points += booster.pointMultiplier() - 1.0;
            }
        } else {
            if (activeBoosters.isEmpty()) {
                coins = 1.0;
                points = 1.0;
            } else {
                PermissionBooster best = activeBoosters.stream()
                        .max(Comparator.comparingInt(PermissionBooster::priority)
                                .thenComparingDouble(PermissionBooster::coinMultiplier))
                        .orElse(null);
                coins = best != null ? best.coinMultiplier() : 1.0;
                points = best != null ? best.pointMultiplier() : 1.0;
            }
        }

        if (boosterConfig.isLevelApply(source)) {
            LevelManager levelManager = plugin.getLevelManager();
            double levelCoinMult = levelManager.getCoinMultiplier(player);
            double levelPointMult = levelManager.getPointMultiplier(player);
            coins += levelCoinMult - 1.0;
            points += levelPointMult - 1.0;
        }

        coins = Math.max(1.0, coins);
        points = Math.max(1.0, points);

        return new Booster(coins, points);
    }
}
