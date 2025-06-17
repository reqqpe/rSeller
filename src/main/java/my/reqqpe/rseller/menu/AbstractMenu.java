package my.reqqpe.rseller.menu;


import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractMenu {
    protected final Main plugin;
    protected FileConfiguration guiConfig;

    public AbstractMenu(Main plugin) {
        this.plugin = plugin;
    }
    protected abstract FileConfiguration getGuiConfig();
    protected abstract String getMenuId();

    public void openMenu(Player player) {
        this.guiConfig = getGuiConfig();

        String title = Colorizer.color(guiConfig.getString("title", "Скупщик"));
        int size = guiConfig.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) {
            size = 54;
        }
        Inventory inv = Bukkit.createInventory(new CustomInventoryHolder(getMenuId()), size, title);

        populateStaticItems(inv);

        player.openInventory(inv);
    }

    protected void populateStaticItems(Inventory inv) {
        Set<Integer> usedSlots = new HashSet<>();
        List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("sell-slots"));
        usedSlots.addAll(sellSlots);

        ConfigurationSection itemsSection = guiConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                String path = "items." + key;
                int slot = guiConfig.getInt(path + ".slot", -1);

                if (sellSlots.contains(slot) || slot < 0 || slot >= inv.getSize() || usedSlots.contains(slot)) {
                    continue;
                }

                String matString = guiConfig.getString(path + ".material", "STONE");
                ItemStack item;

                if (matString.toLowerCase().startsWith("basehead-")) {
                    String base64 = matString.substring("basehead-".length());
                    item = HeadUtil.getCustomHead(base64);
                } else {
                    Material material = Material.matchMaterial(matString);
                    if (material == null) {
                        plugin.getLogger().warning("[" + getMenuId() + "] Неизвестный материал для предмета '" + key + "'");
                        continue;
                    }
                    item = new ItemStack(material);
                }

                int customModelData = -1;
                if (guiConfig.contains(path + ".model-data")) {
                    customModelData = guiConfig.getInt(path + ".model-data");
                }

                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String name = guiConfig.getString(path + ".name", "");
                    meta.setDisplayName(Colorizer.color(name));
                    List<String> lore = guiConfig.getStringList(path + ".lore");
                    if (lore != null && !lore.isEmpty()) {
                        meta.setLore(Colorizer.colorizeAll(lore));
                    }
                    if (customModelData != -1) {
                        meta.setCustomModelData(customModelData);
                    }
                    item.setItemMeta(meta);
                }

                inv.setItem(slot, item);
                usedSlots.add(slot);
            }
        }
    }


    protected static List<Integer> parseSlotList(List<String> list) {
        List<Integer> result = new ArrayList<>();
        for (String str : list) {
            if (str.contains("-")) {
                String[] parts = str.split("-");
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                for (int i = start; i <= end; i++) {
                    result.add(i);
                }
            } else {
                result.add(Integer.parseInt(str));
            }
        }
        return result;
    }

    protected ItemStack createGuiItem(Material material, String namePath, String lorePath, int customModelData, Object... replacements) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = guiConfig.getString(namePath, "");
            meta.setDisplayName(Colorizer.color(name));

            List<String> lore = guiConfig.getStringList(lorePath);
            if (lore != null && !lore.isEmpty()) {
                List<String> finalLore = lore.stream().map(line -> {
                    String formattedLine = line;
                    for (int i = 0; i < replacements.length; i += 2) {
                        if (i + 1 < replacements.length) {
                            String placeholder = (String) replacements[i];
                            String value = String.valueOf(replacements[i+1]);
                            formattedLine = formattedLine.replace(placeholder, value);
                        }
                    }
                    return formattedLine;
                }).collect(Collectors.toList());
                meta.setLore(Colorizer.colorizeAll(finalLore));
            }
            if (customModelData != -1) {
                meta.setCustomModelData(customModelData);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
    protected ItemStack createGuiItem(ItemStack itemStack, String namePath, String lorePath, int customModelData, Object... replacements) {
        ItemStack item = itemStack.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = guiConfig.getString(namePath, "");
            meta.setDisplayName(Colorizer.color(name));

            List<String> lore = guiConfig.getStringList(lorePath);
            if (lore != null && !lore.isEmpty()) {
                List<String> finalLore = lore.stream().map(line -> {
                    String formattedLine = line;
                    for (int i = 0; i < replacements.length; i += 2) {
                        if (i + 1 < replacements.length) {
                            String placeholder = (String) replacements[i];
                            String value = String.valueOf(replacements[i+1]);
                            formattedLine = formattedLine.replace(placeholder, value);
                        }
                    }
                    return formattedLine;
                }).collect(Collectors.toList());
                meta.setLore(Colorizer.colorizeAll(finalLore));
            }
            if (customModelData != -1) {
                meta.setCustomModelData(customModelData);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}

