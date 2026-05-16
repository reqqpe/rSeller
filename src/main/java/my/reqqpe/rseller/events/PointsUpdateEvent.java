package my.reqqpe.rseller.events;

import lombok.Getter;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;


@Getter
public class PointsUpdateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final double points;

    private final boolean command;

    private final String action;

    public PointsUpdateEvent(Player player, double points, boolean command, String action) {
        this.player = player;
        this.points = points;
        this.command = command;
        this.action = action;
    }

    public PlayerData getPlayerData() {
        return PlayerDataCache.getOrCreate(player.getUniqueId());
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}
