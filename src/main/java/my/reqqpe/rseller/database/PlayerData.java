package my.reqqpe.rseller.database;

import com.google.gson.Gson;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    public void setAutosell(Material mat, boolean enabled) {
        autosellMap.put(mat.name(), enabled);
    }

    public boolean isAutosell(Material mat) {
        return autosellMap.getOrDefault(mat.name(), false);
    }

    public String serializeAutosell() {
        return GSON.toJson(autosellMap);
    }

    public void deserializeAutosell(String json) {
        if (json == null || json.isEmpty()) return;
        autosellMap = GSON.fromJson(json, Map.class);
    }

}