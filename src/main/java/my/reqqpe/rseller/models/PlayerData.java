package my.reqqpe.rseller.models;

import com.google.gson.Gson;
import lombok.Data;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class PlayerData {
    private static final Gson GSON = new Gson();

    private final UUID uuid;
    @Setter
    private double points;
    @Setter
    private long level;
    private Map<String, Boolean> autosellMap = new HashMap<>();


    public void addPoints(double points) {
        this.points += points;
    }
    public void addLevel(long level) {
        this.level += level;
    }
    public void removeLevel(long level) {
        this.level -= level;
    }
    public void removePoints(double points) {
        this.points -= points;
    }


    public void setAutosell(String id, boolean enabled) {
        autosellMap.put(id, enabled);
    }

    public boolean isAutosell(String id) {
        return autosellMap.getOrDefault(id, false);
    }

    public String serializeAutosell() {
        return GSON.toJson(autosellMap);
    }

    public void deserializeAutosell(String json) {
        if (json == null || json.isEmpty()) return;
        autosellMap = GSON.fromJson(json, Map.class);
    }

    public boolean hasEnabledAutosell() {
        return autosellMap.values().stream().anyMatch(b -> b);
    }
}
