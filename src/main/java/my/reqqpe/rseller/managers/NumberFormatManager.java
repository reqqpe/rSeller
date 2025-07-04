package my.reqqpe.rseller.managers;


import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class NumberFormatManager {

    private final Map<String, DecimalFormat> formatMap = new HashMap<>();

    public NumberFormatManager(FileConfiguration config) {
        loadFormats(config.getConfigurationSection("numbers_format"), "");
    }

    private void loadFormats(ConfigurationSection section, String prefix) {
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;

            if (section.isConfigurationSection(key)) {
                loadFormats(section.getConfigurationSection(key), path);
            } else {
                String format = section.getString(key, "%.2f");
                try {
                    formatMap.put(path, new DecimalFormat(convertToJavaFormat(format)));
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid number format at " + path + ": " + format);
                }
            }
        }
    }

    private String convertToJavaFormat(String format) {
        if (format.matches("%\\.\\df")) {
            int decimals = Integer.parseInt(format.replaceAll("[^0-9]", ""));
            return "#,##0." + "0".repeat(decimals);
        }
        return "#,##0.00";
    }

    public String format(String path, double value) {
        DecimalFormat df = formatMap.getOrDefault(path, new DecimalFormat("#,##0.00"));
        return df.format(value);
    }

    public DecimalFormat getFormat(String path) {
        return formatMap.getOrDefault(path, new DecimalFormat("#,##0.00"));
    }
}
