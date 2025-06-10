package my.reqqpe.rseller.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

public class HeadUtil {

    private static final Map<String, GameProfile> profileCache = new ConcurrentHashMap<>();

    public static ItemStack getCustomHead(String base64) {
        if (base64 == null || base64.isEmpty()) return new ItemStack(Material.PLAYER_HEAD);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        GameProfile profile = profileCache.computeIfAbsent(base64, b64 -> {
            GameProfile p = new GameProfile(UUID.nameUUIDFromBytes(b64.getBytes()), null);
            p.getProperties().put("textures", new Property("textures", b64));
            return p;
        });

        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        head.setItemMeta(meta);
        return head;
    }
}

