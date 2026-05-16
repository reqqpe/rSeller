package my.reqqpe.rseller.managers;

import my.reqqpe.rseller.Main;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class NumberFormatManager {

    private final Main plugin;


    private final Map<String, String> formatMap = new HashMap<>();

    private final Map<String, Boolean> trimMap = new HashMap<>();

    private static final Pattern FORMAT_PATTERN = Pattern.compile("^%\\.\\d+f~?$");

    public NumberFormatManager(Main plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        formatMap.clear();
        trimMap.clear();
        load();
    }

    private void load() {
        ConfigurationSection section = plugin.getMainConfig().getConfig().getConfigurationSection("numbers_format");
        if (section == null) {
            plugin.getLogger().warning("NumberFormatManager: 'numbers_format' section not found in config.yml, using defaults.");
            return;
        }
        loadSection(section, "");
    }

    private void loadSection(ConfigurationSection section, String prefix) {
        for (String key : section.getKeys(false)) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;

            if (section.isConfigurationSection(key)) {
                loadSection(section.getConfigurationSection(key), path);
            } else {
                String raw = section.getString(key, "%.2f");
                if (raw == null || !FORMAT_PATTERN.matcher(raw).matches()) {
                    plugin.getLogger().warning("NumberFormatManager: invalid format at '" + path + "': '" + raw + "'. Expected %.Nf or %.Nf~ (e.g. %.2f or %.2f~). Using default: %.2f");
                    formatMap.put(path, "%.2f");
                    trimMap.put(path, false);
                } else {
                    boolean trim = raw.endsWith("~");
                    String javaFormat = trim ? raw.substring(0, raw.length() - 1) : raw;
                    formatMap.put(path, javaFormat);
                    trimMap.put(path, trim);
                }
            }
        }
    }

    public String format(String path, double value) {
        String javaFormat = formatMap.getOrDefault(path, "%.2f");
        boolean trim = trimMap.getOrDefault(path, false);

        String result = String.format(javaFormat, value);

        if (trim && result.contains(".")) {
            result = result.replaceAll("0+$", "");
            result = result.replaceAll("\\.$", "");
        }

        return result;
    }

    public String getFormat(String path) {
        return formatMap.getOrDefault(path, "%.2f");
    }
}
