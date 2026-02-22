package my.reqqpe.rseller.utils;

import lombok.Setter;
import lombok.experimental.UtilityClass;
import me.clip.placeholderapi.PlaceholderAPI;
import my.reqqpe.rseller.RSeller;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@UtilityClass
public class MessageUtils {

    @Setter
    private static FileConfiguration config;



    public static String getString(String path) {
        return config.getString(path);
    }
    public static List<String> getListString(String path) {
        return config.getStringList(path);
    }


    public static String replacePlaceholders(Player player, String text, HashMap<String, String> placeholders) {
        if (text == null || text.isEmpty()) return text;

        for (String placeholder : placeholders.keySet()) {
            String value = placeholders.get(placeholder);
            text = text.replace("{" + placeholder + "}", value);
        }

        text = PlaceholderAPI.setPlaceholders(player, text);
        return text;
    }

    public static List<String> replacePlaceholders(Player player, List<String> listText, HashMap<String, String> placeholders) {
        List<String> result = new ArrayList<>();
        for (String s : listText) {
            result.add(replacePlaceholders(player, s, placeholders));
        }
        return result;
    }



    @Setter
    private static RSeller plugin;

    public static void sendMessage(Player player, String string) {
        if (string != null && !string.isEmpty()) {
            plugin.adventure().player(player).sendMessage(
                    Colorizer.color(string)
            );
        }
    }
    public static void sendMessage(Player player, List<String> stringList) {
        if (!stringList.isEmpty()) {
            for (String string : stringList) {
                plugin.adventure().player(player).sendMessage(
                        Colorizer.color(string)
                );
            }
        }
    }
}
