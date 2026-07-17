package my.reqqpe.rseller.api;

import my.reqqpe.rseller.Main;

public final class rSeller {

    private static rSellerAPI instance;

    private rSeller() {}

    public static void init(Main plugin) {
        instance = new rSellerAPIImpl(plugin);
    }

    public static rSellerAPI getAPI() {
        if (instance == null) {
            throw new IllegalStateException("rSeller API is not initialized yet.");
        }
        return instance;
    }
}