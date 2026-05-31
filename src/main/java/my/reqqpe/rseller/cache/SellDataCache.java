package my.reqqpe.rseller.cache;

import my.reqqpe.rseller.models.SellData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class SellDataCache {

    private static Map<UUID, SellData> cache = new ConcurrentHashMap<>();

    public static void add(UUID uuid, SellData sellData) {
        cache.put(uuid, sellData);
    }
    public static void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public static SellData getOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, SellData::new);
    }

    public static boolean has(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public static Map<UUID, SellData> getCache(){
        return Map.copyOf(cache);
    }
}
