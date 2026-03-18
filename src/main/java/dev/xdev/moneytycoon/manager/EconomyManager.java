package dev.xdev.moneytycoon.manager;

import dev.xdev.moneytycoon.MoneyTycoon;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public class EconomyManager {

    private final MoneyTycoon plugin;
    private Economy economy;

    public EconomyManager(MoneyTycoon plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public double getBalance(UUID playerUUID) {
        return economy.getBalance(Bukkit.getOfflinePlayer(playerUUID));
    }

    public boolean deposit(UUID playerUUID, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean withdraw(UUID playerUUID, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean has(UUID playerUUID, double amount) {
        return economy.has(Bukkit.getOfflinePlayer(playerUUID), amount);
    }

    public String format(double amount) {
        return economy.format(amount);
    }

    public Economy getEconomy() {
        return economy;
    }
}
