package dev.xdev.moneytycoon.manager;

import dev.xdev.moneytycoon.MoneyTycoon;

import java.util.UUID;

public class SettingsManager {

    private final MoneyTycoon plugin;

    public SettingsManager(MoneyTycoon plugin) {
        this.plugin = plugin;
    }

    public boolean isSoundsEnabled(UUID uuid) {
        return plugin.getDatabaseManager().getSetting(uuid, "sounds", 1) != 0;
    }

    public boolean isTradeRequestsEnabled(UUID uuid) {
        return plugin.getDatabaseManager().getSetting(uuid, "trade_requests", 1) != 0;
    }

    public boolean isVisitsAllowed(UUID uuid) {
        return plugin.getDatabaseManager().getSetting(uuid, "visits_allowed", 1) != 0;
    }

    public boolean isActionbarEnabled(UUID uuid) {
        return plugin.getDatabaseManager().getSetting(uuid, "actionbar", 1) != 0;
    }

    public boolean isBossbarEnabled(UUID uuid) {
        return plugin.getDatabaseManager().getSetting(uuid, "bossbar", 1) != 0;
    }

    public boolean isNotificationsEnabled(UUID uuid) {
        return plugin.getDatabaseManager().getSetting(uuid, "notifications", 1) != 0;
    }

    public void setSounds(UUID uuid, boolean on) {
        plugin.getDatabaseManager().setSetting(uuid, "sounds", on ? 1 : 0);
    }

    public void setTradeRequests(UUID uuid, boolean on) {
        plugin.getDatabaseManager().setSetting(uuid, "trade_requests", on ? 1 : 0);
    }

    public void setVisitsAllowed(UUID uuid, boolean on) {
        plugin.getDatabaseManager().setSetting(uuid, "visits_allowed", on ? 1 : 0);
    }

    public void setActionbar(UUID uuid, boolean on) {
        plugin.getDatabaseManager().setSetting(uuid, "actionbar", on ? 1 : 0);
    }

    public void setBossbar(UUID uuid, boolean on) {
        plugin.getDatabaseManager().setSetting(uuid, "bossbar", on ? 1 : 0);
    }

    public void setNotifications(UUID uuid, boolean on) {
        plugin.getDatabaseManager().setSetting(uuid, "notifications", on ? 1 : 0);
    }
}
