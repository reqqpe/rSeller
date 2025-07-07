package my.reqqpe.rseller.managers;


import my.reqqpe.rseller.Main;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberFormatManager {


    private final Main plugin;
    private final Map<String, String> formatMap = new HashMap<>();
    private final Pattern pattern = Pattern.compile("%([#,0\\s+\\-]*)(\\d*)?(\\.\\d+)?f");

    public NumberFormatManager(Main plugin) {
        this.plugin = plugin;
        loadFormats(plugin.getConfig().getConfigurationSection("numbers_format"), "");
    }

    public void reload() {
        formatMap.clear();
        loadFormats(plugin.getConfig().getConfigurationSection("numbers_format"), "");
    }

    private void loadFormats(ConfigurationSection section, String prefix) {
        if (section == null) {
            System.err.println("Секция конфигурации пуста для префикса: " + prefix);
            return;
        }

        for (String key : section.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;

            if (section.isConfigurationSection(key)) {
                loadFormats(section.getConfigurationSection(key), path);
            } else {
                String format = section.getString(key, "%.2f");
                try {
                    String validatedFormat = validateFormat(format);
                    formatMap.put(path, validatedFormat);
                } catch (IllegalArgumentException e) {
                    System.err.println("Неверный формат числа в " + path + ": " + format + " - Ошибка: " + e.getMessage());
                    formatMap.put(path, "%.2f");
                }
            }
        }
    }

    private String validateFormat(String format) {
        Matcher matcher = pattern.matcher(format);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Формат не соответствует шаблону %f: " + format);
        }

        String flags = matcher.group(1) != null ? matcher.group(1) : "";
        String width = matcher.group(2) != null ? matcher.group(2) : "";
        String precision = matcher.group(3) != null ? matcher.group(3).substring(1) : "2";

        if (!flags.matches("[#,0\\s+\\-]*")) {
            throw new IllegalArgumentException("Недопустимые флаги в формате: " + flags);
        }

        if (!width.isEmpty()) {
            try {
                Integer.parseInt(width);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Недопустимая ширина в формате: " + width);
            }
        }

        try {
            int precisionValue = Integer.parseInt(precision);
            if (precisionValue < 0) {
                throw new IllegalArgumentException("Точность не может быть отрицательной: " + precision);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Недопустимая точность в формате: " + precision);
        }

        try {
            String.format(format, 42.7);
        } catch (IllegalFormatException e) {
            throw new IllegalArgumentException("Невалидный формат для String.format: " + e.getMessage());
        }

        return format;
    }

    public String format(String path, double value) {
        String format = formatMap.getOrDefault(path, "%.2f");
        try {
            return String.format(format, value);
        } catch (IllegalFormatException e) {
            System.err.println("Ошибка форматирования для пути " + path + ": " + e.getMessage());
            return String.format("%.2f", value);
        }
    }

    public String getFormat(String path) {
        return formatMap.getOrDefault(path, "%.2f");
    }
}
