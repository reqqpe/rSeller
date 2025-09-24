package my.reqqpe.rseller.updateCheker;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import my.reqqpe.rseller.Main;
import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class UpdateChecker {
    private final Main plugin;
    private final String url = "https://api.github.com/repos/reqqpe/rSeller/releases/latest";

    @Getter
    private boolean updateAvailable = false;
    @Getter
    private String latestVersion = null;

    public UpdateChecker(Main plugin) {
        this.plugin = plugin;
    }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Java 11 HttpClient")
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() == 200) {

                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(response.body()).getAsJsonObject();
                    latestVersion = jsonObject.has("tag_name") ? jsonObject.get("tag_name").getAsString() : "Неизвестная версия";

                    String currentVersion = plugin.getDescription().getVersion();
                    updateAvailable = needsUpdate(currentVersion, latestVersion);

                    if (updateAvailable) {
                        plugin.getLogger().warning("======");
                        plugin.getLogger().warning("|");
                        plugin.getLogger().warning("| Доступно обновление: текущая версия " + currentVersion + ", последняя версия " + latestVersion);
                        plugin.getLogger().warning("|");
                        plugin.getLogger().warning("======");
                    } else {
                        plugin.getLogger().info("Обновление не требуется: текущая версия " + currentVersion + ", последняя версия " + latestVersion);
                    }
                } else {
                    plugin.getLogger().warning("Ошибка HTTP при проверке обновлений: " + response.statusCode());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка при проверке обновлений: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private boolean needsUpdate(String currentVersion, String latestVersion) {
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] latestParts = latestVersion.split("\\.");

            int maxLength = Math.max(currentParts.length, latestParts.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (currentPart < latestPart) {
                    return true;
                } else if (currentPart > latestPart) {
                    return false;
                }
            }
            return false;
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Некорректный формат версии: текущая=" + currentVersion + ", последняя=" + latestVersion);
            return false;
        }
    }
}

