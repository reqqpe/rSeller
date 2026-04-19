package my.reqqpe.rseller.economy;

import org.bukkit.entity.Player;

public interface EconomyProvider {
    double getBalance(Player player);
    void deposit(Player player, double amount);
    void withdraw(Player player, double amount);
}
