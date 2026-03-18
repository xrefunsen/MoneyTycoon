package dev.xdev.moneytycoon.command;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.Tycoon;
import dev.xdev.moneytycoon.model.TycoonTheme;
import dev.xdev.moneytycoon.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TycoonCommand implements CommandExecutor, TabCompleter {

    private final MoneyTycoon plugin;
    public TycoonCommand(MoneyTycoon plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { MessageUtil.sendRaw(sender, "§cThis command is for players only!"); return true; }
        if (args.length == 0) {
            Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
            if (t != null) plugin.getGuiManager().openMainMenu(p); else sendHelp(p);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(p);
            case "delete" -> handleDelete(p);
            case "menu" -> ifHas(p, () -> plugin.getGuiManager().openMainMenu(p));
            case "upgrades" -> ifHas(p, () -> plugin.getGuiManager().openUpgradeMenu(p));
            case "collect" -> handleCollect(p);
            case "visit" -> handleVisit(p, args);
            case "tp" -> handleTp(p);
            case "stats" -> handleStats(p);
            case "prestige" -> handlePrestige(p);
            case "rebirth" -> handleRebirth(p);
            case "quest" -> ifHas(p, () -> plugin.getGuiManager().openQuestMenu(p));
            case "pet" -> ifHas(p, () -> plugin.getGuiManager().openPetMenu(p));
            case "theme" -> ifHas(p, () -> plugin.getGuiManager().openThemeMenu(p));
            case "settings" -> ifHas(p, () -> plugin.getGuiManager().openSettingsMenu(p));
            case "coop" -> handleCoop(p, args);
            case "booster" -> ifHas(p, () -> plugin.getGuiManager().openBoosterMenu(p));
            case "milestones" -> ifHas(p, () -> plugin.getGuiManager().openMilestoneMenu(p));
            case "profile" -> ifHas(p, () -> plugin.getGuiManager().openProfileMenu(p));
            case "shop" -> ifHas(p, () -> plugin.getGuiManager().openShopMenu(p));
            case "trade" -> handleTrade(p, args);
            case "leaderboard", "lb", "top" -> plugin.getGuiManager().openLeaderboard(p);
            case "admin" -> handleAdmin(p, args);
            case "help" -> plugin.getGuiManager().openHelpMenu(p);
            default -> sendHelp(p);
        }
        return true;
    }

    private void ifHas(Player p, Runnable r) {
        if (plugin.getTycoonManager().getTycoon(p.getUniqueId()) == null) { MessageUtil.send(p, "no-tycoon", Map.of()); return; }
        r.run();
    }

    private void handleCreate(Player p) {
        if (!p.hasPermission("moneytycoon.create")) { MessageUtil.send(p, "no-permission", Map.of()); return; }
        if (plugin.getTycoonManager().getTycoon(p.getUniqueId()) != null) { MessageUtil.send(p, "tycoon-already-exists", Map.of()); return; }
        plugin.getGuiManager().openIslandTypeMenu(p);
    }

    private void handleDelete(Player p) {
        if (!p.hasPermission("moneytycoon.delete")) { MessageUtil.send(p, "no-permission", Map.of()); return; }
        if (plugin.getTycoonManager().getTycoon(p.getUniqueId()) == null) { MessageUtil.send(p, "no-tycoon", Map.of()); return; }
        plugin.getTycoonManager().deleteTycoon(p.getUniqueId());
        MessageUtil.send(p, "tycoon-deleted", Map.of());
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private void handleCollect(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) { MessageUtil.send(p, "no-tycoon", Map.of()); return; }
        if (t.getBalance() <= 0) { MessageUtil.sendRaw(p, "§6[MoneyTycoon] §cNo money to collect!"); return; }
        double a = t.getBalance();
        plugin.getTycoonManager().collectBalance(p, t);
        MessageUtil.sendRaw(p, "§6[MoneyTycoon] §a$" + String.format("%.2f", a) + " §acollected to your wallet!");
    }

    private void handleVisit(Player p, String[] args) {
        if (!p.hasPermission("moneytycoon.visit") && !p.hasPermission("moneytycoon.admin")) { MessageUtil.send(p, "no-permission", Map.of()); return; }
        if (args.length < 2) { MessageUtil.sendRaw(p, "§6[MoneyTycoon] §cUsage: /tycoon visit <player>"); return; }
        Tycoon t = plugin.getTycoonManager().getTycoon(resolvePlayer(args[1]));
        if (t == null) t = plugin.getTycoonManager().getTycoonByOwnerName(args[1]);
        if (t == null) { MessageUtil.sendRaw(p, "§6[MoneyTycoon] §cThat player doesn't have a tycoon!"); return; }
        if (!p.hasPermission("moneytycoon.admin") && !plugin.getSettingsManager().isVisitsAllowed(t.getOwnerUUID())) {
            MessageUtil.sendRaw(p, "§6[MoneyTycoon] §cThat player has disabled visits to their tycoon!");
            return;
        }
        var it = plugin.getTycoonManager().getIslandType(t.getIslandType());
        p.teleport(t.getBaseLocation().clone().add(it.spawnX() + .5, 1, it.spawnZ() + .5));
        MessageUtil.send(p, "visit-teleported", Map.of("player", t.getOwnerName()));
    }

    private void handleTp(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) { MessageUtil.send(p, "no-tycoon", Map.of()); return; }
        var it = plugin.getTycoonManager().getIslandType(t.getIslandType());
        p.teleport(t.getBaseLocation().clone().add(it.spawnX() + .5, 1, it.spawnZ() + .5));
        MessageUtil.sendRaw(p, "§6[MoneyTycoon] §aTeleported to your tycoon!");
    }

    private void handleStats(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) { MessageUtil.send(p, "no-tycoon", Map.of()); return; }
        MessageUtil.sendRaw(p, "§6§l⚡ Tycoon Statistics ⚡");
        MessageUtil.sendRaw(p, "§8§m─────────────────────────");
        MessageUtil.sendRaw(p, "§6 Balance: §e$" + String.format("%.2f", t.getBalance()));
        MessageUtil.sendRaw(p, "§a Total Earned: §e$" + String.format("%.2f", (double) t.getTotalEarned()));
        MessageUtil.sendRaw(p, "§7 Items Sold: §f" + t.getItemsSold());
        MessageUtil.sendRaw(p, "§b Dropper: §f" + t.getCurrentDropperType().name());
        MessageUtil.sendRaw(p, "§e Prestige: §6" + t.getPrestigeLevel() + " §7(" + String.format("%.1f", t.getPrestigeMultiplier()) + "x)");
        MessageUtil.sendRaw(p, "§d Rebirth: §5" + t.getRebirthLevel() + " §7(" + String.format("%.1f", t.getRebirthMultiplier()) + "x)");
        MessageUtil.sendRaw(p, "§a Theme: §f" + t.getTheme().name());
        MessageUtil.sendRaw(p, "§6 Island Type: §f" + t.getIslandType());
        MessageUtil.sendRaw(p, "§d Pet: §f" + (t.getActivePetId() != null ? t.getActivePetId() : "None"));
        MessageUtil.sendRaw(p, "§a Booster: §f" + (t.hasActiveBooster() ? t.getBoosterMultiplier() + "x (" + t.getBoosterSecondsLeft() + "s)" : "None"));
        MessageUtil.sendRaw(p, "§a Co-op: §f" + t.getCoopMembers().size() + " members");
        MessageUtil.sendRaw(p, "§8§m─────────────────────────");
    }

    private void handlePrestige(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) { MessageUtil.send(p, "no-tycoon", Map.of()); return; }
        if (plugin.getTycoonManager().performPrestige(p, t))
            MessageUtil.send(p, "prestige-complete", Map.of("level", "" + t.getPrestigeLevel(), "multiplier", String.format("%.1f", t.getPrestigeMultiplier())));
        else MessageUtil.send(p, "not-enough-money", Map.of("cost", "?"));
    }

    private void handleRebirth(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) { MessageUtil.send(p, "no-tycoon", Map.of()); return; }
        if (plugin.getTycoonManager().performRebirth(p, t))
            MessageUtil.send(p, "rebirth-complete", Map.of("level", "" + t.getRebirthLevel(), "multiplier", String.format("%.1f", t.getRebirthMultiplier())));
        else MessageUtil.sendRaw(p, "§6[MoneyTycoon] §cRebirth requirements not met! Need Prestige " + plugin.getConfig().getInt("rebirth.prestige-requirement", 10));
    }

    private void handleCoop(Player p, String[] args) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) { MessageUtil.send(p, "no-tycoon", Map.of()); return; }
        if (args.length < 2) { plugin.getGuiManager().openCoopMenu(p); return; }
        switch (args[1].toLowerCase()) {
            case "invite" -> {
                if (args.length < 3) { MessageUtil.sendRaw(p, "§cUsage: /tycoon coop invite <player>"); return; }
                int max = p.hasPermission("moneytycoon.vip") ? plugin.getConfig().getInt("coop.vip-max-members", 5) : plugin.getConfig().getInt("coop.max-members", 2);
                if (t.getCoopMembers().size() >= max) { MessageUtil.send(p, "coop-full", Map.of()); return; }
                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) { MessageUtil.sendRaw(p, "§cPlayer not found!"); return; }
                if (target.getUniqueId().equals(p.getUniqueId())) { MessageUtil.sendRaw(p, "§cYou can't invite yourself!"); return; }
                t.addCoopMember(target.getUniqueId());
                plugin.getDatabaseManager().saveCoopMember(p.getUniqueId(), target.getUniqueId(), target.getName());
                MessageUtil.send(p, "coop-invited", Map.of("player", target.getName()));
                MessageUtil.sendRaw(target, "§6[MoneyTycoon] §a" + p.getName() + " §ainvited you to their tycoon!");
            }
            case "kick" -> {
                if (args.length < 3) { MessageUtil.sendRaw(p, "§cUsage: /tycoon coop kick <player>"); return; }
                Player target = Bukkit.getPlayer(args[2]);
                UUID tid = target != null ? target.getUniqueId() : Bukkit.getOfflinePlayer(args[2]).getUniqueId();
                if (!t.getCoopMembers().contains(tid)) { MessageUtil.sendRaw(p, "§cThat player is not a co-op member!"); return; }
                t.removeCoopMember(tid);
                plugin.getDatabaseManager().removeCoopMember(p.getUniqueId(), tid);
                MessageUtil.send(p, "coop-kicked", Map.of("player", args[2]));
            }
            case "list" -> {
                MessageUtil.sendRaw(p, "§6§lCo-op Members:");
                for (UUID mid : t.getCoopMembers()) {
                    String name = Bukkit.getOfflinePlayer(mid).getName();
                    MessageUtil.sendRaw(p, "§7- §f" + (name != null ? name : mid.toString().substring(0, 8)));
                }
            }
            default -> plugin.getGuiManager().openCoopMenu(p);
        }
    }

    private void handleAdmin(Player p, String[] args) {
        if (!p.hasPermission("moneytycoon.admin")) { MessageUtil.send(p, "no-permission", Map.of()); return; }
        if (args.length < 2) { sendAdminHelp(p); return; }

        switch (args[1].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                MessageUtil.send(p, "reload-complete", Map.of());
            }
            case "delete" -> {
                if (args.length < 3) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin delete <player>"); return; }
                UUID uid = resolvePlayer(args[2]);
                if (plugin.getTycoonManager().getTycoon(uid) == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                plugin.getTycoonManager().deleteTycoon(uid);
                MessageUtil.sendRaw(p, "§a✓ Tycoon of §e" + args[2] + " §adeleted.");
            }
            case "info" -> {
                if (args.length < 3) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin info <player>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                MessageUtil.sendRaw(p, "§6§l⚡ Admin - Tycoon Info ⚡");
                MessageUtil.sendRaw(p, "§8§m──────────────────────────────");
                MessageUtil.sendRaw(p, "§7 Owner: §f" + t.getOwnerName() + " §7(" + t.getOwnerUUID().toString().substring(0, 8) + "...)");
                MessageUtil.sendRaw(p, "§7 Grid Index: §f" + t.getGridIndex());
                MessageUtil.sendRaw(p, "§7 Base Location: §f" + locStr(t.getBaseLocation()));
                MessageUtil.sendRaw(p, "§7 Balance: §e$" + String.format("%.2f", t.getBalance()));
                MessageUtil.sendRaw(p, "§7 Total Earned: §a$" + String.format("%.2f", (double) t.getTotalEarned()));
                MessageUtil.sendRaw(p, "§7 Items Sold: §b" + t.getItemsSold());
                MessageUtil.sendRaw(p, "§7 Dropper: §f" + t.getCurrentDropperType().name());
                MessageUtil.sendRaw(p, "§7 Prestige: §6" + t.getPrestigeLevel() + " §7| Rebirth: §d" + t.getRebirthLevel());
                MessageUtil.sendRaw(p, "§7 Theme: §f" + t.getTheme().name());
                MessageUtil.sendRaw(p, "§7 Pet: §f" + (t.getActivePetId() != null ? t.getActivePetId() : "None"));
                MessageUtil.sendRaw(p, "§7 Owned Pets: §f" + t.getOwnedPets());
                MessageUtil.sendRaw(p, "§7 Booster: §f" + (t.hasActiveBooster() ? t.getBoosterMultiplier() + "x (" + t.getBoosterSecondsLeft() + "s)" : "None"));
                MessageUtil.sendRaw(p, "§7 Co-op Members: §f" + t.getCoopMembers().size());
                MessageUtil.sendRaw(p, "§7 Upgrades: §f" + t.getUpgrades());
                MessageUtil.sendRaw(p, "§7 Value Multiplier: §a" + String.format("%.2f", t.getValueMultiplier()) + "x");
                MessageUtil.sendRaw(p, "§7 Tracked Items: §f" + t.getTrackedItems().size());
                MessageUtil.sendRaw(p, "§8§m──────────────────────────────");
            }
            case "list" -> {
                var all = plugin.getTycoonManager().getAllTycoons();
                MessageUtil.sendRaw(p, "§6§l⚡ All Tycoons §7(" + all.size() + ") ⚡");
                MessageUtil.sendRaw(p, "§8§m──────────────────────────────");
                int i = 1;
                for (Tycoon t : all) {
                    boolean online = Bukkit.getPlayer(t.getOwnerUUID()) != null;
                    MessageUtil.sendRaw(p, "§7 " + i + ". " + (online ? "§a" : "§c") + t.getOwnerName()
                            + " §7| P:§6" + t.getPrestigeLevel() + " §7R:§d" + t.getRebirthLevel()
                            + " §7| $§e" + String.format("%.0f", (double) t.getTotalEarned())
                            + " §7| Grid:§f" + t.getGridIndex());
                    i++;
                }
                MessageUtil.sendRaw(p, "§8§m──────────────────────────────");
            }
            case "tp" -> {
                if (args.length < 3) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin tp <player>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                var it = plugin.getTycoonManager().getIslandType(t.getIslandType());
                p.teleport(t.getBaseLocation().clone().add(it.spawnX() + .5, 1, it.spawnZ() + .5));
                MessageUtil.sendRaw(p, "§a✓ Teleported to §e" + args[2] + "§a's tycoon.");
            }
            case "give" -> {
                if (args.length < 4) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin give <player> <amount>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.sendRaw(p, "§cInvalid amount!"); return; }
                plugin.getEconomyManager().deposit(uid, amount);
                MessageUtil.sendRaw(p, "§a✓ Gave §e$" + String.format("%.2f", amount) + " §ato §e" + args[2] + "§a's wallet.");
                Player target = Bukkit.getPlayer(uid);
                if (target != null) MessageUtil.sendRaw(target, "§6[MoneyTycoon] §aYou received §e$" + String.format("%.2f", amount) + " §afrom an admin!");
            }
            case "setbalance" -> {
                if (args.length < 4) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin setbalance <player> <amount>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { MessageUtil.sendRaw(p, "§cInvalid amount!"); return; }
                t.setBalance(amount);
                plugin.getDatabaseManager().saveTycoon(t);
                MessageUtil.sendRaw(p, "§a✓ Set §e" + args[2] + "§a's tycoon balance to §e$" + String.format("%.2f", amount));
            }
            case "setprestige" -> {
                if (args.length < 4) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin setprestige <player> <level>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                int level;
                try { level = Integer.parseInt(args[3]); } catch (NumberFormatException e) { MessageUtil.sendRaw(p, "§cInvalid level!"); return; }
                t.setPrestigeLevel(Math.max(0, level));
                plugin.getDatabaseManager().saveTycoon(t);
                MessageUtil.sendRaw(p, "§a✓ Set §e" + args[2] + "§a's prestige to §6" + level);
            }
            case "setrebirth" -> {
                if (args.length < 4) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin setrebirth <player> <level>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                int level;
                try { level = Integer.parseInt(args[3]); } catch (NumberFormatException e) { MessageUtil.sendRaw(p, "§cInvalid level!"); return; }
                t.setRebirthLevel(Math.max(0, level));
                plugin.getDatabaseManager().saveTycoon(t);
                MessageUtil.sendRaw(p, "§a✓ Set §e" + args[2] + "§a's rebirth to §d" + level);
            }
            case "setdropper" -> {
                if (args.length < 4) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin setdropper <player> <type>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                Material mat;
                try { mat = Material.valueOf(args[3].toUpperCase()); } catch (IllegalArgumentException e) { MessageUtil.sendRaw(p, "§cInvalid material! Example: DIAMOND"); return; }
                t.setCurrentDropperType(mat);
                plugin.getDatabaseManager().saveTycoon(t);
                MessageUtil.sendRaw(p, "§a✓ Set §e" + args[2] + "§a's dropper to §b" + mat.name());
            }
            case "settheme" -> {
                if (args.length < 4) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin settheme <player> <theme>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                TycoonTheme theme;
                try { theme = TycoonTheme.valueOf(args[3].toUpperCase()); } catch (IllegalArgumentException e) { MessageUtil.sendRaw(p, "§cInvalid theme! Options: CLASSIC, MINE, FARM, NETHER, END, ROYAL"); return; }
                t.setTheme(theme);
                plugin.getTycoonManager().cleanupEntities(t);
                plugin.getPetManager().despawnPet(t);
                plugin.getTycoonManager().clearBase(t);
                plugin.getTycoonManager().buildTycoonBase(t);
                plugin.getTycoonManager().setupLocations(t);
                plugin.getDatabaseManager().saveTycoon(t);
                MessageUtil.sendRaw(p, "§a✓ Set §e" + args[2] + "§a's theme to §d" + theme.name());
            }
            case "givepet" -> {
                if (args.length < 4) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin givepet <player> <pet>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                String petId = args[3].toUpperCase();
                ConfigurationSection petSec = plugin.getConfig().getConfigurationSection("pets");
                if (petSec == null || !petSec.contains(petId)) { MessageUtil.sendRaw(p, "§cInvalid pet! Options: " + (petSec != null ? petSec.getKeys(false) : "none")); return; }
                t.getOwnedPets().add(petId);
                plugin.getDatabaseManager().saveOwnedPet(uid, petId);
                MessageUtil.sendRaw(p, "§a✓ Gave pet §d" + petId + " §ato §e" + args[2]);
                Player target = Bukkit.getPlayer(uid);
                if (target != null) MessageUtil.sendRaw(target, "§6[MoneyTycoon] §aYou received pet §d" + petId + " §afrom an admin!");
            }
            case "givebooster" -> {
                if (args.length < 5) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin givebooster <player> <multiplier> <seconds>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                double mult;
                int secs;
                try { mult = Double.parseDouble(args[3]); secs = Integer.parseInt(args[4]); } catch (NumberFormatException e) { MessageUtil.sendRaw(p, "§cInvalid numbers!"); return; }
                t.setBooster(mult, secs);
                plugin.getDatabaseManager().saveTycoon(t);
                MessageUtil.sendRaw(p, "§a✓ Gave §e" + mult + "x §abooster for §e" + secs + "s §ato §e" + args[2]);
                Player target = Bukkit.getPlayer(uid);
                if (target != null) MessageUtil.sendRaw(target, "§6[MoneyTycoon] §aYou received a §e" + mult + "x §abooster for §e" + secs + "s §afrom an admin!");
            }
            case "reset" -> {
                if (args.length < 3) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin reset <player>"); return; }
                UUID uid = resolvePlayer(args[2]);
                Tycoon t = plugin.getTycoonManager().getTycoon(uid);
                if (t == null) { MessageUtil.sendRaw(p, "§cTycoon not found!"); return; }
                t.setBalance(0);
                t.setTotalEarned(0);
                t.setItemsSold(0);
                t.setPrestigeLevel(0);
                t.setRebirthLevel(0);
                t.setCurrentDropperType(Material.COBBLESTONE);
                t.getUpgrades().clear();
                t.setActivePetId(null);
                t.getOwnedPets().clear();
                t.setLastMilestone(0);
                t.setBooster(1.0, 0);
                plugin.getPetManager().despawnPet(t);
                plugin.getTycoonManager().cleanupEntities(t);
                plugin.getTycoonManager().clearBase(t);
                t.setTheme(TycoonTheme.CLASSIC);
                plugin.getTycoonManager().buildTycoonBase(t);
                plugin.getTycoonManager().setupLocations(t);
                plugin.getDatabaseManager().saveTycoon(t);
                MessageUtil.sendRaw(p, "§a✓ Fully reset §e" + args[2] + "§a's tycoon progress.");
            }
            case "backup" -> {
                plugin.getDatabaseManager().saveAllTycoons(plugin.getTycoonManager().getAllTycoons());
                plugin.getQuestManager().saveAll();
                try {
                    java.io.File src = new java.io.File(plugin.getDataFolder(), "data.db");
                    if (!src.exists()) { MessageUtil.sendRaw(p, "§cNo database file to backup!"); break; }
                    java.io.File backupDir = new java.io.File(plugin.getDataFolder(), "backups");
                    if (!backupDir.exists()) backupDir.mkdirs();
                    String name = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".db";
                    java.io.File dest = new java.io.File(backupDir, name);
                    java.nio.file.Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    MessageUtil.sendRaw(p, "§a✓ Backup saved: §e" + name);
                } catch (Exception ex) { MessageUtil.sendRaw(p, "§cBackup failed: " + ex.getMessage()); }
            }
            case "saveall" -> {
                plugin.getDatabaseManager().saveAllTycoons(plugin.getTycoonManager().getAllTycoons());
                plugin.getQuestManager().saveAll();
                plugin.getLeaderboardManager().refresh();
                MessageUtil.sendRaw(p, "§a✓ All data saved and leaderboard refreshed. §7("
                        + plugin.getTycoonManager().getAllTycoons().size() + " tycoons)");
            }
            case "broadcast" -> {
                if (args.length < 3) { MessageUtil.sendRaw(p, "§cUsage: /tycoon admin broadcast <message>"); return; }
                String msg = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                for (Player online : Bukkit.getOnlinePlayers()) {
                    MessageUtil.sendRaw(online, "§6§l[MoneyTycoon] §e" + msg.replace("&", "§"));
                    if (plugin.getSettingsManager().isSoundsEnabled(online.getUniqueId()))
                        online.playSound(online.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }
                MessageUtil.sendRaw(p, "§a✓ Broadcast sent to §e" + Bukkit.getOnlinePlayers().size() + " §aplayers.");
            }
            default -> sendAdminHelp(p);
        }
    }

    private void handleTrade(Player p, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendRaw(p, "§6[Trade] §eUsage:");
            MessageUtil.sendRaw(p, "§e  /tycoon trade <player> §7- Send trade request");
            MessageUtil.sendRaw(p, "§e  /tycoon trade accept §7- Accept incoming trade");
            MessageUtil.sendRaw(p, "§e  /tycoon trade deny §7- Deny incoming trade");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "accept" -> plugin.getTradeManager().acceptRequest(p);
            case "deny", "decline" -> plugin.getTradeManager().denyRequest(p);
            default -> {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { MessageUtil.sendRaw(p, "§6[Trade] §cPlayer not found or offline!"); return; }
                plugin.getTradeManager().sendRequest(p, target);
            }
        }
    }

    private void sendAdminHelp(Player p) {
        MessageUtil.sendRaw(p, "§c§l⚡ MoneyTycoon Admin Commands ⚡");
        MessageUtil.sendRaw(p, "§8§m──────────────────────────────────────");
        MessageUtil.sendRaw(p, "§7 §lServer Management:");
        MessageUtil.sendRaw(p, "§e  /tycoon admin reload §7- Reload config");
        MessageUtil.sendRaw(p, "§e  /tycoon admin backup §7- Backup database");
        MessageUtil.sendRaw(p, "§e  /tycoon admin saveall §7- Force save all data");
        MessageUtil.sendRaw(p, "§e  /tycoon admin broadcast <msg> §7- Broadcast to all");
        MessageUtil.sendRaw(p, "§e  /tycoon admin list §7- List all tycoons");
        MessageUtil.sendRaw(p, "");
        MessageUtil.sendRaw(p, "§7 §lPlayer Information:");
        MessageUtil.sendRaw(p, "§e  /tycoon admin info <player> §7- View tycoon details");
        MessageUtil.sendRaw(p, "§e  /tycoon admin tp <player> §7- Teleport to tycoon");
        MessageUtil.sendRaw(p, "");
        MessageUtil.sendRaw(p, "§7 §lEconomy:");
        MessageUtil.sendRaw(p, "§e  /tycoon admin give <player> <amount> §7- Give wallet money");
        MessageUtil.sendRaw(p, "§e  /tycoon admin setbalance <player> <amount> §7- Set tycoon balance");
        MessageUtil.sendRaw(p, "");
        MessageUtil.sendRaw(p, "§7 §lProgression:");
        MessageUtil.sendRaw(p, "§e  /tycoon admin setprestige <player> <level> §7- Set prestige");
        MessageUtil.sendRaw(p, "§e  /tycoon admin setrebirth <player> <level> §7- Set rebirth");
        MessageUtil.sendRaw(p, "§e  /tycoon admin setdropper <player> <type> §7- Set dropper");
        MessageUtil.sendRaw(p, "§e  /tycoon admin settheme <player> <theme> §7- Set theme");
        MessageUtil.sendRaw(p, "");
        MessageUtil.sendRaw(p, "§7 §lRewards:");
        MessageUtil.sendRaw(p, "§e  /tycoon admin givepet <player> <pet> §7- Give pet");
        MessageUtil.sendRaw(p, "§e  /tycoon admin givebooster <player> <mult> <secs> §7- Give booster");
        MessageUtil.sendRaw(p, "");
        MessageUtil.sendRaw(p, "§7 §lDangerous:");
        MessageUtil.sendRaw(p, "§c  /tycoon admin delete <player> §7- Delete tycoon");
        MessageUtil.sendRaw(p, "§c  /tycoon admin reset <player> §7- Reset all progress");
        MessageUtil.sendRaw(p, "§8§m──────────────────────────────────────");
    }

    private UUID resolvePlayer(String name) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) return online.getUniqueId();
        @SuppressWarnings("deprecation")
        var offline = Bukkit.getOfflinePlayer(name);
        return offline.getUniqueId();
    }

    private String locStr(org.bukkit.Location loc) {
        return loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    private void sendHelp(Player p) {
        MessageUtil.sendRaw(p, "§6§l⚡ MoneyTycoon Help ⚡");
        MessageUtil.sendRaw(p, "§8§m─────────────────────────");
        MessageUtil.sendRaw(p, "§e/tycoon create §7- Create your tycoon");
        MessageUtil.sendRaw(p, "§e/tycoon delete §7- Delete your tycoon");
        MessageUtil.sendRaw(p, "§e/tycoon menu §7- Main menu");
        MessageUtil.sendRaw(p, "§e/tycoon upgrades §7- Upgrades shop");
        MessageUtil.sendRaw(p, "§e/tycoon collect §7- Collect money");
        MessageUtil.sendRaw(p, "§e/tycoon visit <player> §7- Visit a tycoon");
        MessageUtil.sendRaw(p, "§e/tycoon tp §7- Teleport to your tycoon");
        MessageUtil.sendRaw(p, "§e/tycoon stats §7- View statistics");
        MessageUtil.sendRaw(p, "§e/tycoon prestige §7- Prestige up");
        MessageUtil.sendRaw(p, "§e/tycoon rebirth §7- Rebirth");
        MessageUtil.sendRaw(p, "§e/tycoon quest §7- Quests menu");
        MessageUtil.sendRaw(p, "§e/tycoon pet §7- Pets menu");
        MessageUtil.sendRaw(p, "§e/tycoon theme §7- Change theme");
        MessageUtil.sendRaw(p, "§e/tycoon booster §7- Buy boosters");
        MessageUtil.sendRaw(p, "§e/tycoon coop [invite|kick|list] §7- Co-op");
        MessageUtil.sendRaw(p, "§e/tycoon leaderboard §7- Leaderboard");
        MessageUtil.sendRaw(p, "§8§m─────────────────────────");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        String input = args.length > 0 ? args[args.length - 1].toLowerCase() : "";

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(List.of(
                    "create", "delete", "menu", "upgrades", "collect",
                    "visit", "tp", "stats", "prestige", "rebirth",
                    "quest", "pet", "theme", "booster", "milestones",
                    "profile", "shop", "trade", "coop", "leaderboard", "help"));
            if (sender.hasPermission("moneytycoon.admin")) subs.add("admin");
            return filter(subs, input);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "visit" -> filterPlayers(input);
                case "trade" -> {
                    List<String> opts = new ArrayList<>(List.of("accept", "deny"));
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).forEach(opts::add);
                    yield filter(opts, input);
                }
                case "coop" -> filter(List.of("invite", "kick", "list"), input);
                case "admin" -> sender.hasPermission("moneytycoon.admin")
                        ? filter(List.of("reload", "backup", "saveall", "broadcast", "list", "info", "tp",
                        "give", "setbalance", "setprestige", "setrebirth", "setdropper", "settheme",
                        "givepet", "givebooster", "delete", "reset"), input)
                        : Collections.emptyList();
                case "theme" -> {
                    List<String> themes = new ArrayList<>();
                    for (TycoonTheme th : TycoonTheme.values()) themes.add(th.name().toLowerCase());
                    yield filter(themes, input);
                }
                case "pet" -> {
                    ConfigurationSection sec = plugin.getConfig().getConfigurationSection("pets");
                    yield sec != null ? filter(new ArrayList<>(sec.getKeys(false)), input) : Collections.emptyList();
                }
                case "booster" -> {
                    ConfigurationSection sec = plugin.getConfig().getConfigurationSection("boosters");
                    yield sec != null ? filter(new ArrayList<>(sec.getKeys(false)), input) : Collections.emptyList();
                }
                case "upgrades" -> filter(List.of(
                        "dropper-speed", "value-multiplier", "conveyor-speed",
                        "auto-collect", "extra-dropper", "processor"), input);
                default -> Collections.emptyList();
            };
        }

        if (args.length == 3) {
            return switch (args[0].toLowerCase()) {
                case "coop" -> switch (args[1].toLowerCase()) {
                    case "invite" -> filterPlayers(input);
                    case "kick" -> {
                        if (sender instanceof Player p) {
                            Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
                            if (t != null) {
                                List<String> names = new ArrayList<>();
                                for (UUID mid : t.getCoopMembers()) {
                                    String name = Bukkit.getOfflinePlayer(mid).getName();
                                    if (name != null) names.add(name);
                                }
                                yield filter(names, input);
                            }
                        }
                        yield Collections.emptyList();
                    }
                    default -> Collections.emptyList();
                };
                case "admin" -> {
                    String sub = args[1].toLowerCase();
                    if (List.of("delete", "info", "tp", "give", "setbalance", "setprestige",
                            "setrebirth", "setdropper", "settheme", "givepet", "givebooster", "reset").contains(sub)) {
                        List<String> names = new ArrayList<>();
                        for (Tycoon tc : plugin.getTycoonManager().getAllTycoons()) names.add(tc.getOwnerName());
                        yield filter(names, input);
                    }
                    yield Collections.emptyList();
                }
                default -> Collections.emptyList();
            };
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("admin")) {
            return switch (args[1].toLowerCase()) {
                case "setdropper" -> {
                    ConfigurationSection sec = plugin.getConfig().getConfigurationSection("dropper.types");
                    yield sec != null ? filter(new ArrayList<>(sec.getKeys(false)), input) : Collections.emptyList();
                }
                case "settheme" -> {
                    List<String> themes = new ArrayList<>();
                    for (TycoonTheme th : TycoonTheme.values()) themes.add(th.name());
                    yield filter(themes, input);
                }
                case "givepet" -> {
                    ConfigurationSection sec = plugin.getConfig().getConfigurationSection("pets");
                    yield sec != null ? filter(new ArrayList<>(sec.getKeys(false)), input) : Collections.emptyList();
                }
                case "give", "setbalance" -> filter(List.of("100", "1000", "10000", "100000", "1000000"), input);
                case "setprestige", "setrebirth" -> filter(List.of("0", "1", "5", "10", "25", "50", "100"), input);
                case "givebooster" -> filter(List.of("2", "3", "5", "10"), input);
                default -> Collections.emptyList();
            };
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("givebooster")) {
            return filter(List.of("60", "120", "300", "600", "1800", "3600"), input);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> filterPlayers(String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(input))
                .sorted()
                .collect(Collectors.toList());
    }
}
