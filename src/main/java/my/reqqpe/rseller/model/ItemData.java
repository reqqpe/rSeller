package my.reqqpe.rseller.model;


import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class ItemData {
    private final double price;
    private final double points;
    private final String name;
}
