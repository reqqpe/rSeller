package my.reqqpe.rseller.hooks;

import my.reqqpe.rseller.RSeller;
import my.reqqpe.rseller.utils.LoggerUtil;
import my.reqqpe.rseller.utils.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyHook {

    private static Economy econ = null;

    public static boolean setupEconomy(RSeller plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            LoggerUtil.warn(
                    MessageUtil.getString("log.vault-not-found")
            );
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            LoggerUtil.warn(
                    MessageUtil.getString("log.economy-provider-not-found")
            );
            return false;
        }

        econ = rsp.getProvider();

        LoggerUtil.info(
                MessageUtil.getString("log.economy-hooked")
                        .replace("%provider%", econ.getName())
        );
        return true;
    }

    public static Economy get() {
        return econ;
    }
}
