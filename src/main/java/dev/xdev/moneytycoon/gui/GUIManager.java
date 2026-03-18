package dev.xdev.moneytycoon.gui;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.*;
import dev.xdev.moneytycoon.util.MessageUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.*;

public class GUIManager {

    private final MoneyTycoon plugin;
    private static final LegacyComponentSerializer L = LegacyComponentSerializer.legacySection();

    public GUIManager(MoneyTycoon plugin) { this.plugin = plugin; }

    public static class H implements InventoryHolder {
        public final String type;
        public H(String t) { this.type = t; }
        @Override public Inventory getInventory() { return null; }
    }

    private static final Material[] GRADIENT_GOLD = {
            Material.YELLOW_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE
    };
    private static final Material[] GRADIENT_AQUA = {
            Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE
    };
    private static final Material[] GRADIENT_FIRE = {
            Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE, Material.RED_STAINED_GLASS_PANE
    };
    private static final Material[] GRADIENT_PURPLE = {
            Material.PURPLE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE,
            Material.PINK_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE
    };
    private static final Material[] GRADIENT_GREEN = {
            Material.LIME_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE, Material.WHITE_STAINED_GLASS_PANE
    };

    public void openMainMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("main"), 54, L.deserialize("§6§l⚡ MoneyTycoon ⚡"));

        inv.setItem(4, item(Material.PLAYER_HEAD, "§6§l" + p.getName() + "'s Tycoon",
                "§8§m─────────────────",
                "§7 Balance: §e$" + fmt(t.getBalance()),
                "§7 Total Earned: §a$" + fmt(t.getTotalEarned()),
                "§7 Items Sold: §b" + fmtInt(t.getItemsSold()),
                "§8§m─────────────────",
                "§7 Prestige: §6" + t.getPrestigeLevel() + " §7(" + String.format("%.1f", t.getPrestigeMultiplier()) + "x)",
                "§7 Rebirth: §d" + t.getRebirthLevel() + " §7(" + String.format("%.1f", t.getRebirthMultiplier()) + "x)",
                "§7 Theme: §f" + t.getTheme().name(),
                "§8§m─────────────────"));

        inv.setItem(19, glow(item(Material.DIAMOND_PICKAXE, "§e§l⚙ Upgrades", "§8§m──────────────", "§7Purchase upgrades to", "§7improve your factory.", "", "§a▶ Click to open")));
        inv.setItem(20, glow(item(Material.AMETHYST_SHARD, "§b§l⛏ Droppers", "§8§m──────────────", "§7Buy higher value", "§7dropper types.", "", "§a▶ Click to open")));
        inv.setItem(21, glow(item(Material.BRUSH, "§d§l🎨 Themes", "§8§m──────────────", "§7Change your tycoon's", "§7visual appearance.", "", "§a▶ Click to open")));
        inv.setItem(22, glow(item(Material.RAW_GOLD, "§a§l💰 Collect §7($" + fmt(t.getBalance()) + ")",
                "§8§m──────────────", "§7Collect accumulated", "§7money to your wallet.",
                "", t.getBalance() > 0 ? "§a▶ Click to collect!" : "§c✗ No money yet")));
        inv.setItem(23, glow(item(Material.TROPICAL_FISH, "§d§l🐾 Pets", "§8§m──────────────", "§7Buy and activate pets", "§7for passive bonuses.",
                "", "§7Active: §f" + (t.getActivePetId() != null ? t.getActivePetId() : "None"), "", "§a▶ Click to open")));
        inv.setItem(24, glow(item(Material.WRITABLE_BOOK, "§e§l📜 Quests", "§8§m──────────────", "§7Complete daily & weekly", "§7quests for rewards.", "", "§a▶ Click to open")));
        inv.setItem(25, glow(item(Material.FIREWORK_STAR, "§e§l⚡ Boosters", "§8§m──────────────",
                t.hasActiveBooster() ? "§aActive: §e" + String.format("%.0f", t.getBoosterMultiplier()) + "x §7(" + t.getBoosterSecondsLeft() + "s)"
                        : "§7Buy timed multipliers!", "", "§a▶ Click to open")));

        inv.setItem(29, glow(item(Material.NETHER_STAR, "§6§l✦ Prestige",
                "§8§m──────────────", "§7Level: §e" + t.getPrestigeLevel(),
                "§7Multiplier: §e" + String.format("%.1f", t.getPrestigeMultiplier()) + "x",
                "", "§c⚠ Resets all upgrades!", "", "§6▶ Click to prestige")));
        inv.setItem(30, glow(item(Material.DRAGON_EGG, "§d§l✦ Rebirth",
                "§8§m──────────────", "§7Level: §e" + t.getRebirthLevel(),
                "§7Multiplier: §e" + String.format("%.1f", t.getRebirthMultiplier()) + "x",
                "§7Requires Prestige: §e" + plugin.getConfig().getInt("rebirth.prestige-requirement", 10),
                "", "§c⚠ Resets prestige!", "", "§d▶ Click to rebirth")));
        inv.setItem(31, glow(item(Material.TOTEM_OF_UNDYING, "§6§l⭐ Milestones",
                "§8§m──────────────", "§7View your progress", "§7and earn rewards!", "", "§a▶ Click to open")));
        inv.setItem(32, glow(item(Material.DIAMOND_BLOCK, "§b§l🏆 Leaderboard",
                "§8§m──────────────", "§7See the top players", "§7on the server.", "", "§a▶ Click to open")));
        inv.setItem(33, glow(item(Material.RECOVERY_COMPASS, "§a§l👥 Co-op",
                "§8§m──────────────", "§7Members: §f" + t.getCoopMembers().size(),
                "§7Invite friends to", "§7your tycoon.", "", "§a▶ Click to open")));
        inv.setItem(34, glow(item(Material.SPYGLASS, "§a§l🌐 Visit",
                "§8§m──────────────", "§7Visit other players'", "§7tycoons.", "", "§7Use: §e/tycoon visit <player>")));

        inv.setItem(38, glow(item(Material.ENDER_CHEST, "§6§l🏪 Shop",
                "§8§m──────────────", "§7Buy special items,", "§7boosters & mystery boxes!", "", "§a▶ Click to open")));
        inv.setItem(40, glow(item(Material.EMERALD_BLOCK, "§a§l📊 Profile",
                "§8§m──────────────", "§7View detailed stats", "§7and achievements.", "", "§a▶ Click to open")));
        inv.setItem(41, glow(item(Material.COMPARATOR, "§7§l⚙ Settings",
                "§8§m──────────────", "§7Sounds, trade requests, visits,", "§7actionbar, bossbar, notifications", "", "§a▶ Click to open")));
        inv.setItem(42, glow(item(Material.BELL, "§e§l⇄ Trade",
                "§8§m──────────────", "§7Trade with other players!", "§7Use: §e/tycoon trade <player>")));
        inv.setItem(49, item(Material.BARRIER, "§c§lClose", "§7Close this menu"));

        gradientBorder(inv, GRADIENT_GOLD);
        p.openInventory(inv);
    }

    public void openUpgradeMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("upgrades"), 36, L.deserialize("§e§l⚙ Upgrades Shop"));
        String[] ids = {"dropper-speed", "value-multiplier", "conveyor-speed", "auto-collect", "extra-dropper", "processor"};
        Material[] icons = {Material.CLOCK, Material.RAW_GOLD, Material.POWERED_RAIL, Material.COPPER_INGOT, Material.PISTON, Material.BLAST_FURNACE};
        for (int i = 0; i < ids.length; i++) {
            int lvl = t.getUpgradeLevel(ids[i]);
            int max = plugin.getTycoonManager().getUpgradeMaxLevel(ids[i]);
            double cost = plugin.getTycoonManager().getUpgradeCost(ids[i], lvl);
            boolean mx = lvl >= max;
            String bar = buildProgressBar(lvl, max);
            inv.setItem(10 + i, item(icons[i], cfg("upgrades." + ids[i] + ".display-name") + " §7[" + lvl + "/" + max + "]",
                    "§8§m──────────────",
                    cfg("upgrades." + ids[i] + ".description"),
                    "", "§7Progress: " + bar,
                    "", mx ? "§a§l✓ MAX LEVEL!" : "§7Cost: §e$" + fmt(cost),
                    mx ? "" : "§a▶ Click to purchase!"));
            if (mx) inv.setItem(10 + i, glow(inv.getItem(10 + i)));
        }
        inv.setItem(31, glow(item(Material.SPECTRAL_ARROW, "§7← Back to Menu", "")));
        gradientBorder(inv, GRADIENT_FIRE);
        p.openInventory(inv);
    }

    public void openDropperMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("droppers"), 36, L.deserialize("§b§l⛏ Dropper Types"));
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("dropper.types");
        if (sec == null) return;
        List<String> keys = new ArrayList<>(sec.getKeys(false));
        keys.sort(Comparator.comparingInt(k -> sec.getInt(k + ".order", 0)));
        int slot = 10;
        for (String key : keys) {
            Material mat; try { mat = Material.valueOf(key); } catch (Exception e) { continue; }
            boolean cur = t.getCurrentDropperType() == mat;
            boolean vip = sec.getBoolean(key + ".vip", false);
            double cost = sec.getDouble(key + ".cost", 0);
            double value = sec.getDouble(key + ".value", 1);
            inv.setItem(slot, item(mat, cfg("dropper.types." + key + ".display-name") + (cur ? " §a§l✓" : ""),
                    "§8§m──────────────",
                    "§7Base Value: §e$" + fmt(value) + " §7/ item",
                    "§7Effective Value: §a$" + fmt(value * t.getValueMultiplier()),
                    "", vip ? "§d§l★ VIP EXCLUSIVE" : "",
                    cur ? "§a§l✓ CURRENTLY ACTIVE" : (cost <= 0 ? "§a▸ Click to select!" : "§7Cost: §e$" + fmt(cost)),
                    !cur && cost > 0 ? "§a▸ Click to purchase!" : ""));
            slot++; if (slot == 17) slot = 19;
        }
        inv.setItem(31, glow(item(Material.SPECTRAL_ARROW, "§7← Back to Menu", "")));
        gradientBorder(inv, GRADIENT_AQUA);
        p.openInventory(inv);
    }

    public void openThemeMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("themes"), 36, L.deserialize("§d§l🎨 Themes"));
        int slot = 10;
        for (TycoonTheme th : TycoonTheme.values()) {
            boolean cur = t.getTheme() == th;
            boolean vip = plugin.getConfig().getBoolean("themes." + th.name() + ".vip", false);
            double cost = plugin.getConfig().getDouble("themes." + th.name() + ".cost", 0);
            inv.setItem(slot, item(th.floor, cfg("themes." + th.name() + ".display-name") + (cur ? " §a§l✓" : ""),
                    "§8§m──────────────",
                    "§7Floor: §f" + th.floor.name(), "§7Walls: §f" + th.wall.name(),
                    "§7Conveyor: §f" + th.conveyor.name(), "",
                    vip ? "§d§l★ VIP EXCLUSIVE" : "",
                    cur ? "§a§l✓ CURRENTLY ACTIVE" : (cost <= 0 ? "§aFree - Click to apply!" : "§7Cost: §e$" + fmt(cost)),
                    !cur && cost > 0 ? "§a▸ Click to purchase!" : ""));
            slot++;
        }
        inv.setItem(31, glow(item(Material.SPECTRAL_ARROW, "§7← Back to Menu", "")));
        gradientBorder(inv, GRADIENT_PURPLE);
        p.openInventory(inv);
    }

    public void openQuestMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("quests"), 45, L.deserialize("§e§l📜 Quests"));

        inv.setItem(4, item(Material.CLOCK, "§e§lQuest Board",
                "§7Complete quests to earn rewards!", "§7Quests reset automatically."));

        Map<String, Quest> quests = plugin.getQuestManager().getPlayerQuests(p.getUniqueId());
        int dailySlot = 19, weeklySlot = 28;
        for (var entry : quests.entrySet()) {
            Quest q = entry.getValue();
            Material icon;
            if (q.isClaimed()) icon = Material.GRAY_DYE;
            else if (q.isCompleted()) icon = Material.LIME_DYE;
            else icon = Material.ORANGE_DYE;

            String progressBar = buildProgressBar(q.getProgress(), q.getTarget());
            String status = q.isClaimed() ? "§7§m§oCompleted & Claimed"
                    : (q.isCompleted() ? "§a§l⬆ CLICK TO CLAIM!" : "§e" + q.getProgress() + "/" + q.getTarget());

            int targetSlot = q.isDaily() ? dailySlot++ : weeklySlot++;
            inv.setItem(targetSlot, item(icon, q.getDisplayName(),
                    "§8§m──────────────", q.getDescription(), "",
                    "§7Progress: " + progressBar, "§7Status: " + status,
                    "", "§7Reward: §e$" + fmt(q.getReward()),
                    q.isDaily() ? "§b⟳ Resets Daily" : "§6⟳ Resets Weekly"));
        }

        inv.setItem(11, glow(item(Material.SUNFLOWER, "§b§lDaily Quests", "§7Resets every 24 hours")));
        inv.setItem(15, glow(item(Material.GOLDEN_APPLE, "§6§lWeekly Quests", "§7Resets every Monday")));
        inv.setItem(40, glow(item(Material.SPECTRAL_ARROW, "§7← Back to Menu", "")));
        gradientBorder(inv, GRADIENT_FIRE);
        p.openInventory(inv);
    }

    public void openPetMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("pets"), 36, L.deserialize("§d§l🐾 Pet Shop"));

        inv.setItem(4, glow(item(Material.NAME_TAG, "§d§lYour Pets",
                "§7Active Pet: §f" + (t.getActivePetId() != null ? t.getActivePetId() : "None"),
                "§7Owned: §f" + t.getOwnedPets().size() + "/" + TycoonPet.values().length)));

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("pets");
        if (sec == null) return;
        int slot = 19;
        for (String key : sec.getKeys(false)) {
            boolean owned = t.getOwnedPets().contains(key);
            boolean active = key.equals(t.getActivePetId());
            boolean vip = sec.getBoolean(key + ".vip", false);
            double cost = sec.getDouble(key + ".cost", 0);
            Material icon = active ? Material.SPAWNER : (owned ? Material.EGG : Material.IRON_BARS);
            inv.setItem(slot, item(icon, cfg("pets." + key + ".display-name") + (active ? " §a§l✓" : ""),
                    "§8§m──────────────", cfg("pets." + key + ".description"), "",
                    vip ? "§d§l★ VIP EXCLUSIVE" : "",
                    active ? "§a§l✓ ACTIVE PET" : (owned ? "§a▸ Click to activate!" : "§7Cost: §e$" + fmt(cost)),
                    !owned && !active ? "§a▸ Click to purchase!" : ""));
            slot++;
        }
        inv.setItem(31, item(Material.ARROW, "§7← Back to Menu", ""));
        gradientBorder(inv, GRADIENT_PURPLE);
        p.openInventory(inv);
    }

    public void openBoosterMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("boosters"), 27, L.deserialize("§e§l⚡ Boosters"));

        if (t.hasActiveBooster()) {
            inv.setItem(4, item(Material.GLOWSTONE, "§a§l⚡ Active Booster",
                    "§8§m──────────────",
                    "§7Multiplier: §e" + String.format("%.0f", t.getBoosterMultiplier()) + "x",
                    "§7Time Left: §e" + t.getBoosterSecondsLeft() + " seconds",
                    "", "§7All earnings are multiplied!"));
        } else {
            inv.setItem(4, item(Material.COAL, "§7§lNo Active Booster",
                    "§7Purchase one below to", "§7multiply your earnings!"));
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("boosters");
        if (sec != null) {
            int slot = 11;
            for (String key : sec.getKeys(false)) {
                double mult = sec.getDouble(key + ".multiplier", 2.0);
                int dur = sec.getInt(key + ".duration", 300);
                double cost = sec.getDouble(key + ".cost", 5000);
                Material icon = mult >= 5 ? Material.BEACON : (mult >= 3 ? Material.BLAZE_POWDER : Material.GLOWSTONE_DUST);
                inv.setItem(slot, item(icon, cfg("boosters." + key + ".display-name"),
                        "§8§m──────────────",
                        "§7Multiplier: §e" + String.format("%.0f", mult) + "x all earnings",
                        "§7Duration: §e" + dur + " seconds",
                        "§7Cost: §e$" + fmt(cost), "",
                        t.hasActiveBooster() ? "§c✗ Already have active booster!" : "§a▸ Click to activate!"));
                slot += 2;
            }
        }
        inv.setItem(22, item(Material.ARROW, "§7← Back to Menu", ""));
        gradientBorder(inv, GRADIENT_GOLD);
        p.openInventory(inv);
    }

    public void openMilestoneMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("milestones"), 45, L.deserialize("§6§l⭐ Milestones"));

        inv.setItem(4, item(Material.GOLD_BLOCK, "§6§lYour Progress",
                "§7Total Earned: §e$" + fmt(t.getTotalEarned()),
                "§7Last Milestone: §e$" + fmt(t.getLastMilestone())));

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("milestones");
        if (sec != null) {
            List<String> keys = new ArrayList<>(sec.getKeys(false));
            keys.sort(Comparator.comparingLong(Long::parseLong));
            int slot = 19;
            for (String key : keys) {
                long threshold = Long.parseLong(key);
                double reward = sec.getDouble(key + ".reward", 0);
                boolean reached = t.getTotalEarned() >= threshold;
                double progress = Math.min(1.0, (double) t.getTotalEarned() / threshold);

                Material icon = reached ? Material.EMERALD : Material.COAL;
                String progressBar = buildProgressBar((int) (progress * 20), 20);

                inv.setItem(slot, item(icon,
                        (reached ? "§a§l✓ " : "§7§l✗ ") + "§e$" + fmt(threshold) + " Milestone",
                        "§8§m──────────────",
                        "§7Reward: §e$" + fmt(reward),
                        "§7Progress: " + progressBar,
                        "§7 " + String.format("%.1f", progress * 100) + "%",
                        "", reached ? "§a§l✓ COMPLETED!" : "§c Not yet reached"));
                slot++;
                if (slot == 26) slot = 28;
                if (slot == 35) break;
            }
        }
        inv.setItem(40, item(Material.ARROW, "§7← Back to Menu", ""));
        gradientBorder(inv, GRADIENT_GREEN);
        p.openInventory(inv);
    }

    public void openProfileMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("profile"), 45, L.deserialize("§a§l📊 Player Profile"));

        inv.setItem(4, item(Material.PLAYER_HEAD, "§6§l" + p.getName(),
                "§8§m─────────────────────",
                "§7 Tycoon Owner",
                "§8§m─────────────────────"));

        inv.setItem(19, item(Material.GOLD_INGOT, "§e§l💰 Economy",
                "§8§m──────────────",
                "§7 Balance: §e$" + fmt(t.getBalance()),
                "§7 Wallet: §a$" + fmt(plugin.getEconomyManager().getBalance(p.getUniqueId())),
                "§7 Total Earned: §6$" + fmt(t.getTotalEarned()),
                "§7 Items Sold: §b" + fmtInt(t.getItemsSold())));

        inv.setItem(20, item(Material.EXPERIENCE_BOTTLE, "§6§l⚡ Progression",
                "§8§m──────────────",
                "§7 Prestige: §6Lv." + t.getPrestigeLevel() + " §7(" + String.format("%.1f", t.getPrestigeMultiplier()) + "x)",
                "§7 Rebirth: §dLv." + t.getRebirthLevel() + " §7(" + String.format("%.1f", t.getRebirthMultiplier()) + "x)",
                "§7 Total Mult: §a" + String.format("%.1f", t.getValueMultiplier()) + "x"));

        inv.setItem(21, item(Material.DROPPER, "§b§l🏭 Factory",
                "§8§m──────────────",
                "§7 Dropper: §f" + t.getCurrentDropperType().name(),
                "§7 Theme: §f" + t.getTheme().name(),
                "§7 Speed Lv: §e" + t.getUpgradeLevel("dropper-speed"),
                "§7 Value Lv: §e" + t.getUpgradeLevel("value-multiplier"),
                "§7 Conv. Lv: §e" + t.getUpgradeLevel("conveyor-speed"),
                "§7 Processor Lv: §e" + t.getUpgradeLevel("processor"),
                "§7 Extra Droppers: §e" + t.getUpgradeLevel("extra-dropper")));

        inv.setItem(23, item(Material.PLAYER_HEAD, "§a§l👥 Social",
                "§8§m──────────────",
                "§7 Co-op Members: §f" + t.getCoopMembers().size(),
                "§7 Pet: §f" + (t.getActivePetId() != null ? t.getActivePetId() : "None")));

        inv.setItem(24, item(Material.FIREWORK_ROCKET, "§e§l⚡ Booster",
                "§8§m──────────────",
                t.hasActiveBooster()
                        ? "§a Active: " + String.format("%.0f", t.getBoosterMultiplier()) + "x (" + t.getBoosterSecondsLeft() + "s)"
                        : "§7 No active booster"));

        boolean isVip = p.hasPermission("moneytycoon.vip");
        inv.setItem(25, item(isVip ? Material.DIAMOND : Material.BARRIER,
                isVip ? "§d§l★ VIP STATUS" : "§7§lNo VIP",
                "§8§m──────────────",
                isVip ? "§a✓ VIP Active!" : "§7Not a VIP member",
                isVip ? "§7 2x Money Multiplier" : "",
                isVip ? "§7 Exclusive Items" : "",
                isVip ? "§7 5 Co-op Slots" : ""));

        inv.setItem(40, item(Material.ARROW, "§7← Back to Menu", ""));
        gradientBorder(inv, GRADIENT_GREEN);
        p.openInventory(inv);
    }

    public void openPrestigeConfirm(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        double cost = plugin.getConfig().getDouble("prestige.base-cost", 100000)
                * Math.pow(plugin.getConfig().getDouble("prestige.cost-multiplier", 3.0), t.getPrestigeLevel());
        Inventory inv = Bukkit.createInventory(new H("confirm-prestige"), 27, L.deserialize("§6§l✦ Confirm Prestige?"));

        inv.setItem(4, item(Material.NETHER_STAR, "§6§l✦ PRESTIGE",
                "§8§m──────────────",
                "§7Current Level: §e" + t.getPrestigeLevel(),
                "§7New Level: §a" + (t.getPrestigeLevel() + 1),
                "§7New Multiplier: §a" + String.format("%.1f", 1.0 + (t.getPrestigeLevel() + 1) * 0.5) + "x",
                "", "§7Cost: §e$" + fmt(cost),
                "", "§c⚠ ALL upgrades will be reset!",
                "§c⚠ Dropper type will be reset!"));
        inv.setItem(11, item(Material.LIME_WOOL, "§a§l✓ CONFIRM", "§7Click to prestige!"));
        inv.setItem(15, item(Material.RED_WOOL, "§c§l✗ CANCEL", "§7Go back to menu"));
        gradientBorder(inv, GRADIENT_FIRE);
        p.openInventory(inv);
    }

    public void openRebirthConfirm(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        double cost = plugin.getConfig().getDouble("rebirth.base-cost", 10000000)
                * Math.pow(plugin.getConfig().getDouble("rebirth.cost-multiplier", 5.0), t.getRebirthLevel());
        int reqPrestige = plugin.getConfig().getInt("rebirth.prestige-requirement", 10);
        Inventory inv = Bukkit.createInventory(new H("confirm-rebirth"), 27, L.deserialize("§d§l✦ Confirm Rebirth?"));

        inv.setItem(4, item(Material.END_CRYSTAL, "§d§l✦ REBIRTH",
                "§8§m──────────────",
                "§7Current Level: §e" + t.getRebirthLevel(),
                "§7New Level: §a" + (t.getRebirthLevel() + 1),
                "§7New Multiplier: §a" + String.format("%.1f", 1.0 + (t.getRebirthLevel() + 1) * 1.0) + "x",
                "", "§7Cost: §e$" + fmt(cost),
                "§7Requires Prestige: §e" + reqPrestige,
                "§7Your Prestige: §f" + t.getPrestigeLevel(),
                "", "§c⚠ Prestige & upgrades will be reset!"));
        inv.setItem(11, item(Material.LIME_WOOL, "§a§l✓ CONFIRM", "§7Click to rebirth!"));
        inv.setItem(15, item(Material.RED_WOOL, "§c§l✗ CANCEL", "§7Go back to menu"));
        gradientBorder(inv, GRADIENT_PURPLE);
        p.openInventory(inv);
    }

    public void openIslandTypeMenu(Player p) {
        if (plugin.getTycoonManager().getTycoon(p.getUniqueId()) != null) return;
        Inventory inv = Bukkit.createInventory(new H("island-type"), 36, L.deserialize("§a§l🏝 Choose Island Type"));
        inv.setItem(4, item(Material.GRASS_BLOCK, "§a§lCreate Your Tycoon",
                "§8§m──────────────", "§7Select an island type below", "§7to start your tycoon!"));

        List<IslandType> types = plugin.getTycoonManager().getAvailableIslandTypes();
        Material[] icons = {Material.GRASS_BLOCK, Material.EMERALD_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK};
        int slot = 10;
        for (int i = 0; i < types.size() && slot < 26; i++) {
            IslandType it = types.get(i);
            boolean canAfford = it.cost() <= 0 || plugin.getEconomyManager().has(p.getUniqueId(), it.cost());
            boolean vipOk = !it.vip() || p.hasPermission("moneytycoon.vip");
            Material icon = i < icons.length ? icons[i] : Material.GRASS_BLOCK;
            inv.setItem(slot, item(icon, it.displayName(),
                    "§8§m──────────────",
                    it.description(),
                    "",
                    "§7Plot Size: §f" + it.plotSize() + "x" + it.plotSize(),
                    it.cost() <= 0 ? "§a§lFREE" : "§7Cost: §e$" + fmt(it.cost()),
                    it.vip() ? "§d§l★ VIP EXCLUSIVE" : "",
                    "",
                    canAfford && vipOk ? "§a▶ Click to create!" : (canAfford ? "§c§lRequires VIP!" : "§c§lNot enough money!")));
            slot++;
            if (slot == 17) slot = 19;
        }
        inv.setItem(31, item(Material.BARRIER, "§c§lCancel", "§7Close without creating"));
        gradientBorder(inv, GRADIENT_GREEN);
        p.openInventory(inv);
    }

    public void openLeaderboard(Player p) {
        Inventory inv = Bukkit.createInventory(new H("leaderboard"), 36, L.deserialize("§b§l🏆 Leaderboard"));
        inv.setItem(4, item(Material.DIAMOND, "§b§lTop Players", "§7Ranked by total earnings"));

        List<String[]> top = plugin.getLeaderboardManager().getTop();
        Material[] trophies = {Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK,
                Material.LAPIS_BLOCK, Material.REDSTONE_BLOCK, Material.COAL_BLOCK,
                Material.STONE, Material.STONE, Material.STONE, Material.STONE};
        String[] colors = {"§6§l", "§7§l", "§c§l", "§9", "§c", "§8", "§7", "§7", "§7", "§7"};

        for (int i = 0; i < Math.min(top.size(), 10); i++) {
            String[] d = top.get(i);
            int slot = i < 3 ? (12 + i) : (18 + i);
            inv.setItem(slot, item(trophies[Math.min(i, trophies.length - 1)],
                    colors[Math.min(i, colors.length - 1)] + "#" + (i + 1) + " §f" + d[0],
                    "§8§m──────────────",
                    "§7Total Earned: §e$" + fmt(Long.parseLong(d[1])),
                    "§7Prestige: §6Lv." + d[2],
                    "§7Rebirth: §dLv." + d[3]));
        }
        inv.setItem(31, item(Material.ARROW, "§7← Back to Menu", ""));
        gradientBorder(inv, GRADIENT_AQUA);
        p.openInventory(inv);
    }

    public void openSettingsMenu(Player p) {
        var sm = plugin.getSettingsManager();
        UUID uid = p.getUniqueId();
        Inventory inv = Bukkit.createInventory(new H("settings"), 27, L.deserialize("§7§l⚙ Settings"));
        inv.setItem(4, item(Material.COMPARATOR, "§7§lPreferences",
                "§8§m──────────────", "§7Toggle features you find", "§7annoying or distracting.", "", "§eClick items to toggle"));

        inv.setItem(10, toggleItem(Material.NOTE_BLOCK, "§7Sounds", sm.isSoundsEnabled(uid),
                "§7GUI clicks, level-up, trade..."));
        inv.setItem(11, toggleItem(Material.BELL, "§7Trade Requests", sm.isTradeRequestsEnabled(uid),
                "§7Receive trade requests from others"));
        inv.setItem(12, toggleItem(Material.ENDER_PEARL, "§7Allow Visits", sm.isVisitsAllowed(uid),
                "§7Let others visit your tycoon"));
        inv.setItem(13, toggleItem(Material.NAME_TAG, "§7Actionbar", sm.isActionbarEnabled(uid),
                "§7Balance display above hotbar"));
        inv.setItem(14, toggleItem(Material.BARRIER, "§7Bossbar", sm.isBossbarEnabled(uid),
                "§7Prestige progress bar"));
        inv.setItem(15, toggleItem(Material.BOOK, "§7Notifications", sm.isNotificationsEnabled(uid),
                "§7Prestige/Rebirth ready alerts"));

        inv.setItem(22, item(Material.ARROW, "§7← Back to Menu", ""));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);
        p.openInventory(inv);
    }

    private ItemStack toggleItem(Material m, String name, boolean on, String desc) {
        return item(m, name + (on ? " §a§l✓" : " §c§l✗"),
                "§8§m──────────────", desc,
                "", on ? "§aEnabled §7- Click to disable" : "§cDisabled §7- Click to enable");
    }

    public void openCoopMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        int max = p.hasPermission("moneytycoon.vip")
                ? plugin.getConfig().getInt("coop.vip-max-members", 5)
                : plugin.getConfig().getInt("coop.max-members", 2);
        Inventory inv = Bukkit.createInventory(new H("coop"), 36, L.deserialize("§a§l👥 Co-op Manager"));
        inv.setItem(4, item(Material.PLAYER_HEAD, "§a§lCo-op Members §7(" + t.getCoopMembers().size() + "/" + max + ")",
                "§8§m──────────────",
                "§7Invite friends to your tycoon!", "",
                "§7Command: §e/tycoon coop invite <player>",
                "§7Kick: §e/tycoon coop kick <player>"));
        int slot = 19;
        for (UUID memberId : t.getCoopMembers()) {
            String name = Bukkit.getOfflinePlayer(memberId).getName();
            boolean online = Bukkit.getPlayer(memberId) != null;
            inv.setItem(slot, item(online ? Material.LIME_DYE : Material.GRAY_DYE,
                    "§f" + (name != null ? name : memberId.toString().substring(0, 8)),
                    "§7Status: " + (online ? "§aOnline" : "§cOffline"), "",
                    "§c▸ Click to kick"));
            slot++;
        }
        inv.setItem(31, item(Material.ARROW, "§7← Back to Menu", ""));
        gradientBorder(inv, GRADIENT_GREEN);
        p.openInventory(inv);
    }

    public void openHelpMenu(Player p) {
        Inventory inv = Bukkit.createInventory(new H("help"), 45, L.deserialize("§e§l📖 Help & Guide"));

        inv.setItem(10, item(Material.GRASS_BLOCK, "§a§lGetting Started",
                "§7 1. Type §e/tycoon create",
                "§7 2. Your factory auto-produces!",
                "§7 3. Collect money from the §echest",
                "§7 4. Buy upgrades at §eemerald block",
                "§7 5. View quests at §ebookshelf"));

        inv.setItem(12, item(Material.ANVIL, "§e§lUpgrades",
                "§7 Dropper Speed - Faster production",
                "§7 Value Multiplier - More money",
                "§7 Conveyor Speed - Faster transport",
                "§7 Auto Collect - Auto money deposit",
                "§7 Extra Dropper - More droppers",
                "§7 Processor - +30% value/level"));

        inv.setItem(14, item(Material.NETHER_STAR, "§6§lPrestige & Rebirth",
                "§7 §6Prestige: §fResets upgrades for",
                "§7 a permanent 0.5x multiplier.",
                "",
                "§7 §dRebirth: §fResets prestige for",
                "§7 a permanent 1.0x multiplier.",
                "§7 Requires Prestige Lv.10+"));

        inv.setItem(16, item(Material.BONE, "§d§lPets & Boosters",
                "§7 §dPets: §fBuy pets for passive",
                "§7 bonuses like auto-collect,",
                "§7 speed, value, double drop.",
                "",
                "§7 §eBoosters: §fBuy timed",
                "§7 multipliers (2x, 3x, 5x)."));

        inv.setItem(28, item(Material.GOLD_BLOCK, "§6§lMilestones",
                "§7 Earn automatic rewards when",
                "§7 you reach earnings milestones.",
                "§7 $1K, $10K, $100K, $1M, $10M"));

        inv.setItem(30, item(Material.PLAYER_HEAD, "§a§lCo-op System",
                "§7 Invite friends to your tycoon!",
                "§7 /tycoon coop invite <player>",
                "§7 Max 2 members (5 for VIP)"));

        inv.setItem(32, item(Material.DIAMOND, "§d§lVIP Perks",
                "§7 ★ 2x Money Multiplier",
                "§7 ★ Exclusive Droppers",
                "§7 ★ Royal Theme",
                "§7 ★ VIP Bee Pet",
                "§7 ★ Free Auto Collect",
                "§7 ★ 5 Co-op Slots"));

        inv.setItem(34, item(Material.COMMAND_BLOCK, "§c§lAdmin Commands",
                "§7 /tycoon admin reload",
                "§7 /tycoon admin delete <player>"));

        inv.setItem(40, item(Material.ARROW, "§7← Back to Menu", ""));
        gradientBorder(inv, GRADIENT_GOLD);
        p.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent ev) {
        if (!(ev.getInventory().getHolder() instanceof H h)) return;
        ev.setCancelled(true);
        Player p = (Player) ev.getWhoClicked();
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        ItemStack cl = ev.getCurrentItem();
        if (cl == null || cl.getType() == Material.AIR || isGlass(cl.getType())) return;

        switch (h.type) {
            case "main" -> handleMain(p, t, ev.getSlot());
            case "upgrades" -> handleUpgrade(p, t, ev.getSlot());
            case "droppers" -> handleDropper(p, t, ev.getSlot(), cl);
            case "themes" -> handleTheme(p, t, ev.getSlot());
            case "quests" -> handleQuest(p, ev.getSlot());
            case "pets" -> handlePet(p, t, ev.getSlot());
            case "boosters" -> handleBooster(p, t, ev.getSlot());
            case "milestones" -> { if (ev.getSlot() == 40) openMainMenu(p); }
            case "profile" -> { if (ev.getSlot() == 40) openMainMenu(p); }
            case "leaderboard" -> { if (ev.getSlot() == 31) openMainMenu(p); }
            case "settings" -> handleSettings(p, ev.getSlot());
            case "coop" -> handleCoop(p, t, ev.getSlot());
            case "help" -> { if (ev.getSlot() == 40) openMainMenu(p); }
            case "confirm-prestige" -> handlePrestigeConfirm(p, t, ev.getSlot());
            case "confirm-rebirth" -> handleRebirthConfirm(p, t, ev.getSlot());
            case "island-type" -> handleIslandType(p, ev.getSlot());
            case "shop" -> handleShop(p, t, ev.getSlot(), cl);
            case "trade" -> handleTrade(p, ev.getSlot());
            case "trade-pets" -> handleTradePetSelect(p, ev.getSlot());
        }
    }

    private void handleMain(Player p, Tycoon t, int slot) {
        if (t == null) return;
        playSoundIfEnabled(p, Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        switch (slot) {
            case 19 -> openUpgradeMenu(p);
            case 20 -> openDropperMenu(p);
            case 21 -> openThemeMenu(p);
            case 22 -> { if (t.getBalance() > 0) { double a = t.getBalance(); plugin.getTycoonManager().collectBalance(p, t);
                    MessageUtil.sendRaw(p, "§6[MoneyTycoon] §a$" + fmt(a) + " §acollected!"); openMainMenu(p); } }
            case 23 -> openPetMenu(p);
            case 24 -> openQuestMenu(p);
            case 25 -> openBoosterMenu(p);
            case 29 -> openPrestigeConfirm(p);
            case 30 -> openRebirthConfirm(p);
            case 31 -> openMilestoneMenu(p);
            case 32 -> openLeaderboard(p);
            case 33 -> openCoopMenu(p);
            case 38 -> openShopMenu(p);
            case 40 -> openProfileMenu(p);
            case 41 -> openSettingsMenu(p);
            case 49 -> p.closeInventory();
        }
    }

    private void handleUpgrade(Player p, Tycoon t, int slot) {
        if (t == null) return;
        String[] ids = {"dropper-speed", "value-multiplier", "conveyor-speed", "auto-collect", "extra-dropper", "processor"};
        int idx = slot - 10;
        if (idx < 0 || idx >= ids.length) { if (slot == 31) openMainMenu(p); return; }
        int cur = t.getUpgradeLevel(ids[idx]);
        if (cur >= plugin.getTycoonManager().getUpgradeMaxLevel(ids[idx])) { MessageUtil.send(p, "upgrade-max-level", Map.of()); return; }
        double cost = plugin.getTycoonManager().getUpgradeCost(ids[idx], cur);
        if (plugin.getTycoonManager().purchaseUpgrade(p, t, ids[idx])) {
            playSoundIfEnabled(p, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
            MessageUtil.send(p, "upgrade-purchased", Map.of("upgrade", cfg("upgrades." + ids[idx] + ".display-name"), "level", ""+(cur+1)));
            openUpgradeMenu(p);
        } else MessageUtil.send(p, "not-enough-money", Map.of("cost", fmt(cost)));
    }

    private void handleDropper(Player p, Tycoon t, int slot, ItemStack cl) {
        if (t == null) return;
        if (slot == 31) { openMainMenu(p); return; }
        Material type = cl.getType();
        if (isGlass(type) || type == Material.ARROW) return;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("dropper.types");
        if (sec == null || !sec.contains(type.name())) return;
        if (t.getCurrentDropperType() == type) return;
        if (plugin.getTycoonManager().purchaseDropperType(p, t, type)) {
            playSoundIfEnabled(p, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            MessageUtil.send(p, "dropper-unlocked", Map.of("dropper", cfg("dropper.types." + type.name() + ".display-name")));
            openDropperMenu(p);
        } else MessageUtil.send(p, "not-enough-money", Map.of("cost", fmt(sec.getDouble(type.name() + ".cost"))));
    }

    private void handleTheme(Player p, Tycoon t, int slot) {
        if (t == null) return;
        if (slot == 31) { openMainMenu(p); return; }
        int idx = slot - 10;
        TycoonTheme[] vals = TycoonTheme.values();
        if (idx < 0 || idx >= vals.length) return;
        if (t.getTheme() == vals[idx]) return;
        if (plugin.getTycoonManager().changeTheme(p, t, vals[idx])) {
            playSoundIfEnabled(p, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1f);
            MessageUtil.send(p, "theme-changed", Map.of("theme", cfg("themes." + vals[idx].name() + ".display-name")));
            p.closeInventory();
        } else MessageUtil.sendRaw(p, "§6[MoneyTycoon] §cCannot change theme! Check requirements.");
    }

    private void handleQuest(Player p, int slot) {
        if (slot == 40) { openMainMenu(p); return; }
        Map<String, Quest> quests = plugin.getQuestManager().getPlayerQuests(p.getUniqueId());
        int dailyIdx = slot - 19, weeklyIdx = slot - 28;
        List<String> dailyKeys = new ArrayList<>(), weeklyKeys = new ArrayList<>();
        for (var e : quests.entrySet()) {
            if (e.getValue().isDaily()) dailyKeys.add(e.getKey()); else weeklyKeys.add(e.getKey());
        }
        String questId = null;
        if (dailyIdx >= 0 && dailyIdx < dailyKeys.size()) questId = dailyKeys.get(dailyIdx);
        else if (weeklyIdx >= 0 && weeklyIdx < weeklyKeys.size()) questId = weeklyKeys.get(weeklyIdx);
        if (questId != null) {
            Quest q = quests.get(questId);
            if (q != null && q.isCompleted() && !q.isClaimed()) {
                plugin.getQuestManager().claimReward(p.getUniqueId(), q.getId());
                playSoundIfEnabled(p, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                MessageUtil.send(p, "quest-claimed", Map.of("reward", fmt(q.getReward())));
                openQuestMenu(p);
            }
        }
    }

    private void handlePet(Player p, Tycoon t, int slot) {
        if (t == null) return;
        if (slot == 31) { openMainMenu(p); return; }
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("pets");
        if (sec == null) return;
        List<String> keys = new ArrayList<>(sec.getKeys(false));
        int idx = slot - 19;
        if (idx < 0 || idx >= keys.size()) return;
        String petId = keys.get(idx);
        if (t.getOwnedPets().contains(petId)) {
            plugin.getPetManager().activatePet(p, t, petId);
            playSoundIfEnabled(p, Sound.ENTITY_CAT_PURREOW, 0.7f, 1f);
            MessageUtil.send(p, "pet-activated", Map.of("pet", cfg("pets." + petId + ".display-name")));
            openPetMenu(p);
        } else {
            if (plugin.getPetManager().purchasePet(p, t, petId)) {
                playSoundIfEnabled(p, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                MessageUtil.send(p, "pet-purchased", Map.of("pet", cfg("pets." + petId + ".display-name")));
                openPetMenu(p);
            } else MessageUtil.send(p, "not-enough-money", Map.of("cost", fmt(sec.getDouble(petId + ".cost"))));
        }
    }

    private void handleBooster(Player p, Tycoon t, int slot) {
        if (t == null) return;
        if (slot == 22) { openMainMenu(p); return; }
        if (t.hasActiveBooster()) { MessageUtil.sendRaw(p, "§6[MoneyTycoon] §cYou already have an active booster!"); return; }
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("boosters");
        if (sec == null) return;
        List<String> keys = new ArrayList<>(sec.getKeys(false));
        int[] slots = {11, 13, 15};
        for (int i = 0; i < keys.size() && i < slots.length; i++) {
            if (slot == slots[i]) {
                String key = keys.get(i);
                double cost = sec.getDouble(key + ".cost", 5000);
                if (!plugin.getEconomyManager().has(p.getUniqueId(), cost)) { MessageUtil.send(p, "not-enough-money", Map.of("cost", fmt(cost))); return; }
                plugin.getEconomyManager().withdraw(p.getUniqueId(), cost);
                t.setBooster(sec.getDouble(key + ".multiplier", 2.0), sec.getInt(key + ".duration", 300));
                plugin.getDatabaseManager().saveTycoon(t);
                playSoundIfEnabled(p, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
                MessageUtil.send(p, "booster-activated", Map.of("booster", cfg("boosters." + key + ".display-name"), "duration", String.valueOf(sec.getInt(key + ".duration"))));
                p.closeInventory();
                return;
            }
        }
    }

    private void handlePrestigeConfirm(Player p, Tycoon t, int slot) {
        if (t == null) return;
        if (slot == 11) {
            if (plugin.getTycoonManager().performPrestige(p, t)) {
                playSoundIfEnabled(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                MessageUtil.send(p, "prestige-complete", Map.of("level", ""+t.getPrestigeLevel(), "multiplier", String.format("%.1f", t.getPrestigeMultiplier())));
            } else MessageUtil.send(p, "not-enough-money", Map.of("cost", "?"));
            p.closeInventory();
        } else if (slot == 15) openMainMenu(p);
    }

    private void handleRebirthConfirm(Player p, Tycoon t, int slot) {
        if (t == null) return;
        if (slot == 11) {
            if (plugin.getTycoonManager().performRebirth(p, t)) {
                playSoundIfEnabled(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 0.7f);
                MessageUtil.send(p, "rebirth-complete", Map.of("level", ""+t.getRebirthLevel(), "multiplier", String.format("%.1f", t.getRebirthMultiplier())));
            } else MessageUtil.sendRaw(p, "§6[MoneyTycoon] §cRebirth requirements not met!");
            p.closeInventory();
        } else if (slot == 15) openMainMenu(p);
    }

    private void handleSettings(Player p, int slot) {
        if (slot == 22) { openMainMenu(p); return; }
        var sm = plugin.getSettingsManager();
        UUID uid = p.getUniqueId();
        boolean changed = false;
        switch (slot) {
            case 10 -> { sm.setSounds(uid, !sm.isSoundsEnabled(uid)); changed = true; }
            case 11 -> { sm.setTradeRequests(uid, !sm.isTradeRequestsEnabled(uid)); changed = true; }
            case 12 -> { sm.setVisitsAllowed(uid, !sm.isVisitsAllowed(uid)); changed = true; }
            case 13 -> { sm.setActionbar(uid, !sm.isActionbarEnabled(uid)); changed = true; }
            case 14 -> { sm.setBossbar(uid, !sm.isBossbarEnabled(uid)); changed = true; }
            case 15 -> { sm.setNotifications(uid, !sm.isNotificationsEnabled(uid)); changed = true; }
        }
        if (changed) {
            playSoundIfEnabled(p, Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            openSettingsMenu(p);
        }
    }

    private void handleIslandType(Player p, int slot) {
        if (slot == 31) { p.closeInventory(); return; }
        int idx = slot < 17 ? slot - 10 : (slot >= 19 && slot <= 25 ? slot - 12 : -1);
        if (idx < 0) return;
        List<IslandType> types = plugin.getTycoonManager().getAvailableIslandTypes();
        if (idx >= types.size()) return;
        IslandType it = types.get(idx);
        if (it.vip() && !p.hasPermission("moneytycoon.vip")) {
            MessageUtil.sendRaw(p, "§6[MoneyTycoon] §cThis island type requires VIP!");
            return;
        }
        if (it.cost() > 0 && !plugin.getEconomyManager().has(p.getUniqueId(), it.cost())) {
            MessageUtil.send(p, "not-enough-money", Map.of("cost", fmt(it.cost())));
            return;
        }
        if (it.cost() > 0) plugin.getEconomyManager().withdraw(p.getUniqueId(), it.cost());
        Tycoon t = plugin.getTycoonManager().createTycoon(p, it.id());
        if (t != null) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            MessageUtil.send(p, "tycoon-created", Map.of());
            plugin.getQuestManager().loadPlayer(p.getUniqueId());
            p.closeInventory();
        }
    }

    private void handleCoop(Player p, Tycoon t, int slot) {
        if (t == null) return;
        if (slot == 31) { openMainMenu(p); return; }
        if (slot >= 19 && slot < 19 + t.getCoopMembers().size()) {
            List<UUID> members = new ArrayList<>(t.getCoopMembers());
            int idx = slot - 19;
            if (idx < members.size()) {
                UUID mid = members.get(idx);
                t.removeCoopMember(mid);
                plugin.getDatabaseManager().removeCoopMember(t.getOwnerUUID(), mid);
                String name = Bukkit.getOfflinePlayer(mid).getName();
                MessageUtil.send(p, "coop-kicked", Map.of("player", name != null ? name : "?"));
                openCoopMenu(p);
            }
        }
    }

    public void openShopMenu(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("shop"), 45, L.deserialize("§6§l🏪 Server Shop"));

        inv.setItem(4, item(Material.EMERALD, "§6§lServer Shop",
                "§7Buy special items!", "§7Wallet: §e$" + fmt(plugin.getEconomyManager().getBalance(p.getUniqueId()))));

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shop.items");
        if (sec != null) {
            int slot = 19;
            for (String key : sec.getKeys(false)) {
                Material mat;
                try { mat = Material.valueOf(sec.getString(key + ".material", "STONE")); }
                catch (Exception e) { mat = Material.STONE; }
                double cost = sec.getDouble(key + ".cost", 1000);
                boolean vip = sec.getBoolean(key + ".vip", false);
                inv.setItem(slot, item(mat,
                        sec.getString(key + ".display-name", key).replace("&", "§"),
                        "§8§m──────────────",
                        sec.getString(key + ".description", "").replace("&", "§"),
                        "", vip ? "§d§l★ VIP ONLY" : "",
                        "§7Cost: §e$" + fmt(cost), "§a▸ Click to buy!"));
                slot++;
                if (slot == 26) slot = 28;
                if (slot == 35) break;
            }
        }
        inv.setItem(40, item(Material.ARROW, "§7← Back to Menu", ""));
        gradientBorder(inv, GRADIENT_GREEN);
        p.openInventory(inv);
    }

    public void openTradeMenu(Player p, dev.xdev.moneytycoon.manager.TradeManager.TradeSession session) {
        UUID myId = p.getUniqueId();
        UUID theirId = session.getOther(myId);
        String theirName = Bukkit.getOfflinePlayer(theirId).getName();
        Inventory inv = Bukkit.createInventory(new H("trade"), 36,
                L.deserialize("§e§l⇄ Trade: " + (theirName != null ? theirName : "?")));

        inv.setItem(0, item(Material.PLAYER_HEAD, "§a§lYour Offer",
                "§7Money: §e$" + fmt(session.getMyMoney(myId)),
                "§7Pet: §d" + (session.getMyPet(myId) != null ? session.getMyPet(myId) : "None")));
        inv.setItem(8, item(Material.PLAYER_HEAD, "§c§lTheir Offer",
                "§7Money: §e$" + fmt(session.getTheirMoney(myId)),
                "§7Pet: §d" + (session.getTheirPet(myId) != null ? session.getTheirPet(myId) : "None")));

        inv.setItem(10, item(Material.GOLD_INGOT, "§e+ Add Money",
                "§7Click to type amount in chat", "§7Current: §e$" + fmt(session.getMyMoney(myId))));
        inv.setItem(11, item(Material.NAME_TAG, "§d+ Add Pet",
                "§7Click to select a pet", "§7Current: §d" + (session.getMyPet(myId) != null ? session.getMyPet(myId) : "None")));
        if (session.getMyPet(myId) != null)
            inv.setItem(12, item(Material.BARRIER, "§c- Remove Pet", "§7Remove your pet from trade"));

        ItemStack div = item(Material.IRON_BARS, "§8§l|", "");
        inv.setItem(4, div); inv.setItem(13, div); inv.setItem(22, div);

        inv.setItem(14, item(session.getTheirMoney(myId) > 0 ? Material.GOLD_BLOCK : Material.LIGHT_GRAY_DYE,
                "§7Their Money: §e$" + fmt(session.getTheirMoney(myId)), ""));
        inv.setItem(15, item(session.getTheirPet(myId) != null ? Material.SPAWNER : Material.LIGHT_GRAY_DYE,
                "§7Their Pet: §d" + (session.getTheirPet(myId) != null ? session.getTheirPet(myId) : "None"), ""));

        boolean myConf = session.isMyConfirm(myId);
        boolean theirConf = session.isTheirConfirm(myId);
        inv.setItem(27, item(myConf ? Material.LIME_WOOL : Material.GREEN_WOOL,
                myConf ? "§a§l✓ YOU CONFIRMED" : "§a§lConfirm Trade",
                myConf ? "§7Waiting for other player..." : "§7Click to confirm the trade",
                "", "§c§oChanging offers resets confirms!"));
        inv.setItem(31, item(Material.RED_WOOL, "§c§l✗ Cancel Trade", "§7Cancel and close"));
        inv.setItem(35, item(theirConf ? Material.LIME_DYE : Material.GRAY_DYE,
                theirConf ? "§a✓ They Confirmed" : "§c✗ Not Confirmed", ""));

        inv.setItem(3, item(Material.SHIELD, "§b§lTrade Protection",
                "§7 ✓ Both must confirm",
                "§7 ✓ Balance verified on complete",
                "§7 ✓ Pet ownership verified",
                "§7 ✓ All trades are logged",
                "§7 ✓ Auto-cancel on disconnect",
                "§7 ✓ 2 min timeout"));

        for (int i = 0; i < 36; i++) if (inv.getItem(i) == null) inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, " "));
        p.openInventory(inv);
    }

    public void openTradePetSelect(Player p) {
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        Inventory inv = Bukkit.createInventory(new H("trade-pets"), 27, L.deserialize("§d§l Select Pet to Trade"));
        int slot = 10;
        for (String petId : t.getOwnedPets()) {
            inv.setItem(slot, item(Material.EGG, "§d" + petId,
                    "§7Click to add this pet to trade"));
            slot++;
            if (slot > 16) break;
        }
        if (t.getOwnedPets().isEmpty())
            inv.setItem(13, item(Material.BARRIER, "§cNo Pets Owned", "§7Buy pets first!"));
        inv.setItem(22, item(Material.ARROW, "§7← Back to Trade", ""));
        gradientBorder(inv, GRADIENT_PURPLE);
        p.openInventory(inv);
    }

    private void handleShop(Player p, Tycoon t, int slot, ItemStack cl) {
        if (t == null) return;
        if (slot == 40) { openMainMenu(p); return; }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("shop.items");
        if (sec == null) return;
        List<String> keys = new ArrayList<>(sec.getKeys(false));
        int idx = slot >= 28 ? slot - 28 + 7 : slot - 19;
        if (idx < 0 || idx >= keys.size()) return;

        String key = keys.get(idx);
        double cost = sec.getDouble(key + ".cost", 1000);
        boolean vip = sec.getBoolean(key + ".vip", false);
        String type = sec.getString(key + ".type", "");

        if (vip && !p.hasPermission("moneytycoon.vip")) {
            MessageUtil.sendRaw(p, "§6[Shop] §cThis item is VIP only!"); return;
        }
        if (!plugin.getEconomyManager().has(p.getUniqueId(), cost)) {
            MessageUtil.send(p, "not-enough-money", Map.of("cost", fmt(cost))); return;
        }

        plugin.getEconomyManager().withdraw(p.getUniqueId(), cost);
        String displayName = sec.getString(key + ".display-name", key).replace("&", "§");

        switch (type.toUpperCase()) {
            case "BOOSTER" -> {
                double mult = sec.getDouble(key + ".multiplier", 2.0);
                int dur = sec.getInt(key + ".duration", 60);
                t.setBooster(mult, dur);
                plugin.getDatabaseManager().saveTycoon(t);
            }
            case "INSTANT_COLLECT" -> {
                if (t.getBalance() > 0) {
                    double bal = t.getBalance();
                    plugin.getTycoonManager().collectBalance(p, t);
                    MessageUtil.sendRaw(p, "§6[Shop] §aInstantly collected §e$" + fmt(bal) + "§a!");
                }
            }
            case "LUCKY_TOKEN" -> {
                int amount = sec.getInt(key + ".value", 50);
                int current = t.getUpgradeLevel("lucky-tokens");
                t.setUpgradeLevel("lucky-tokens", current + amount);
                plugin.getDatabaseManager().saveTycoon(t);
            }
            case "MYSTERY_BOX" -> {
                double rand = Math.random();
                if (rand < 0.3) {
                    String[] pets = {"COLLECTOR", "SPEED", "VALUE", "LUCKY"};
                    String pet = pets[(int) (Math.random() * pets.length)];
                    if (!t.getOwnedPets().contains(pet)) {
                        t.getOwnedPets().add(pet);
                        plugin.getDatabaseManager().saveOwnedPet(p.getUniqueId(), pet);
                        MessageUtil.sendRaw(p, "§6[Shop] §d§l★ Mystery Box gave you pet: §e" + pet + "§d!");
                    } else {
                        plugin.getEconomyManager().deposit(p.getUniqueId(), 10000);
                        MessageUtil.sendRaw(p, "§6[Shop] §a★ Mystery Box gave you §e$10,000§a! (pet duplicate)");
                    }
                } else {
                    double bonus = 5000 + Math.random() * 15000;
                    plugin.getEconomyManager().deposit(p.getUniqueId(), bonus);
                    MessageUtil.sendRaw(p, "§6[Shop] §a★ Mystery Box gave you §e$" + fmt(bonus) + "§a!");
                }
                plugin.getDatabaseManager().saveTycoon(t);
            }
        }
        playSoundIfEnabled(p, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
        MessageUtil.sendRaw(p, "§6[Shop] §aPurchased §e" + displayName + " §afor §e$" + fmt(cost) + "§a!");
        openShopMenu(p);
    }

    private void handleTrade(Player p, int slot) {
        var session = plugin.getTradeManager().getSession(p.getUniqueId());
        if (session == null) { p.closeInventory(); return; }

        switch (slot) {
            case 10 -> {
                p.closeInventory();
                MessageUtil.sendRaw(p, "§6[Trade] §eType the amount of money to offer in chat:");
                MessageUtil.sendRaw(p, "§7 Type §ecancel §7to go back.");
                plugin.getTradeManager().pendingMoneyInput.put(p.getUniqueId(), true);
            }
            case 11 -> openTradePetSelect(p);
            case 12 -> {
                plugin.getTradeManager().removePet(p);
            }
            case 27 -> plugin.getTradeManager().confirmTrade(p);
            case 31 -> plugin.getTradeManager().cancelTrade(p.getUniqueId(), "Cancelled by " + p.getName());
        }
    }

    private void handleTradePetSelect(Player p, int slot) {
        if (slot == 22) {
            var session = plugin.getTradeManager().getSession(p.getUniqueId());
            if (session != null) openTradeMenu(p, session);
            return;
        }
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        List<String> pets = new ArrayList<>(t.getOwnedPets());
        int idx = slot - 10;
        if (idx < 0 || idx >= pets.size()) return;
        plugin.getTradeManager().addPet(p, pets.get(idx));
    }

    @SuppressWarnings("deprecation")
    private ItemStack item(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        if (meta == null) return i;
        meta.setDisplayName(name);
        if (lore.length > 0) { List<String> l = new ArrayList<>(); for (String s : lore) if (s != null) l.add(s); meta.setLore(l); }
        i.setItemMeta(meta);
        return i;
    }

    private void fillBorder(Inventory inv, Material glass) {
        ItemStack g = item(glass, " ");
        int size = inv.getSize();
        for (int i = 0; i < 9; i++) { if (inv.getItem(i) == null) inv.setItem(i, g); }
        for (int i = size - 9; i < size; i++) { if (inv.getItem(i) == null) inv.setItem(i, g); }
        for (int i = 9; i < size - 9; i += 9) { if (inv.getItem(i) == null) inv.setItem(i, g); }
        for (int i = 17; i < size - 9; i += 9) { if (inv.getItem(i) == null) inv.setItem(i, g); }
    }

    private boolean isGlass(Material m) { return m.name().endsWith("STAINED_GLASS_PANE"); }

    private void playSoundIfEnabled(Player p, Sound s, float vol, float pitch) {
        if (plugin.getSettingsManager().isSoundsEnabled(p.getUniqueId()))
            p.playSound(p.getLocation(), s, vol, pitch);
    }

    @SuppressWarnings("deprecation")
    private ItemStack glow(ItemStack item) {
        if (item == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private void gradientBorder(Inventory inv, Material[] gradient) {
        int size = inv.getSize();
        int idx = 0;
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, item(gradient[idx++ % gradient.length], " "));
        }
        for (int i = size - 9; i < size; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, item(gradient[idx++ % gradient.length], " "));
        }
        for (int i = 9; i < size - 9; i += 9) {
            if (inv.getItem(i) == null) inv.setItem(i, item(gradient[idx++ % gradient.length], " "));
        }
        for (int i = 17; i < size - 9; i += 9) {
            if (inv.getItem(i) == null) inv.setItem(i, item(gradient[idx++ % gradient.length], " "));
        }
    }

    private String buildProgressBar(int current, int max) {
        int filled = max > 0 ? (int) ((double) current / max * 20) : 0;
        filled = Math.min(filled, 20);
        return "§a" + "▌".repeat(filled) + "§7" + "▌".repeat(20 - filled) + " §f" + current + "/" + max;
    }

    private String cfg(String path) { return plugin.getConfig().getString(path, "?").replace("&", "§"); }

    private String fmt(double a) {
        if (a >= 1_000_000_000) return String.format("%.1fB", a / 1_000_000_000);
        if (a >= 1_000_000) return String.format("%.1fM", a / 1_000_000);
        if (a >= 1_000) return String.format("%.1fK", a / 1_000);
        return String.format("%.0f", a);
    }

    private String fmtInt(long a) {
        if (a >= 1_000_000) return String.format("%.1fM", a / 1_000_000.0);
        if (a >= 1_000) return String.format("%.1fK", a / 1_000.0);
        return String.valueOf(a);
    }
}
