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
    private SoundsSection sounds;
    private boolean metrics;
    private boolean updateCheck;
    private String sellCommand;

    public MainConfig(Main plugin) {
        super(plugin, "config.yml");
    }

    @Override
    protected void load() {
        economy = new EconomySection();
        autosell = new AutoSellSection();
        numbersFormat = new NumbersFormatSection();
        sounds = new SoundsSection();
        metrics = getBoolean("metrics", true);
        updateCheck = getBoolean("update-check", true);
        sellCommand = getString("sell-command", "mainGUI");
    }

    @Getter
    public class EconomySection {
        private final String type = getString("economy.type", "VAULT");
        private final String coinsengineCurrency = getString("economy.coinsengine.currency-name", "money");
    }

    @Getter
    public class AutoSellSection {
        private final boolean enabled = getBoolean("autosell.enable", true);
        private final String typeMessage = getString("autosell.message-settings.type", "sell");
        private final String sendType = getString("autosell.message-settings.send-type", "message");
        private final int delayMessage = getInt("autosell.message-settings.task-delay", 5);
        private final String typeSell = getString("autosell.settings.type", "pickup");
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

    @Getter
    public class SoundsSection {
        private final String sell = getString("sounds.sell", "ENTITY_EXPERIENCE_ORB_PICKUP");
        private final float sellVolume = (float) getDouble("sounds.sell-volume", 1.0);
        private final float sellPitch = (float) getDouble("sounds.sell-pitch", 1.0);

        private final String autosell = getString("sounds.autosell", "");
        private final float autosellVolume = (float) getDouble("sounds.autosell-volume", 1.0);
        private final float autosellPitch = (float) getDouble("sounds.autosell-pitch", 1.0);

        private final String noSell = getString("sounds.no-sell", "");
        private final float noSellVolume = (float) getDouble("sounds.no-sell-volume", 1.0);
        private final float noSellPitch = (float) getDouble("sounds.no-sell-pitch", 1.0);
    }
}