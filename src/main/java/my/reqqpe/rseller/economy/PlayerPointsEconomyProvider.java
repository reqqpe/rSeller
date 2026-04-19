package my.reqqpe.rseller.economy;

import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;

public class PlayerPointsEconomyProvider implements EconomyProvider {

    private final PlayerPointsAPI playerPointsAPI;

    public PlayerPointsEconomyProvider(PlayerPointsAPI playerPointsAPI) {
        this.playerPointsAPI = playerPointsAPI;
    }

    @Override
    public double getBalance(Player player) {
        return playerPointsAPI.look(player.getUniqueId());
    }

    @Override
    public void deposit(Player player, double amount) {
        playerPointsAPI.give(player.getUniqueId(), (int) amount);
    }

    @Override
    public void withdraw(Player player, double amount) {
        playerPointsAPI.take(player.getUniqueId(), (int) amount);
    }
}
