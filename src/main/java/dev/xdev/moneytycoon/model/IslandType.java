package dev.xdev.moneytycoon.model;

import dev.xdev.moneytycoon.MoneyTycoon;
import org.bukkit.configuration.ConfigurationSection;

public record IslandType(
        String id,
        String displayName,
        String description,
        int plotSize,
        double cost,
        int dropperX,
        int dropperZ,
        int convStartZ,
        int convEndZ,
        int sellZ,
        int spawnX,
        int spawnZ,
        boolean vip
) {
    public static IslandType fromConfig(MoneyTycoon plugin, String id) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("island-types." + id);
        if (sec == null) sec = plugin.getConfig().getConfigurationSection("island-types.DEFAULT");
        if (sec == null) return defaultType(id);
        return new IslandType(
                id,
                sec.getString("display-name", "&aIsland").replace("&", "§"),
                sec.getString("description", "").replace("&", "§"),
                sec.getInt("plot-size", 35),
                sec.getDouble("cost", 0),
                sec.getInt("dropper-x", 17),
                sec.getInt("dropper-z", 5),
                sec.getInt("conv-start-z", 8),
                sec.getInt("conv-end-z", 22),
                sec.getInt("sell-z", 23),
                sec.getInt("spawn-x", 17),
                sec.getInt("spawn-z", 30),
                sec.getBoolean("vip", false)
        );
    }

    private static IslandType defaultType(String id) {
        return new IslandType(id, "§aIsland", "", 35, 0, 17, 5, 8, 22, 23, 17, 30, false);
    }
}
