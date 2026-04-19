package my.reqqpe.rseller.configs.impl;

import lombok.Getter;
import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.configs.CustomConfig;

import java.util.List;

@Getter
public class MainConfig extends CustomConfig {

    private EconomySection economy;
    private AutoSellSection autosell;
    private NumbersFormatSection numbersFormat;

    public MainConfig(Main plugin) {
        super(plugin, "config.yml");
    }

    @Override
    protected void load() {

        economy = new EconomySection();
        autosell = new AutoSellSection();
        numbersFormat = new NumbersFormatSection();
    }

    @Getter
    public class EconomySection {
        private final String type = getString("economy.type", "VAULT");
        private final String coinsengineCurrency = getString("economy.coinsengine.currency-name", "money");
    }

    @Getter
    public class AutoSellSection {

        private final boolean enabled = getBoolean("autosell.enable", true);
        private final String type = getString("autosell.settings.type", "pickup");
        private final boolean sellInventory = getBoolean("autosell.settings.sell-inventory", false);
        private final int taskDelay = getInt("autosell.settings.task-delay", 5);

        private final boolean whitelist = getBoolean("autosell.type-list", false);
        private final List<String> worlds = getStringList("autosell.worlds-list");

        private final String startCategory = getString("autosell.start_category", "example");
    }

    @Getter
    public class NumbersFormatSection {

        private final String points = getString("numbers_format.placeholders.points", "%.2f");
        private final String pointsNeeded = getString("numbers_format.placeholders.points_needed", "%.2f");

        private final String guiSellPrice = getString("numbers_format.mainGUI.sell_price", "%.2f");
        private final String guiSellPoints = getString("numbers_format.mainGUI.sell_points", "%.2f");
    }
}