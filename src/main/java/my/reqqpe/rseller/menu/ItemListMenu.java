package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.managers.MenuManager;
import my.reqqpe.rseller.models.item.Item;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ItemListMenu extends AbstractMenu implements Listener {

    private static final int PAGE_SIZE = 45;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public ItemListMenu(Main plugin) {
        super(plugin);
    }

    @Override
    protected FileConfiguration getGuiConfig() {
        return plugin.getItemListGUIConfig().getConfig();
    }

    @Override
    protected String getMenuId() {
        return "itemListGUI";
    }

    @Override
    public void openMenu(Player player) {
        super.openMenu(player);
        fillItems(player, player.getOpenInventory().getTopInventory());
    }

    private void fillItems(Player player, Inventory inv) {
        List<Item> all = plugin.getItemManager().getAll();
        all.sort(Comparator.comparing(Item::id));

        int page = playerPages.getOrDefault(player.getUniqueId(), 1);
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PAGE_SIZE));
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, all.size());
        List<Item> pageItems = all.subList(start, end);

        for (int i = 0; i < specialSlots.size(); i++) {
            int slot = specialSlots.get(i);
            if (i < pageItems.size()) {
                inv.setItem(slot, buildItemStack(pageItems.get(i)));
            } else {
                inv.setItem(slot, null);
            }
        }

        updateInfoItem(inv, player, page, totalPages, all.size());
    }

    private ItemStack buildItemStack(Item item) {
        Material mat = item.material();
        ItemStack stack = new ItemStack(mat == Material.AIR ? Material.STONE : mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        String displayName = item.itemData() != null && item.itemData().name() != null
                ? item.itemData().name()
                : item.id().toLowerCase().replace("_", " ");

        meta.setDisplayName(Colorizer.color("&f" + displayName));
        meta.setLore(Colorizer.colorizeAll(List.of(
                "",
                "&#54A0F4* &fID: &e" + item.id(),
                "&#54A0F4* &fЦена: &a" + item.price(),
                "&#54A0F4* &fОчки: &b" + item.points(),
                "",
                "&7Нажмите для редактирования"
        )));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "edit_item_id"),
                PersistentDataType.STRING,
                item.id()
        );
        stack.setItemMeta(meta);
        return stack;
    }

    private void updateInfoItem(Inventory inv, Player player, int page, int totalPages, int totalItems) {
        int slot = guiConfig.getInt("items.page_info.slot", 49);
        ItemStack info = inv.getItem(slot);
        if (info == null || info.getType() == Material.AIR) return;

        ItemMeta meta = info.getItemMeta();
        if (meta == null) return;

        String name = guiConfig.getString("items.page_info.name", "&7Страница {page}/{total_pages}");
        meta.setDisplayName(Colorizer.color(name
                .replace("{page}", String.valueOf(page))
                .replace("{total_pages}", String.valueOf(totalPages))));

        List<String> lore = guiConfig.getStringList("items.page_info.lore");
        List<String> formatted = new ArrayList<>();
        for (String line : lore) {
            formatted.add(Colorizer.color(line
                    .replace("{page}", String.valueOf(page))
                    .replace("{total_pages}", String.valueOf(totalPages))
                    .replace("{total_items}", String.valueOf(totalItems))));
        }
        meta.setLore(formatted);
        info.setItemMeta(meta);
        inv.setItem(slot, info);
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        super.handleClick(player, e);
    }

    @Override
    protected boolean handleSpecialSlotClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return true;
        if (!clicked.hasItemMeta()) return true;

        String id = clicked.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "edit_item_id"), PersistentDataType.STRING);
        if (id == null) return true;

        Item item = plugin.getItemManager().getById(id);
        if (item == null) return true;

        ItemEditMenu editMenu = (ItemEditMenu) MenuManager.getMenu("itemEditGUI");
        if (editMenu == null) return true;

        editMenu.openFor(player, item);
        return true;
    }

    @Override
    protected void executeAction(Player player, String action) {
        String a = action.toLowerCase().trim();
        if (a.equals("[next_page]")) {
            changePage(player, 1);
        } else if (a.equals("[prev_page]")) {
            changePage(player, -1);
        } else if (a.equals("[add_item]")) {
            startAddItem(player);
        } else {
            runMainActions(player, action);
        }
    }

    private void startAddItem(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            player.sendMessage(Colorizer.color("&cВозьмите предмет в руку и повторите."));
            return;
        }

        player.closeInventory();
        player.sendMessage(Colorizer.color("&#54A0F4Введите ID для нового предмета &7(cancel — отмена)&f:"));

        plugin.getChatInputManager().awaitString(player, id -> {
            if (!id.matches("[a-zA-Z0-9_]+")) {
                player.sendMessage(Colorizer.color("&cID может содержать только буквы, цифры и подчёркивания."));
                return;
            }

            if (plugin.getItemManager().getById(id) != null) {
                player.sendMessage(Colorizer.color("&cПредмет с ID &e" + id + " &cуже существует."));
                return;
            }

            player.sendMessage(Colorizer.color("&#54A0F4Введите цену &7(0 — без цены)&f:"));

            plugin.getChatInputManager().awaitDouble(player, "price", price -> {
                player.sendMessage(Colorizer.color("&#54A0F4Введите количество очков &7(0 — без очков)&f:"));

                plugin.getChatInputManager().awaitDouble(player, "points", points -> {
                    Item item = Item.fromItemStack(id, hand, price, points, plugin);
                    if (item == null) {
                        player.sendMessage(Colorizer.color("&cОшибка при создании предмета, проверьте консоль."));
                        return;
                    }

                    plugin.getItemStorage().save(item);
                    plugin.getItemsConfig().reload();
                    plugin.getItemManager().reload();

                    player.sendMessage(Colorizer.color("&aПредмет &e" + id + " &aдобавлен. Цена: &f" + price + " &aОчки: &f" + points));

                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> openMenu(player));
                });
            });
        });
    }

    private void changePage(Player player, int dir) {
        List<Item> all = plugin.getItemManager().getAll();
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PAGE_SIZE));
        int current = playerPages.getOrDefault(player.getUniqueId(), 1);
        int next = current + dir;
        if (next < 1 || next > totalPages) return;
        playerPages.put(player.getUniqueId(), next);
        openMenu(player);
    }

    public void refreshFor(Player player) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof CustomInventoryHolder holder)) return;
        if (!holder.id().equals(getMenuId())) return;
        fillItems(player, inv);
    }
}
