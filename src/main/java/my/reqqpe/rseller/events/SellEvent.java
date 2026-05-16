package my.reqqpe.rseller.events;

import lombok.Getter;
import my.reqqpe.rseller.cache.PlayerDataCache;
import my.reqqpe.rseller.models.PlayerData;
import my.reqqpe.rseller.models.item.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;


@Getter
public class SellEvent extends Event {


    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final List<Item> sellingItems;

    public SellEvent(Player player, List<Item> sellingItems) {
        this.player = player;
        this.sellingItems = sellingItems;
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
