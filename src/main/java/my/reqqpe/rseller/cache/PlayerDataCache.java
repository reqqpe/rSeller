package my.reqqpe.rseller.cache;

import my.reqqpe.rseller.database.PlayerData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataCache {

    private static Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public static void add(UUID uuid, PlayerData playerData) {
        cache.put(uuid, playerData);
    }
    public static void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public static PlayerData getOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerData::new);
    }

    public static boolean has(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public static Map<UUID, PlayerData> getCache(){
        return Map.copyOf(cache);
    }
}
