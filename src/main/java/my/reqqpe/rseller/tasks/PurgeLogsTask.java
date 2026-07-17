package my.reqqpe.rseller.tasks;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.database.repositories.SellLogRepository;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class PurgeLogsTask {

    private static final long TICKS_PER_DAY = 20L * 60 * 60 * 24;

    private final Main plugin;
    private final SellLogRepository repository;
    private final int purgeDays;

    public PurgeLogsTask(Main plugin, SellLogRepository repository, int purgeDays) {
        this.plugin = plugin;
        this.repository = repository;
        this.purgeDays = purgeDays;
        runTask();
    }

    private void runTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long cutoff = Instant.now()
                    .minus(purgeDays, ChronoUnit.DAYS)
                    .getEpochSecond();

            repository.purgeOlderThan(cutoff).thenAccept(deleted -> {
                if (deleted > 0) {
                    plugin.getLogger().info("[SellLog] Purged " + deleted + " log entries older than " + purgeDays + " days.");
                }
            });
        }, TICKS_PER_DAY, TICKS_PER_DAY);
    }
}
