package dev.xdev.moneytycoon.manager;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.Tycoon;

import java.util.Collections;
import java.util.List;

public class LeaderboardManager {

    private final MoneyTycoon plugin;
    private List<String[]> cachedTop = Collections.emptyList();

    public LeaderboardManager(MoneyTycoon plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        for (Tycoon t : plugin.getTycoonManager().getAllTycoons()) {
            plugin.getDatabaseManager().updateLeaderboard(
                    t.getOwnerUUID(), t.getOwnerName(),
                    t.getTotalEarned(), t.getPrestigeLevel(), t.getRebirthLevel());
        }
        cachedTop = plugin.getDatabaseManager().getTopPlayers(10);
    }

    public List<String[]> getTop() {
        return cachedTop;
    }
}
