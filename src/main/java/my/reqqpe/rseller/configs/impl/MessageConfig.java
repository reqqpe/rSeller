package my.reqqpe.rseller.configs.impl;

import my.reqqpe.rseller.Main;
import my.reqqpe.rseller.configs.CustomConfig;

import lombok.Getter;
import my.reqqpe.rseller.Main;

@Getter
public class MessageConfig extends CustomConfig {

    private String reloadAll;
    private String reloadGuis;
    private String reloadItems;
    private String reloadConfig;

    private String noPermission;
    private String noArguments;
    private String pointsUsage;
    private String unInt;
    private String notFoundPlayer;

    private String updatePointsSender;
    private String updatePointsTarget;

    private String noSellItems;
    private String sellItems;
    private String autoSell;

    private String autosellEnable;
    private String autosellDisable;

    private String negativeValue;
    private String negativeSet;
    private String notEnoughPoints;

    private String alreadyExistsItem;
    private String onlyPlayer;
    private String noItem;
    private String createSuccess;

    public MessageConfig(Main plugin) {
        super(plugin, "messages.yml");
    }

    @Override
    protected void load() {

        reloadAll = getString("reload.all", "");
        reloadGuis = getString("reload.guis", "");
        reloadItems = getString("reload.items", "");
        reloadConfig = getString("reload.config", "");

        noPermission = getString("no-permission", "");
        noArguments = getString("no-arguments", "");
        pointsUsage = getString("points-usage", "");
        unInt = getString("un-int", "");
        notFoundPlayer = getString("not-found-player", "");

        updatePointsSender = getString("update-points-sender", "");
        updatePointsTarget = getString("update-points-target", "");

        noSellItems = getString("no-sell-items", "");
        sellItems = getString("sell-items", "");
        autoSell = getString("auto-sell", "");

        autosellEnable = getString("autosell-enable", "");
        autosellDisable = getString("autosell-disable", "");

        negativeValue = getString("negative-value", "");
        negativeSet = getString("negative-set", "");
        notEnoughPoints = getString("not-enough-points", "");

        alreadyExistsItem = getString("already-exists-item", "");
        onlyPlayer = getString("only-player", "");
        noItem = getString("no-item", "");
        createSuccess = getString("create-success", "");
    }
}
