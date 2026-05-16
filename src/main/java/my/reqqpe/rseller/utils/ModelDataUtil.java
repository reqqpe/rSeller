package my.reqqpe.rseller.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.List;

@UtilityClass
public class ModelDataUtil {

    private static final Method GET_COMPONENT;
    private static final Method SET_COMPONENT;

    private static final Method HAS_LEGACY;
    private static final Method GET_LEGACY;
    private static final Method SET_LEGACY;

    static {

        Method getComponent = null;
        Method setComponent = null;

        Method hasLegacy = null;
        Method getLegacy = null;
        Method setLegacy = null;

        try {
            Class<?> componentClass =
                    Class.forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");

            getComponent = ItemMeta.class.getMethod("getCustomModelDataComponent");
            setComponent = ItemMeta.class.getMethod("setCustomModelDataComponent", componentClass);

        } catch (Exception ignored) {
        }

        try {
            hasLegacy = ItemMeta.class.getMethod("hasCustomModelData");
            getLegacy = ItemMeta.class.getMethod("getCustomModelData");
            setLegacy = ItemMeta.class.getMethod("setCustomModelData", Integer.class);
        } catch (Exception ignored) {
        }

        GET_COMPONENT = getComponent;
        SET_COMPONENT = setComponent;

        HAS_LEGACY = hasLegacy;
        GET_LEGACY = getLegacy;
        SET_LEGACY = setLegacy;
    }

    public static void setModelData(ItemMeta meta, int data) {

        if (meta == null) {
            return;
        }

        try {
            // 1.21+
            if (GET_COMPONENT != null && SET_COMPONENT != null) {

                Object component = GET_COMPONENT.invoke(meta);

                Method setFloats =
                        component.getClass().getMethod("setFloats", List.class);

                setFloats.invoke(component, List.of((float) data));

                SET_COMPONENT.invoke(meta, component);

                return;
            }

        } catch (Exception ignored) {
        }

        try {
            // legacy
            if (SET_LEGACY != null) {
                SET_LEGACY.invoke(meta, data);
            }

        } catch (Exception ignored) {
        }
    }

    public static Integer getModelData(ItemMeta meta) {

        if (meta == null) {
            return null;
        }

        try {

            // 1.21+
            if (GET_COMPONENT != null) {

                Object component = GET_COMPONENT.invoke(meta);

                Method getFloats =
                        component.getClass().getMethod("getFloats");

                List<Float> floats =
                        (List<Float>) getFloats.invoke(component);

                if (!floats.isEmpty()) {
                    return floats.getFirst().intValue();
                }
            }

        } catch (Exception ignored) {
        }

        try {

            // legacy
            if (HAS_LEGACY != null &&
                    (boolean) HAS_LEGACY.invoke(meta)) {

                return (Integer) GET_LEGACY.invoke(meta);
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    public static boolean hasModelData(ItemMeta meta) {
        return getModelData(meta) != null;
    }
}