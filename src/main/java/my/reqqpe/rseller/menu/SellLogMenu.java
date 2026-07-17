package my.reqqpe.rseller.menu;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.models.SellLog;
import my.reqqpe.rseller.models.SellSource;
import my.reqqpe.rseller.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SellLogMenu extends AbstractMenu {

    private static final String MENU_ID = "sellLogGUI";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").withZone(ZoneId.systemDefault());

    public static final DateTimeFormatter INPUT_DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public enum SortField {
        DATE, COINS, POINTS, AMOUNT;

        public SortField next() {
            SortField[] v = values();
            return v[(ordinal() + 1) % v.length];
        }

        public String label() {
            return switch (this) {
                case DATE -> "Дата";
                case COINS -> "Монеты";
                case POINTS -> "Очки";
                case AMOUNT -> "Количество";
            };
        }

        public static SortField from(String s) {
            if (s == null) return DATE;
            return switch (s.toLowerCase()) {
                case "coins" -> COINS;
                case "points" -> POINTS;
                case "amount" -> AMOUNT;
                default -> DATE;
            };
        }
    }

    public enum SortOrder {
        DESC, ASC;

        public SortOrder toggle() {
            return this == DESC ? ASC : DESC;
        }

        public String label() {
            return this == DESC ? "↓ убыв." : "↑ возр.";
        }

        public static SortOrder from(String s) {
            return "asc".equalsIgnoreCase(s) ? ASC : DESC;
        }
    }

    public record DateFilter(long from, long to) {

        public boolean matches(SellLog log) {
            if (from != -1 && log.soldAt() < from) return false;
            if (to != -1 && log.soldAt() > to) return false;
            return true;
        }

        public String fromLabel() {
            return from == -1 ? "—"
                    : INPUT_DATE_FMT.format(Instant.ofEpochSecond(from).atZone(ZoneId.systemDefault()));
        }

        public String toLabel() {
            return to == -1 ? "—"
                    : INPUT_DATE_FMT.format(Instant.ofEpochSecond(to).atZone(ZoneId.systemDefault()));
        }
    }

    private static final class ViewerState {
        List<SellLog> rawLogs;
        List<SellLog> sortedLogs;
        int page;
        String target;
        DateFilter filter;
        SortField sortField;
        SortOrder sortOrder;
        boolean allMode;

        ViewerState(List<SellLog> rawLogs, String target, DateFilter filter,
                    SortField sortField, SortOrder sortOrder, boolean allMode) {
            this.rawLogs = rawLogs;
            this.page = 0;
            this.target = target;
            this.filter = filter;
            this.sortField = sortField;
            this.sortOrder = sortOrder;
            this.allMode = allMode;
        }

        int totalPages(int pageSize) {
            return Math.max(1, (int) Math.ceil((double) sortedLogs.size() / pageSize));
        }

        void applySort() {
            Comparator<SellLog> cmp = switch (sortField) {
                case COINS -> Comparator.comparingDouble(SellLog::coins);
                case POINTS -> Comparator.comparingDouble(SellLog::points);
                case AMOUNT -> Comparator.comparingInt(SellLog::amount);
                case DATE -> Comparator.comparingLong(SellLog::soldAt);
            };
            if (sortOrder == SortOrder.DESC) cmp = cmp.reversed();
            sortedLogs = rawLogs.stream().sorted(cmp).toList();
        }
    }

    private final Map<UUID, ViewerState> viewers = new HashMap<>();
    private final Set<UUID> reopening = new HashSet<>();

    private String logItemName;
    private List<String> logItemLore;
    private Material logItemFallback;
    private boolean logItemConfigLoaded = false;

    private final Map<Long, String> dateCache = new HashMap<>();

    public SellLogMenu(Main plugin) {
        super(plugin);
    }

    @Override
    protected FileConfiguration getGuiConfig() {
        return plugin.getSellLogGUIConfig().getConfig();
    }

    @Override
    protected String getMenuId() {
        return MENU_ID;
    }

    public void openFor(Player viewer, UUID targetUuid, String targetName) {
        openFor(viewer, targetUuid, targetName, null, SortField.DATE, SortOrder.DESC);
    }

    public void openFor(Player viewer, UUID targetUuid, String targetName, DateFilter filter) {
        openFor(viewer, targetUuid, targetName, filter, SortField.DATE, SortOrder.DESC);
    }

    public void openFor(Player viewer, UUID targetUuid, String targetName,
                        DateFilter filter, SortField sortField, SortOrder sortOrder) {
        plugin.getSellLogRepository()
                .getByPlayer(targetUuid, 10_000)
                .thenAccept(allLogs -> Bukkit.getScheduler().runTask(plugin, () -> {
                    List<SellLog> filtered = applyFilter(allLogs, filter);
                    initState(viewer, filtered, targetName, filter, sortField, sortOrder, false);
                }));
    }

    public void openForAll(Player viewer, String label,
                           DateFilter filter, SortField sortField, SortOrder sortOrder) {
        plugin.getSellLogRepository()
                .getAllLogs(10_000)
                .thenAccept(allLogs -> Bukkit.getScheduler().runTask(plugin, () -> {
                    List<SellLog> filtered = applyFilter(allLogs, filter);
                    initState(viewer, filtered, label, filter, sortField, sortOrder, true);
                }));
    }

    private List<SellLog> applyFilter(List<SellLog> logs, DateFilter filter) {
        if (filter == null) return new ArrayList<>(logs);
        return logs.stream()
                .filter(filter::matches)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void initState(Player viewer, List<SellLog> filtered, String target,
                           DateFilter filter, SortField sortField, SortOrder sortOrder, boolean allMode) {
        ViewerState state = new ViewerState(filtered, target, filter, sortField, sortOrder, allMode);
        state.applySort();
        viewers.put(viewer.getUniqueId(), state);
        ensureLogItemConfig();
        openMenu(viewer);
    }

    @Override
    public void openMenu(Player player) {
        this.guiConfig = getGuiConfig();

        ViewerState state = getState(player);
        int pageSize = specialSlots.isEmpty() ? 45 : specialSlots.size();

        String rawTitle = guiConfig.getString("title", "&8Логи продаж: &f{target} &8[{page}/{total}]");
        String title = Colorizer.color(rawTitle
                .replace("{target}", state.target)
                .replace("{page}", String.valueOf(state.page + 1))
                .replace("{total}", String.valueOf(state.totalPages(pageSize))));

        int size = guiConfig.getInt("size", 54);
        if (size % 9 != 0 || size < 9 || size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(new CustomInventoryHolder(getMenuId()), size, title);

        loadItems(inv, player);
        fillLogSlots(state, inv, pageSize);

        reopening.add(player.getUniqueId());
        player.openInventory(inv);
        reopening.remove(player.getUniqueId());
        startItemUpdates(player, inv);
    }

    @Override
    protected String replacePlaceholders(Player player, String text, Inventory inventory) {
        if (text == null || text.isEmpty()) return "";

        ViewerState state = getState(player);
        int pageSize = specialSlots.isEmpty() ? 45 : specialSlots.size();

        return text
                .replace("{target}", state.target)
                .replace("{page}", String.valueOf(state.page + 1))
                .replace("{total}", String.valueOf(state.totalPages(pageSize)))
                .replace("{total_entries}", String.valueOf(state.sortedLogs.size()))
                .replace("{filter_from}", state.filter != null ? state.filter.fromLabel() : "—")
                .replace("{filter_to}", state.filter != null ? state.filter.toLabel() : "—")
                .replace("{sort_field}", state.sortField.label())
                .replace("{sort_order}", state.sortOrder.label());
    }

    @Override
    protected List<String> replacePlaceholders(Player player, List<String> list, Inventory inventory) {
        List<String> result = new ArrayList<>(list.size());
        for (String line : list) result.add(replacePlaceholders(player, line, inventory));
        return result;
    }

    private void fillLogSlots(ViewerState state, Inventory inv, int pageSize) {
        int start = state.page * pageSize;

        for (int i = 0; i < specialSlots.size(); i++) {
            int slot = specialSlots.getInt(i);
            int logIndex = start + i;

            if (logIndex >= state.sortedLogs.size()) {
                inv.setItem(slot, null);
                continue;
            }
            inv.setItem(slot, buildLogItem(state.sortedLogs.get(logIndex)));
        }
    }

    private ItemStack buildLogItem(SellLog log) {
        Material mat = Material.matchMaterial(log.itemId());
        if (mat == null || mat == Material.AIR) mat = logItemFallback;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String date = dateCache.computeIfAbsent(log.soldAt(),
                ts -> DATE_FMT.format(Instant.ofEpochSecond(ts)));

        meta.setDisplayName(Colorizer.color(applyLogPlaceholders(logItemName, log, date)));

        List<String> lore = new ArrayList<>(logItemLore.size());
        for (String line : logItemLore) {
            lore.add(Colorizer.color(applyLogPlaceholders(line, log, date)));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String applyLogPlaceholders(String text, SellLog log, String date) {
        return text
                .replace("{player}", log.playerName().isEmpty() ? log.uuid().toString() : log.playerName())
                .replace("{item_id}", log.itemId())
                .replace("{amount}", String.valueOf(log.amount()))
                .replace("{coins}", String.format("%.2f", log.coins()))
                .replace("{points}", String.format("%.2f", log.points()))
                .replace("{source}", sourceLabel(log.source()))
                .replace("{date}", date)
                .replace("{level}", String.valueOf(log.level()))
                .replace("{coin_booster}", String.format("x%.2f", log.coinBooster()))
                .replace("{point_booster}", String.format("x%.2f", log.pointBooster()))
                .replace("{world}", log.world());
    }

    @Override
    public void handleClick(Player player, InventoryClickEvent e) {
        e.setCancelled(true);
        super.handleClick(player, e);
    }

    @Override
    protected boolean handleSpecialSlotClick(Player player, InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof CustomInventoryHolder holder)) return false;
        if (!holder.id().equals(getMenuId())) return false;

        if (specialSlots.contains(e.getRawSlot())) {
            e.setCancelled(true);
            return true;
        }
        return false;
    }

    @Override
    protected void executeAction(Player player, String action) {
        switch (action.toLowerCase().trim()) {
            case "[next_page]" -> switchPage(player, 1);
            case "[prev_page]" -> switchPage(player, -1);
            case "[toggle_sort]" -> toggleSort(player);
            case "[toggle_order]" -> toggleOrder(player);
            default -> runMainActions(player, action);
        }
    }

    private void switchPage(Player player, int direction) {
        ViewerState state = getState(player);
        int pageSize = specialSlots.isEmpty() ? 45 : specialSlots.size();
        int newPage = state.page + direction;

        if (newPage >= 0 && newPage < state.totalPages(pageSize)) {
            state.page = newPage;
            openMenu(player);
        }
    }

    private void toggleSort(Player player) {
        ViewerState state = getState(player);
        state.sortField = state.sortField.next();
        state.page = 0;
        state.applySort();
        openMenu(player);
    }

    private void toggleOrder(Player player) {
        ViewerState state = getState(player);
        state.sortOrder = state.sortOrder.toggle();
        state.page = 0;
        state.applySort();
        openMenu(player);
    }

    @Override
    public void closeMenu(Player player, InventoryCloseEvent e) {
        UUID uuid = player.getUniqueId();
        if (reopening.contains(uuid)) return;
        viewers.remove(uuid);
        super.closeMenu(player, e);
    }

    private ViewerState getState(Player player) {
        return viewers.computeIfAbsent(player.getUniqueId(), id -> {
            ViewerState def = new ViewerState(List.of(), player.getName(), null,
                    SortField.DATE, SortOrder.DESC, false);
            def.sortedLogs = List.of();
            return def;
        });
    }

    private void ensureLogItemConfig() {
        if (logItemConfigLoaded) return;
        FileConfiguration cfg = getGuiConfig();
        logItemName = cfg.getString("log_item.name", "&e{item_id} &7x{amount}");
        logItemLore = cfg.getStringList("log_item.lore");
        String fallback = cfg.getString("log_item.fallback-material", "PAPER");
        logItemFallback = Material.matchMaterial(fallback);
        if (logItemFallback == null) logItemFallback = Material.PAPER;
        logItemConfigLoaded = true;
    }

    public void invalidateConfig() {
        logItemConfigLoaded = false;
        dateCache.clear();
    }

    private static String sourceLabel(SellSource source) {
        return switch (source) {
            case GUI -> "GUI";
            case MENU_ACTION -> "Меню";
            case AUTOSELL -> "Авто-продажа";
        };
    }
}
