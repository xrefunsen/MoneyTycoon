package dev.xdev.moneytycoon;

import dev.xdev.moneytycoon.command.TycoonCommand;
import dev.xdev.moneytycoon.database.DatabaseManager;
import dev.xdev.moneytycoon.gui.GUIManager;
import dev.xdev.moneytycoon.listener.TycoonListener;
import dev.xdev.moneytycoon.manager.*;
import dev.xdev.moneytycoon.task.TycoonTask;
import dev.xdev.moneytycoon.world.VoidGenerator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public final class MoneyTycoon extends JavaPlugin {

    private static MoneyTycoon instance;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private WorldManager worldManager;
    private TycoonManager tycoonManager;
    private QuestManager questManager;
    private LeaderboardManager leaderboardManager;
    private PetManager petManager;
    private TradeManager tradeManager;
    private HologramManager hologramManager;
    private LogManager logManager;
    private SettingsManager settingsManager;
    private GUIManager guiManager;
    private TycoonTask tycoonTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        economyManager = new EconomyManager(this);
        if (!economyManager.setup()) {
            getLogger().severe("Vault not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.initialize();
        } catch (Exception e) {
            getLogger().severe("Database initialization failed: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        worldManager = new WorldManager(this);
        worldManager.initialize();

        tycoonManager = new TycoonManager(this);
        questManager = new QuestManager(this);
        leaderboardManager = new LeaderboardManager(this);
        petManager = new PetManager(this);
        tradeManager = new TradeManager(this);
        hologramManager = new HologramManager(this);
        logManager = new LogManager(this);
        settingsManager = new SettingsManager(this);
        guiManager = new GUIManager(this);

        tycoonManager.loadAll();

        TycoonCommand cmd = new TycoonCommand(this);
        var tycoonCmd = getCommand("tycoon");
        if (tycoonCmd != null) {
            tycoonCmd.setExecutor(cmd);
            tycoonCmd.setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(new TycoonListener(this), this);

        tycoonTask = new TycoonTask(this);
        tycoonTask.start();

        long autoSave = getConfig().getInt("general.auto-save-interval", 5) * 60L * 20L;
        getServer().getScheduler().runTaskTimer(this, () -> {
            databaseManager.saveAllTycoons(tycoonManager.getAllTycoons());
            questManager.saveAll();
            leaderboardManager.refresh();
        }, autoSave, autoSave);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new dev.xdev.moneytycoon.placeholder.TycoonExpansion(this).register();
                getLogger().info("PlaceholderAPI integration enabled!");
            } catch (Exception e) {
                getLogger().warning("PlaceholderAPI integration failed: " + e.getMessage());
            }
        }

        leaderboardManager.refresh();
        getLogger().info("MoneyTycoon v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (tycoonTask != null) tycoonTask.stop();
        if (hologramManager != null) hologramManager.removeAll();
        if (tycoonManager != null) tycoonManager.cleanup();
        if (databaseManager != null) {
            if (tycoonManager != null) databaseManager.saveAllTycoons(tycoonManager.getAllTycoons());
            if (questManager != null) questManager.saveAll();
            databaseManager.close();
        }
        getLogger().info("MoneyTycoon disabled.");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new VoidGenerator();
    }

    public static MoneyTycoon getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public TycoonManager getTycoonManager() { return tycoonManager; }
    public QuestManager getQuestManager() { return questManager; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public PetManager getPetManager() { return petManager; }
    public TradeManager getTradeManager() { return tradeManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public LogManager getLogManager() { return logManager; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public TycoonTask getTycoonTask() { return tycoonTask; }
}
