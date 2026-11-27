package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.managers.MenuManager;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemEditMenu extends AbstractMenu implements Listener {

    private final Map<UUID, EditSession> sessions = new HashMap<>();

    private static class EditSession {
        String id;
        double price;
        double points;

        EditSession(Item item) {
            this.id = item.id();
            this.price = item.price();
            this.points = item.points();
        }
    }

    public ItemEditMenu(Main plugin) {
        super(plugin);
    }

    @Override
    protected FileConfiguration getGuiConfig() {
        return plugin.getItemEditGUIConfig().getConfig();
    }

    @Override
    protected String getMenuId() {
        return "itemEditGUI";
    }

    public void openFor(Player player, Item item) {
        sessions.put(player.getUniqueId(), new EditSession(item));
        openMenu(player);
    }

    @Override
    public void openMenu(Player player) {
        EditSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        this.guiConfig = getGuiConfig();

        String rawTitle = guiConfig.getString("title", "&8Редактор");
        String title = Colorizer.color(rawTitle.replace("{item_id}", session.id));
        int size = guiConfig.getInt("size", 54);
        Inventory inv = Bukkit.createInventory(new CustomInventoryHolder(getMenuId()), size, title);

        loadItems(inv, player);
        placeDisplayItem(inv, player, session);

        player.openInventory(inv);
    }

    @Override
    protected String replacePlaceholders(Player player, String text, Inventory inventory) {
        EditSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            text = applySession(text, session);
        }
        return super.replacePlaceholders(player, text, inventory);
    }

    private void placeDisplayItem(Inventory inv, Player player, EditSession session) {
        Item item = plugin.getItemManager().getById(session.id);
        if (item == null) return;

        int slot = guiConfig.getInt("items.item_display.slot", 14);

        ItemStack stack = new ItemStack(item.material());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        String displayName = item.itemData() != null && item.itemData().name() != null
                ? item.itemData().name()
                : item.id().toLowerCase().replace("_", " ");

        meta.setDisplayName(Colorizer.color("&f" + displayName));

        List<String> lore = guiConfig.getStringList("items.item_display.lore");
        List<String> formatted = new ArrayList<>();
        for (String line : lore) {
            formatted.add(Colorizer.color(applySession(line, session)));
        }
        meta.setLore(formatted);
        stack.setItemMeta(meta);
        inv.setItem(slot, stack);
    }

    private void refreshDisplay(Inventory inv, Player player) {
        EditSession session = sessions.get(player.getUniqueId());
        if (session == null) return;
        placeDisplayItem(inv, player, session);
        refreshButton(inv, "price_info", session);
        refreshButton(inv, "points_info", session);
    }

    private void refreshButton(Inventory inv, String itemId, EditSession session) {
        int slot = guiConfig.getInt("items." + itemId + ".slot", -1);
        if (slot < 0) return;

        ItemStack stack = inv.getItem(slot);
        if (stack == null || !stack.hasItemMeta()) return;

        ItemMeta meta = stack.getItemMeta();
        List<String> lore = guiConfig.getStringList("items." + itemId + ".lore");
        List<String> formatted = new ArrayList<>();
        for (String line : lore) {
            formatted.add(Colorizer.color(applySession(line, session)));
        }
        meta.setLore(formatted);
        stack.setItemMeta(meta);
        inv.setItem(slot, stack);
    }

    private String applySession(String text, EditSession session) {
        Item item = plugin.getItemManager().getById(session.id);
        String displayName = (item != null && item.itemData() != null && item.itemData().name() != null)
                ? item.itemData().name()
                : session.id.toLowerCase().replace("_", " ");
        String material = item != null ? item.material().name() : "UNKNOWN";

        return text
                .replace("{item_id}", session.id)
                .replace("{item_name}", displayName)
                .replace("{item_material}", material)
                .replace("{item_price}", formatValue(session.price))
                .replace("{item_points}", formatValue(session.points));
    }

    private String formatValue(double value) {
        if (value == Math.floor(value)) return String.valueOf((long) value);
        return String.format("%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        super.handleClick(player, e);
    }

    @Override
    protected void executeAction(Player player, String action) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.id().equals(getMenuId())) return;

        EditSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        String a = action.trim();

        if (a.toLowerCase().startsWith("[price] ")) {
            double delta = parseDelta(a.substring("[price] ".length()));
            session.price = Math.max(0, Math.round((session.price + delta) * 100.0) / 100.0);
            refreshDisplay(inv, player);
            return;
        }

        if (a.toLowerCase().startsWith("[points] ")) {
            double delta = parseDelta(a.substring("[points] ".length()));
            session.points = Math.max(0, Math.round((session.points + delta) * 100.0) / 100.0);
            refreshDisplay(inv, player);
            return;
        }

        if (a.toLowerCase().equals("[price_input]")) {
            player.closeInventory();
            player.sendMessage(Colorizer.color("&#54A0F4Введите новую цену в чат:"));
            plugin.getChatInputManager().awaitDouble(player, "price", value -> {
                session.price = Math.max(0, value);
                sessions.put(player.getUniqueId(), session);
                openMenu(player);
            });
            return;
        }

        if (a.toLowerCase().equals("[points_input]")) {
            player.closeInventory();
            player.sendMessage(Colorizer.color("&#54A0F4Введите новые очки в чат:"));
            plugin.getChatInputManager().awaitDouble(player, "points", value -> {
                session.points = Math.max(0, value);
                sessions.put(player.getUniqueId(), session);
                openMenu(player);
            });
            return;
        }

        if (a.toLowerCase().equals("[save]")) {
            Item original = plugin.getItemManager().getById(session.id);
            if (original == null) return;

            Item updated = new Item(session.id, original.material(), original.itemData(),
                    session.price, session.points);

            plugin.getItemStorage().save(updated);
            plugin.getItemsConfig().reload();
            plugin.getItemManager().reload();

            player.sendMessage(Colorizer.color("&aПредмет &e" + session.id + " &aсохранён. Цена: &f"
                    + formatValue(session.price) + " &aОчки: &f" + formatValue(session.points)));

            ItemListMenu listMenu = (ItemListMenu) MenuManager.getMenu("itemListGUI");
            if (listMenu != null) listMenu.openMenu(player);
            return;
        }

        if (a.toLowerCase().equals("[delete]")) {
            plugin.getItemStorage().delete(session.id);
            plugin.getItemsConfig().reload();
            plugin.getItemManager().reload();

            player.sendMessage(Colorizer.color("&cПредмет &e" + session.id + " &cудалён."));
            sessions.remove(player.getUniqueId());

            ItemListMenu listMenu = (ItemListMenu) MenuManager.getMenu("itemListGUI");
            if (listMenu != null) listMenu.openMenu(player);
            return;
        }

        if (a.toLowerCase().equals("[back]")) {
            sessions.remove(player.getUniqueId());
            ItemListMenu listMenu = (ItemListMenu) MenuManager.getMenu("itemListGUI");
            if (listMenu != null) listMenu.openMenu(player);
            return;
        }

        runMainActions(player, action);
    }

    @Override
    public void closeMenu(Player player, org.bukkit.event.inventory.InventoryCloseEvent e) {
        super.closeMenu(player, e);
    }

    private double parseDelta(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
