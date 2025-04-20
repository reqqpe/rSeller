package my.reqqpe.rseller.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;

public class colorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F0-9]{6})");

    public static String color(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            StringBuilder builder = new StringBuilder("&x");
            for (char c : hexCode.toCharArray())
                builder.append('&').append(c);
            text = text.replace("&#" + hexCode, builder.toString());
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> color(List<String> list) {
        List<String> result = new ArrayList<>();
        for (String string : list)
            result.add(color(string));
        return result;
    }
}