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
    private String autoSellTask;

    private String autosellEnable;
    private String autosellDisable;
    private String autosellMessageEnable;
    private String autosellMessageDisable;

    private String autosellNotFoundCategory;
    private String autosellNotFoundItem;
    private String autosellCategoryToggle;
    private String autosellItemToggle;
    private String autosellUsageAdmin;
    private String autosellUsageDef;

    private String negativeValue;
    private String negativeSet;
    private String notEnoughPoints;

    private String alreadyExistsItem;
    private String onlyPlayer;
    private String onlyConsole;
    private String noItem;
    private String createSuccess;

    // Console messages
    private String consoleDatabaseInitialized;
    private String consoleBstatsInitialized;
    private String consolePlaceholderApiNotFound;
    private String consoleEconomyNotFound;
    private String consoleEconomyUnknown;
    private String consoleItemSaved;
    private String consoleItemDeleted;
    private String consoleInvalidLevelId;
    private String consoleAutosellSectionMissing;
    private String consoleAutosellInvalidItem;
    private String consoleUpdateAvailable;
    private String consoleUpdateNotRequired;
    private String consoleUpdateHttpError;
    private String consoleUpdateError;
    private String consoleUpdateVersionFormat;
    private String consoleMenuNotFound;
    private String consoleSoundUnknown;
    private String consoleSoundError;
    private String consoleMenuSlotOutOfBounds;
    private String consoleMenuSlotOccupied;
    private String consoleMenuNoSlots;
    private String consoleMenuUnknownMaterial;
    private String consoleMenuUnknownEnchant;
    private String consoleMenuInvalidEnchantLevel;
    private String consoleMenuEnchantError;

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
        autoSellTask = getString("auto-sell-task", "");

        autosellEnable = getString("autosell-enable", "");
        autosellDisable = getString("autosell-disable", "");
        autosellMessageEnable = getString("autosell-message-enable", "");
        autosellMessageDisable = getString("autosell-message-disable", "");

        autosellNotFoundCategory = getString("autosell-not-found-category", "");
        autosellNotFoundItem = getString("autosell-not-found-item", "");
        autosellCategoryToggle = getString("autosell-toggle-category", "");
        autosellItemToggle = getString("autosell-toggle-item", "");
        autosellUsageAdmin = getString("autosell-usage-admin", "");
        autosellUsageDef = getString("autosell-usage-def", "");

        negativeValue = getString("negative-value", "");
        negativeSet = getString("negative-set", "");
        notEnoughPoints = getString("not-enough-points", "");

        alreadyExistsItem = getString("already-exists-item", "");
        onlyPlayer = getString("only-player", "");
        onlyConsole = getString("only-console", "");
        noItem = getString("no-item", "");
        createSuccess = getString("create-success", "");

        consoleDatabaseInitialized = getString("console.database-initialized", "Database initialized: {type}");
        consoleBstatsInitialized = getString("console.bstats-initialized", "bStats успешно инициализирован!");
        consolePlaceholderApiNotFound = getString("console.placeholderapi-not-found", "PlaceholderAPI не найден, плагин отключён");
        consoleEconomyNotFound = getString("console.economy-not-found", "Экономика {type} не найдена, плагин отключён");
        consoleEconomyUnknown = getString("console.economy-unknown", "Неизвестный тип экономики: {type}, плагин отключён");
        consoleItemSaved = getString("console.item-saved", "[ItemStorage] Item {id} saved");
        consoleItemDeleted = getString("console.item-deleted", "[ItemStorage] Item {id} deleted");
        consoleInvalidLevelId = getString("console.invalid-level-id", "Invalid level id: {id}");
        consoleAutosellSectionMissing = getString("console.autosell-section-missing", "Section 'autosell' is missing in config.yml!");
        consoleAutosellInvalidItem = getString("console.autosell-invalid-item", "Invalid item id in autosell list: {id}");
        consoleUpdateAvailable = getString("console.update-available", "Update available: current {current}, latest {latest}");
        consoleUpdateNotRequired = getString("console.update-not-required", "No update required: current {current}, latest {latest}");
        consoleUpdateHttpError = getString("console.update-http-error", "HTTP error while checking for updates: {code}");
        consoleUpdateError = getString("console.update-error", "Error while checking for updates: {error}");
        consoleUpdateVersionFormat = getString("console.update-version-format", "Invalid version format: current={current}, latest={latest}");
        consoleMenuNotFound = getString("console.menu-not-found", "Menu not found, id: {id}");
        consoleSoundUnknown = getString("console.sound-unknown", "Unknown sound: {sound}");
        consoleSoundError = getString("console.sound-error", "Error playing sound.");
        consoleMenuSlotOutOfBounds = getString("console.menu-slot-out-of-bounds", "[{menu}] Skipped slot {slot} for item {item}: out of bounds (0-{max}).");
        consoleMenuSlotOccupied = getString("console.menu-slot-occupied", "[{menu}] Skipped slot {slot} for item {item}: slot already occupied.");
        consoleMenuNoSlots = getString("console.menu-no-slots", "[{menu}] No available slots for item {item}.");
        consoleMenuUnknownMaterial = getString("console.menu-unknown-material", "[{menu}] Unknown material for item {item}: {material}");
        consoleMenuUnknownEnchant = getString("console.menu-unknown-enchant", "[{menu}] Unknown enchantment for item {item}: {enchant}");
        consoleMenuInvalidEnchantLevel = getString("console.menu-invalid-enchant-level", "[{menu}] Invalid enchantment level for item {item}: {level} (must be > 0)");
        consoleMenuEnchantError = getString("console.menu-enchant-error", "[{menu}] Error adding enchantment {enchant}:{level} for item {item}: {error}");
    }
}
