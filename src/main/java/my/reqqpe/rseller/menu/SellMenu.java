package my.reqqpe.rseller.menu;


import me.clip.placeholderapi.PlaceholderAPI;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.managers.NumberFormatManager;
import my.reqqpe.rseller.utils.Colorizer;
import my.reqqpe.rseller.utils.SyntaxParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class SellMenu extends AbstractMenu implements Listener {
    private final Map<UUID, List<BukkitTask>> menuUpdateTasks = new HashMap<>();


    public SellMenu(Main plugin) {
        super(plugin);
    }

    @Override
    protected FileConfiguration getGuiConfig() {
        return plugin.getMainGUI().getConfig();
    }

    @Override
    public String getMenuId() {
        return "SELL_MENU";
    }


    @Override
    public void openMenu(Player player) {
        super.openMenu(player);

        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;


        updatePlaceholders(player, inv);
        startAutoUpdates(player, inv);
    }
    private void updatePlaceholders(Player player, Inventory inv) {
        var result = plugin.getSellManager().calculateSellPreview(player, inv, new ArrayList<>(special_slots));

        NumberFormatManager numberFormatManager = new NumberFormatManager(plugin.getConfig());

        String coinsFormatted = numberFormatManager.format("mainGUI.sell_price", result.coins());
        String pointsFormatted = numberFormatManager.format("mainGUI.sell_points", result.points());

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            boolean changed = false;

            if (meta.hasDisplayName()) {
                String displayName = meta.getDisplayName()
                        .replace("{sell_price}", coinsFormatted)
                        .replace("{sell_points}", pointsFormatted);
                displayName = PlaceholderAPI.setPlaceholders(player, displayName);
                meta.setDisplayName(displayName);
                changed = true;
            }

            if (meta.hasLore()) {
                List<String> newLore = new ArrayList<>();
                for (String line : meta.getLore()) {
                    String processedLine = line
                            .replace("{sell_price}", coinsFormatted)
                            .replace("{sell_points}", pointsFormatted);
                    processedLine = PlaceholderAPI.setPlaceholders(player, processedLine);
                    newLore.add(processedLine);
                }
                meta.setLore(newLore);
                changed = true;
            }

            if (changed) {
                item.setItemMeta(meta);
                inv.setItem(i, item);
            }
        }
    }

    private void startAutoUpdates(Player player, Inventory inv) {
        FileConfiguration config = getGuiConfig();
        NumberFormatManager numberFormatManager = new NumberFormatManager(plugin.getConfig());
        List<BukkitTask> tasks = new ArrayList<>();

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            return;
        }


        for (String itemId : itemsSection.getKeys(false)) {
            String path = "items." + itemId;
            int slot = config.getInt(path + ".slot", -1);
            if (slot == -1 || slot >= inv.getSize()) {
                continue;
            }

            int interval = config.getInt(path + ".update_interval", -1);
            if (interval <= 0) {
                continue;
            }

            String configDisplayName = config.getString(path + ".name", "");
            List<String> configLore = config.getStringList(path + ".lore");


            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }

                if (!player.getOpenInventory().getTopInventory().equals(inv)) {
                    return;
                }

                var result = plugin.getSellManager().calculateSellPreview(player, inv, new ArrayList<>(special_slots));
                String coinsFormatted = numberFormatManager.format("mainGUI.sell_price", result.coins());
                String pointsFormatted = numberFormatManager.format("mainGUI.sell_points", result.points());


                ItemStack item = inv.getItem(slot);
                if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
                    return;
                }

                ItemMeta meta = item.getItemMeta();
                boolean changed = false;

                if (!configDisplayName.isEmpty()) {
                    String updatedDisplayName = configDisplayName
                            .replace("{sell_price}", coinsFormatted)
                            .replace("{sell_points}", pointsFormatted);
                    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                        updatedDisplayName = PlaceholderAPI.setPlaceholders(player, updatedDisplayName);
                    } else {
                    }
                    if (!updatedDisplayName.equals(meta.hasDisplayName() ? meta.getDisplayName() : "")) {
                        meta.setDisplayName(Colorizer.color(updatedDisplayName));
                        changed = true;
                    }
                }

                if (!configLore.isEmpty()) {
                    List<String> newLore = new ArrayList<>();
                    for (String line : configLore) {
                        String processedLine = line
                                .replace("{sell_price}", coinsFormatted)
                                .replace("{sell_points}", pointsFormatted);
                        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                            String beforePAPI = processedLine;
                            processedLine = PlaceholderAPI.setPlaceholders(player, processedLine);

                        } else {

                        }
                        newLore.add(processedLine);
                    }
                    if (!newLore.equals(meta.hasLore() ? meta.getLore() : new ArrayList<>())) {
                        meta.setLore(Colorizer.colorizeAll(newLore));
                        changed = true;
                    }
                }

                if (changed) {
                    item.setItemMeta(meta);
                    inv.setItem(slot, item);
                    player.updateInventory();
                }

            }, interval, interval);

            tasks.add(task);
        }

        cancelUpdates(player);
        menuUpdateTasks.put(player.getUniqueId(), tasks);
    }
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;

        List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("specials-slots"));
        Inventory inv = e.getInventory();

        for (int slot : sellSlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
                for (ItemStack leftover : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }
        cancelUpdates(player);
    }

    @EventHandler
    public void clickInventory(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.getId().equals(getMenuId())) return;
        
        Set<Integer> protectedSlots = slotToItemId.keySet();

        int rawSlot = e.getRawSlot();
        Inventory clickedInv = e.getClickedInventory();
        Inventory menuInv = e.getInventory();

        if (protectedSlots.contains(rawSlot) && clickedInv == menuInv) {
            e.setCancelled(true);
        }

        if (clickedInv == menuInv && rawSlot < menuInv.getSize()) {
            String itemId = slotToItemId.get(rawSlot);
            if (itemId == null) return;

            ConfigurationSection itemSection = guiConfig.getConfigurationSection("items." + itemId);
            if (itemSection == null) return;

            boolean isLeftClick = e.isLeftClick();
            boolean isRightClick = e.isRightClick();

            ConfigurationSection reqSection = itemSection.getConfigurationSection(
                    isLeftClick ? "left_click_requaments" :
                            isRightClick ? "right_click_requaments" : null);

            boolean allRequirementsPassed = true;

            if (reqSection != null) {
                ConfigurationSection sub = reqSection.getConfigurationSection("requaments");
                if (sub != null && !sub.getKeys(false).isEmpty()) {
                    for (String key : sub.getKeys(false)) {
                        ConfigurationSection entry = sub.getConfigurationSection(key);
                        if (entry == null) continue;

                        String syntax = entry.getString("syntax");
                        String typeRaw = entry.getString("type", "AUTO");
                        SyntaxParser.Type type = SyntaxParser.Type.valueOf(typeRaw.toUpperCase());

                        if (!SyntaxParser.parse(syntax, type)) {
                            player.sendMessage("§cУсловие не выполнено: " + syntax);
                            allRequirementsPassed = false;
                            break;
                        }
                    }
                }
            }

            if (!allRequirementsPassed) {
                List<String> denyCommands = reqSection != null ? reqSection.getStringList("deny_commands") : Collections.emptyList();
                for (String cmd : denyCommands) {
                    runCommand(player, cmd);
                }
                return;
            }

            List<String> actions = itemSection.getStringList(
                    isLeftClick ? "left_click_actions" :
                            isRightClick ? "right_click_actions" : null);

            for (String action : actions) {
                executeAction(player, action);
            }
        }
    }
    private void runCommand(Player player, String cmdLine) {
        String cmd = cmdLine.trim();
        if (cmd.startsWith("[console] ")) {
            String command = cmd.substring(10).replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else if (cmd.startsWith("[player] ")) {
            String command = cmd.substring(9).replace("%player%", player.getName());
            player.performCommand(command);
        } else if (cmd.startsWith("[text] ")) {
            String message = Colorizer.color(cmd.substring(7).replace("%player%", player.getName()));
            player.sendMessage(message);
        } else if (cmd.startsWith("[opengui] ")) {
            String guiId = cmd.substring(10).trim();
            if (guiId.equalsIgnoreCase("autoSellGUI")) {
                plugin.getAutoSellMenu().openMenu(player);
            }
            else if (guiId.equalsIgnoreCase("mainGUI")) {
                openMenu(player);
            }
        } else {
            player.sendMessage(Colorizer.color(cmd));
        }
    }

    private void executeAction(Player player, String action) {
        if (action.equalsIgnoreCase("[sell]")) {
            Inventory inv = player.getOpenInventory().getTopInventory();
            if (!(inv.getHolder() instanceof CustomInventoryHolder holder)) return;
            if (!holder.getId().equals(getMenuId())) return;

            List<Integer> sellSlots = parseSlotList(guiConfig.getStringList("special-slots"));
            plugin.getSellManager().sellItems(player, inv, sellSlots);
        } else {
            runCommand(player, action);
        }
    }

    private void cancelUpdates(Player player) {
        List<BukkitTask> tasks = menuUpdateTasks.remove(player.getUniqueId());
        if (tasks != null) {
            tasks.forEach(BukkitTask::cancel);
        }
    }
}