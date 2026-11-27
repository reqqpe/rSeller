package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputManager implements Listener {

    private final Plugin plugin;

    private static class PendingInput {
        final Consumer<String> callback;

        PendingInput(Consumer<String> callback) {
            this.callback = callback;
        }
    }

    private final Map<UUID, PendingInput> pending = new HashMap<>();

    public ChatInputManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void awaitDouble(Player player, String field, Consumer<Double> callback) {
        pending.put(player.getUniqueId(), new PendingInput(raw -> {
            if (raw.equalsIgnoreCase("cancel")) {
                player.sendMessage(Colorizer.color("&cВвод отменён."));
                return;
            }
            try {
                double value = Double.parseDouble(raw);
                if (value < 0) {
                    player.sendMessage(Colorizer.color("&cЗначение не может быть отрицательным."));
                    return;
                }
                callback.accept(value);
            } catch (NumberFormatException ex) {
                player.sendMessage(Colorizer.color("&cНеверный формат числа: &e" + raw));
            }
        }));
    }

    public void awaitString(Player player, Consumer<String> callback) {
        pending.put(player.getUniqueId(), new PendingInput(raw -> {
            if (raw.equalsIgnoreCase("cancel")) {
                player.sendMessage(Colorizer.color("&cВвод отменён."));
                return;
            }
            callback.accept(raw);
        }));
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
        String message = e.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> input.callback.accept(message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }
}
