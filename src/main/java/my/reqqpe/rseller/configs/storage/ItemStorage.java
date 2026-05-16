package my.reqqpe.rseller.configs.storage;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.models.item.Item;
import org.bukkit.configuration.file.FileConfiguration;

public class ItemStorage {

    private final Main plugin;

    public ItemStorage(Main plugin) {
        this.plugin = plugin;
    }

    public boolean save(Item item) {

        FileConfiguration config = plugin.getItemsConfig().getConfig();

        config.set("items." + item.id() + ".price", item.price());
        config.set("items." + item.id() + ".points", item.points());

        plugin.getItemsConfig().saveConfig();

        plugin.getLogger().info(plugin.getMessageConfig().getConsoleItemSaved().replace("{id}", item.id()));
        return true;
    }

    public boolean delete(String id) {

        FileConfiguration config = plugin.getItemsConfig().getConfig();

        config.set("items." + id, null);
        config.set("custom-items." + id, null);

        plugin.getItemsConfig().saveConfig();

        plugin.getLogger().info(plugin.getMessageConfig().getConsoleItemDeleted().replace("{id}", id));
        return true;
    }
}
