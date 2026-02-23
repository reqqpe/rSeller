package my.reqqpe.rseller.models;

import my.reqqpe.rseller.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;

public record Level(
        int level,
        double coinMultiplier,
        double pointMultiplier,
        double requiredPoints,
        List<ParsedAction> parsedActions
) {

    public void performActions(Player player) {

        for (ParsedAction pa : parsedActions) {
            String action = pa.action();
            String data = pa.data();
            switch (action) {
                case "player": {
                    Bukkit.dispatchCommand(player, data);
                    break;
                }
                case "console": {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), data);
                    break;
                }
                case "message": {
                    data = MessageUtil.replacePlaceholders(player, data, new HashMap<>());
                    MessageUtil.sendMessage(player, data);
                    break;
                }
                case "sound": {
                    String[] parts = data.split(";");
                    try {
                        Sound sound = Sound.valueOf(parts[0].toUpperCase());
                        float volume = parts.length >= 2 ? Float.parseFloat(parts[1]) : 1.0f;
                        float pitch  = parts.length >= 3 ? Float.parseFloat(parts[2]) : 1.0f;
                        player.playSound(player.getLocation(), sound, volume, pitch);
                    } catch (Exception e) {
                        break;
                    }
                    break;
                }
            }
        }
    }
}
