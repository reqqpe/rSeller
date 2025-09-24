package my.reqqpe.rseller.events;

import lombok.AccessLevel;
import lombok.Getter;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;




/*
 Вызывается в 2 случаях:
 1. С помощью команды (command = true)
 2. При продаже (command = false)
 */


@Getter
public class PointsUpdateEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final double points;
    private final Database database;

    // Если обновление очков выполнено с помощью команды - true
    private final boolean command;

    // Допустимые значения: add, remove, set
    private final String action;

    public PointsUpdateEvent(Player player, double points, boolean command, String action, Database database) {
        this.player = player;
        this.points = points;
        this.command = command;
        this.action = action;
        this.database = database;
    }

    public PlayerData getPlayerData() {
        return database.getPlayerData(player.getUniqueId());
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }
}
