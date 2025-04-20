package my.reqqpe.rseller;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomySetup {
    private static Economy econ = null;

    public static void setupEconomy(Main plugin) {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) econ = rsp.getProvider();
    }

    public static Economy getEconomy() {
        return econ;
    }
}
