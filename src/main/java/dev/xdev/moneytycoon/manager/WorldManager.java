package dev.xdev.moneytycoon.manager;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.world.VoidGenerator;
import org.bukkit.*;

public class WorldManager {

    private final MoneyTycoon plugin;
    private World tycoonWorld;

    public WorldManager(MoneyTycoon plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        String worldName = plugin.getConfig().getString("general.world", "moneytycoon_world");
        tycoonWorld = Bukkit.getWorld(worldName);

        if (tycoonWorld == null) {
            plugin.getLogger().info("Creating tycoon world: " + worldName);
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new VoidGenerator());
            creator.environment(World.Environment.NORMAL);
            creator.generateStructures(false);
            tycoonWorld = Bukkit.createWorld(creator);
        }

        if (tycoonWorld != null) {
            tycoonWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            tycoonWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            tycoonWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            tycoonWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            tycoonWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
            tycoonWorld.setGameRule(GameRule.MOB_GRIEFING, false);
            tycoonWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
            tycoonWorld.setGameRule(GameRule.DO_TILE_DROPS, false);
            tycoonWorld.setTime(6000);
            tycoonWorld.setStorm(false);
            tycoonWorld.setThundering(false);
            tycoonWorld.setDifficulty(Difficulty.PEACEFUL);
            plugin.getLogger().info("Tycoon world ready: " + worldName);
        } else {
            plugin.getLogger().severe("Failed to create tycoon world!");
        }
    }

    public World getTycoonWorld() {
        return tycoonWorld;
    }
}
