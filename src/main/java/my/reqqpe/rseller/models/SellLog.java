package my.reqqpe.rseller.models;

import java.time.Instant;
import java.util.UUID;

public record SellLog(
        long id,
        UUID uuid,
        String itemId,
        int amount,
        double coins,
        double points,
        SellSource source,
        long soldAt,
        int level,
        double coinBooster,
        double pointBooster,
        String world,
        String playerName
) {

    public static SellLog of(UUID uuid, String playerName, String itemId, int amount,
                             double coins, double points, SellSource source,
                             int level, double coinBooster, double pointBooster,
                             String world) {
        return new SellLog(0, uuid, itemId, amount, coins, points,
                source, Instant.now().getEpochSecond(),
                level, coinBooster, pointBooster, world, playerName);
    }
}
