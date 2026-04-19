package my.reqqpe.rseller.economy;


import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI;

public class CoinsEngineEconomyProvider implements EconomyProvider {

    private final ExcellentEconomyAPI api;
    private final String currency;

    public CoinsEngineEconomyProvider(String currencyName) {
        this.currency = currencyName;

        RegisteredServiceProvider<ExcellentEconomyAPI> provider =
                Bukkit.getServer().getServicesManager().getRegistration(ExcellentEconomyAPI.class);

        if (provider == null) {
            throw new IllegalStateException("ExcellentEconomyAPI is not registered!");
        }

        this.api = provider.getProvider();
    }

    @Override
    public double getBalance(Player player) {
        if (api == null) {
            throw new IllegalStateException("Economy API is not initialized!");
        }

        return api.getBalance(player, currency);
    }

    @Override
    public void deposit(Player player, double amount) {
        if (api == null) return;

        api.depositAsync(player.getUniqueId(), currency, amount);
    }

    @Override
    public void withdraw(Player player, double amount) {
        if (api == null) return;

        api.withdrawAsync(player.getUniqueId(), currency, amount);
    }
}
