package my.reqqpe.rseller.database;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.UUID;

// Аннотация @Data создаёт гетеры и сетеры для всех полей, конструктор для final полей
// Переопределяет методы equals hashcode и tostring
@Data
public class PlayerData {
    private final UUID uuid;
    private int points = 0;

    public Player toPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void addPoints(int p) {
        this.points += p;
    }

    public void removePoints(int p) {
        this.points += p;
    }
}
