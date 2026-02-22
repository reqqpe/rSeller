package my.reqqpe.rseller.hooks;

import my.reqqpe.rseller.RSeller;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyHook {

    private static Economy econ = null;

    public static boolean setupEconomy(RSeller plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault не найден! Экономика отключена.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().warning("Не найден провайдер Economy! (убедись что установлен EssentialsX)");
            return false;
        }

        econ = rsp.getProvider();

        plugin.getLogger().info("Экономика подключена через " + econ.getName());
        return true;
    }

    public static Economy get() {
        return econ;
    }
}
