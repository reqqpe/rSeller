package my.reqqpe.rseller.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomyProvider implements EconomyProvider{


    private final Economy econ;

    public VaultEconomyProvider() {
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        econ = rsp.getProvider();
    }

    @Override
    public double getBalance(Player player) {
        return econ.getBalance(player);
    }

    @Override
    public void deposit(Player player, double amount) {
        econ.depositPlayer(player, amount);
    }

    @Override
    public void withdraw(Player player, double amount) {
        econ.withdrawPlayer(player, amount);
    }
}
