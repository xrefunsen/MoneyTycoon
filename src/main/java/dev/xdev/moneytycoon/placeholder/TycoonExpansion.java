package dev.xdev.moneytycoon.placeholder;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.Tycoon;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class TycoonExpansion extends PlaceholderExpansion {

    private final MoneyTycoon plugin;

    public TycoonExpansion(MoneyTycoon plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "moneytycoon"; }
    @Override public @NotNull String getAuthor() { return "xDev"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        Tycoon t = plugin.getTycoonManager().getTycoon(player.getUniqueId());
        if (t == null) return "None";

        return switch (params.toLowerCase()) {
            case "balance" -> String.format("%.2f", t.getBalance());
            case "total_earned" -> String.valueOf(t.getTotalEarned());
            case "items_sold" -> String.valueOf(t.getItemsSold());
            case "prestige" -> String.valueOf(t.getPrestigeLevel());
            case "rebirth" -> String.valueOf(t.getRebirthLevel());
            case "dropper" -> t.getCurrentDropperType().name();
            case "theme" -> t.getTheme().name();
            case "multiplier" -> String.format("%.1f", t.getValueMultiplier());
            case "prestige_multiplier" -> String.format("%.1f", t.getPrestigeMultiplier());
            case "rebirth_multiplier" -> String.format("%.1f", t.getRebirthMultiplier());
            case "pet" -> t.getActivePetId() != null ? t.getActivePetId() : "None";
            case "coop_count" -> String.valueOf(t.getCoopMembers().size());
            case "has_tycoon" -> "Yes";
            case "booster" -> t.getBoosterEndTime() > System.currentTimeMillis()
                    ? String.format("%.1fx", t.getBoosterMultiplier()) : "None";
            case "booster_time" -> t.getBoosterEndTime() > System.currentTimeMillis()
                    ? String.valueOf((t.getBoosterEndTime() - System.currentTimeMillis()) / 1000) : "0";
            case "milestone" -> String.valueOf(t.getLastMilestone());
            default -> null;
        };
    }
}
