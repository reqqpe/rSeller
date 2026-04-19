package my.reqqpe.rseller.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeadUtil {

    private static final Map<String, PlayerProfile> profileCache = new ConcurrentHashMap<>();

    public static ItemStack getCustomHead(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        PlayerProfile profile = profileCache.computeIfAbsent(base64, key -> {
            PlayerProfile p = Bukkit.createProfile(UUID.nameUUIDFromBytes(key.getBytes()));
            p.setProperty(new ProfileProperty("textures", key));
            return p;
        });

        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);

        return head;
    }





    public static void clearCache() {
        profileCache.clear();
    }
}

