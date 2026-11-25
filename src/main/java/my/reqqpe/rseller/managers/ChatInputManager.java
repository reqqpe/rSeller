package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputManager implements Listener {

    private static class PendingInput {
        final String field;
        final Consumer<Double> callback;

        PendingInput(String field, Consumer<Double> callback) {
            this.field = field;
            this.callback = callback;
        }
    }

    private final Map<UUID, PendingInput> pending = new HashMap<>();

    public void awaitDouble(Player player, String field, Consumer<Double> callback) {
        pending.put(player.getUniqueId(), new PendingInput(field, callback));
    }

    public void cancel(Player player) {
        pending.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        PendingInput input = pending.remove(player.getUniqueId());
        if (input == null) return;

        e.setCancelled(true);

        String msg = e.getMessage().trim();

        if (msg.equalsIgnoreCase("cancel")) {
            player.sendMessage(Colorizer.color("&cВвод отменён."));
            return;
        }

        try {
            double value = Double.parseDouble(msg);
            if (value < 0) {
                player.sendMessage(Colorizer.color("&cЗначение не может быть отрицательным."));
                return;
            }
            input.callback.accept(value);
        } catch (NumberFormatException ex) {
            player.sendMessage(Colorizer.color("&cНеверный формат числа: &e" + msg));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }
}
