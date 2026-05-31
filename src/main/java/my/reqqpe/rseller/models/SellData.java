package my.reqqpe.rseller.models;

import lombok.Data;

import java.util.UUID;


@Data
public class SellData {
    private final UUID uuid;

    private double points = 0;
    private double money = 0;
    private long count = 0;

    public SellData(UUID uuid) {
        this.uuid = uuid;
    }

    public void addPoints(double points) {
        this.points += points;
    }
    public void addMoney(double money) {
        this.money += money;
    }
    public void addCount(double count) {
        this.count += count;
    }
}
