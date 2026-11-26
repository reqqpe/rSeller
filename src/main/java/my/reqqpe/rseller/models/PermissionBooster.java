package my.reqqpe.rseller.models;

public record PermissionBooster(
        String id,
        String permission,
        double coinMultiplier,
        double pointMultiplier,
        int priority,
        BoosterSource source
) {}
