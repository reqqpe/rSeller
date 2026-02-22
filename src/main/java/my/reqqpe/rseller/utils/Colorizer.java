package my.reqqpe.rseller.utils;

import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Colorizer {

    @Setter
    public static boolean LEGACY_FORMAT;

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");
    private static final char COLOR_CHAR = '§';

    public Component color(String message) {
        return LEGACY_FORMAT
                ? Component.text(colorLegacy(message))
                : colorComponent(message);
    }

    public String colorLegacy(String message) {
        if (message == null || message.isEmpty()) return message;

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder builder = new StringBuilder(message.length() + 32);

        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(builder,
                    COLOR_CHAR + "x" +
                            COLOR_CHAR + hex.charAt(0) +
                            COLOR_CHAR + hex.charAt(1) +
                            COLOR_CHAR + hex.charAt(2) +
                            COLOR_CHAR + hex.charAt(3) +
                            COLOR_CHAR + hex.charAt(4) +
                            COLOR_CHAR + hex.charAt(5));
        }

        message = matcher.appendTail(builder).toString();
        return translateAlternateColorCodes('&', message);
    }

    public Component colorComponent(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        String parsed = replaceHex(message);

        return MINI.deserialize(parsed);
    }

    public List<Component> colorizeAll(List<String> list) {
        List<Component> result = new ArrayList<>();
        for (String s : list) {
            result.add(color(s));
        }
        return result;
    }

    private String replaceHex(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, "<#" + hex + ">");
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        final char[] b = textToTranslate.toCharArray();

        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && isValidColorCharacter(b[i + 1])) {
                b[i++] = COLOR_CHAR;
                b[i] |= 0x20;
            }
        }

        return new String(b);
    }

    private boolean isValidColorCharacter(char c) {
        return (c >= '0' && c <= '9') ||
                (c >= 'a' && c <= 'f') ||
                c == 'r' ||
                (c >= 'k' && c <= 'o') ||
                c == 'x' ||
                (c >= 'A' && c <= 'F') ||
                c == 'R' ||
                (c >= 'K' && c <= 'O') ||
                c == 'X';
    }
}