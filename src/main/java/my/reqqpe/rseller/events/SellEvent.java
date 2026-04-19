package my.reqqpe.rseller.events;

import lombok.AccessLevel;
import lombok.Getter;
import my.reqqpe.rseller.database.Database;
import my.reqqpe.rseller.database.PlayerData;
import my.reqqpe.rseller.models.item.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;



/*
 Данный ивент вызывается в двух случаях:
 1. Обычная продажа
 2. Автоматическая продажа
*/

@Getter
public class SellEvent extends Event {


    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final List<Item> sellingItems;
    private final Database database;

    public SellEvent(Player player, List<Item> sellingItems, Database database) {
        this.player = player;
        this.sellingItems = sellingItems;
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
