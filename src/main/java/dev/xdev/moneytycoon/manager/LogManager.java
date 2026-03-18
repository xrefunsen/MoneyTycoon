package dev.xdev.moneytycoon.manager;

import dev.xdev.moneytycoon.MoneyTycoon;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class LogManager {

    private final MoneyTycoon plugin;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogManager(MoneyTycoon plugin) {
        this.plugin = plugin;
    }

    private boolean isEnabled(String type) {
        return plugin.getConfig().getBoolean("logging.enabled", true)
                && plugin.getConfig().getBoolean("logging.log-" + type, true);
    }

    private void log(String line) {
        try {
            File logDir = new File(plugin.getDataFolder(), "logs");
            if (!logDir.exists()) logDir.mkdirs();
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            File file = new File(logDir, "moneytycoon-" + date + ".log");
            try (FileWriter w = new FileWriter(file, true)) {
                w.write("[" + LocalDateTime.now().format(formatter) + "] " + line + "\n");
            }
        } catch (IOException ignored) {}
    }

    public void logPrestige(UUID player, String name, int newLevel) {
        if (!isEnabled("prestige")) return;
        log("PRESTIGE | " + name + " (" + player + ") -> Level " + newLevel);
    }

    public void logRebirth(UUID player, String name, int newLevel) {
        if (!isEnabled("rebirth")) return;
        log("REBIRTH | " + name + " (" + player + ") -> Level " + newLevel);
    }

    public void logTrade(UUID p1, String n1, UUID p2, String n2, double m1, double m2) {
        if (!isEnabled("trades")) return;
        log("TRADE | " + n1 + " <-> " + n2 + " | $" + m1 + " / $" + m2);
    }

    public void logAdmin(String admin, String action, String target, String details) {
        if (!isEnabled("admin-actions")) return;
        log("ADMIN | " + admin + " | " + action + " | " + target + " | " + details);
    }
}
