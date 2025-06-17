package my.reqqpe.rseller.database;

import com.google.gson.Gson;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

@Data
public class PlayerData {
    private static final Gson GSON = new Gson();

    private final UUID uuid;
    private double points = 0;
    private Map<String, Boolean> autosellMap = new HashMap<>();

    public Player toPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void addPoints(double p) {
        this.points += p;
    }

    public void removePoints(double p) {
        this.points -= p;
    }

    public void setAutosell(String itemId, boolean enabled) {
        autosellMap.put(itemId, enabled);
    }

    public boolean isAutosell(String itemId) {
        return autosellMap.getOrDefault(itemId, false);
    }

    public String serializeAutosell() {
        return GSON.toJson(autosellMap);
    }

    public void deserializeAutosell(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            Type type = new TypeToken<Map<String, Boolean>>(){}.getType();
            Map<String, Boolean> map = GSON.fromJson(json, type);
            if (map != null) {
                this.autosellMap = new HashMap<>(map);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("Ошибка при десериализации autosell для игрока " + uuid + ": " + e.getMessage());
        }
    }
}