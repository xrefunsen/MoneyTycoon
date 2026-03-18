package dev.xdev.moneytycoon.task;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.Tycoon;
import dev.xdev.moneytycoon.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;

import java.util.*;

public class TycoonTask {

    private final MoneyTycoon plugin;
    private BukkitTask task;
    private int tick = 0;
    private final java.util.Map<java.util.UUID, Long> lastSoundTime = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Long> lastPrestigeNotify = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Long> lastRebirthNotify = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, BossBar> playerBossBars = new java.util.HashMap<>();
    private static final long SOUND_COOLDOWN_MS = 500;
    private static final long PRESTIGE_NOTIFY_COOLDOWN_MS = 60000;
    private static final long REBIRTH_NOTIFY_COOLDOWN_MS = 60000;

    public TycoonTask(MoneyTycoon plugin) { this.plugin = plugin; }
    public void start() {
        int interval = Math.max(1, plugin.getConfig().getInt("performance.tick-interval", 2));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, interval);
    }
    public void stop() {
        if (task != null) task.cancel();
        for (BossBar bar : playerBossBars.values()) bar.removeAll();
        playerBossBars.clear();
    }

    private void tick() {
        tick++;
        int petInterval = plugin.getConfig().getInt("performance.pet-teleport-interval", 40);
        int uiInterval = plugin.getConfig().getInt("performance.ui-update-interval", 20);

        for (Tycoon t : plugin.getTycoonManager().getAllTycoons()) {
            if (!t.isActive()) continue;
            Player owner = Bukkit.getPlayer(t.getOwnerUUID());
            if (owner == null) continue;
            processTycoon(t, tick, petInterval, uiInterval);
        }
    }

    private void processTycoon(Tycoon t, int tick, int petInterval, int uiInterval) {
        Player owner = Bukkit.getPlayer(t.getOwnerUUID());
        if (owner == null) return;
        processDropper(t);
        processConveyor(t);
        if (tick % petInterval == 0) plugin.getPetManager().teleportPetToOwner(t);
        if (tick % uiInterval == 0) {
            updateScoreboard(owner, t);
            checkMilestones(owner, t);
            if (plugin.getSettingsManager().isNotificationsEnabled(owner.getUniqueId())) {
                checkPrestigeNotify(owner, t);
                checkRebirthNotify(owner, t);
            }
            if (plugin.getConfig().getBoolean("actionbar.enabled", true) && plugin.getSettingsManager().isActionbarEnabled(owner.getUniqueId()))
                updateActionbar(owner, t);
            if (plugin.getConfig().getBoolean("bossbar.enabled", true) && plugin.getSettingsManager().isBossbarEnabled(owner.getUniqueId()))
                updateBossbar(owner, t);
            plugin.getHologramManager().updateDynamic(t);
        }
    }

    private void processDropper(Tycoon t) {
        t.incrementDropperTick();
        int interval = plugin.getTycoonManager().getDropperInterval(t);
        if (t.getDropperTickCounter() < interval) return;
        t.resetDropperTick();
        int maxItems = plugin.getConfig().getInt("performance.max-items-per-tycoon", 80);
        if (t.getTrackedItems().size() >= maxItems) return;

        Location dl = t.getDropperLocation();
        if (dl == null || !dl.isWorldLoaded()) return;
        spawnItem(t, dl);

        int extra = t.getUpgradeLevel("extra-dropper");
        List<Location> extras = t.getExtraDropperLocations();
        if (extras != null)
            for (int i = 0; i < Math.min(extra, extras.size()); i++)
                spawnItem(t, extras.get(i));
    }

    private void spawnItem(Tycoon t, Location loc) {
        World w = loc.getWorld();
        if (w == null) return;

        boolean doubleDrop = false;
        double ddChance = plugin.getPetManager().getPetBonus(t, "DOUBLE_DROP");
        if (ddChance > 0 && Math.random() < ddChance) doubleDrop = true;

        Location sp = loc.clone().subtract(0, 1, 0);
        ItemStack is = new ItemStack(t.getCurrentDropperType(), doubleDrop ? 2 : 1);
        Item item = w.dropItem(sp, is);
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setVelocity(new Vector(0, -0.3, 0));
        t.getTrackedItems().put(item.getUniqueId(), -1);
        if (plugin.getConfig().getBoolean("performance.particles", true))
            w.spawnParticle(Particle.CRIT, sp, 3, 0.2, 0.2, 0.2, 0.02);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (item.isDead() || !item.isValid()) { t.getTrackedItems().remove(item.getUniqueId()); return; }
            t.getTrackedItems().put(item.getUniqueId(), 0);
            List<Location> path = t.getConveyorPath();
            if (!path.isEmpty()) {
                item.teleport(path.get(0).clone());
                item.setVelocity(new Vector(0, 0, 0));
                item.setGravity(false);
            }
        }, 15L);
    }

    private void processConveyor(Tycoon t) {
        Map<UUID, Integer> items = t.getTrackedItems();
        List<Location> path = t.getConveyorPath();
        if (path == null || path.isEmpty()) return;

        int spd = t.getUpgradeLevel("conveyor-speed");
        double baseSpeed = 0.45 + spd * 0.15;

        List<UUID> remove = new ArrayList<>();
        Map<UUID, Integer> update = new HashMap<>();

        for (var entry : items.entrySet()) {
            if (entry.getValue() < 0) continue;
            Entity e = Bukkit.getEntity(entry.getKey());
            if (e == null || e.isDead() || !e.isValid()) { remove.add(entry.getKey()); continue; }

            int idx = entry.getValue();
            Location target = path.get(idx);
            Location pos = e.getLocation();

            double dx = target.getX() - pos.getX();
            double dy = target.getY() - pos.getY();
            double dz = target.getZ() - pos.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (idx >= path.size() - 1 && dist < 0.4) {
                e.remove();
                int count = (e instanceof Item itm) ? itm.getItemStack().getAmount() : 1;
                double value = plugin.getTycoonManager().getDropperValue(t) * count;

                boolean autoCollect = t.getUpgradeLevel("auto-collect") > 0
                        || plugin.getPetManager().hasAutoCollectPet(t)
                        || (Bukkit.getPlayer(t.getOwnerUUID()) != null
                        && Bukkit.getPlayer(t.getOwnerUUID()).hasPermission("moneytycoon.vip")
                        && plugin.getConfig().getBoolean("vip.auto-collect-free", true));

                if (autoCollect) {
                    plugin.getEconomyManager().deposit(t.getOwnerUUID(), value);
                    t.setTotalEarned(t.getTotalEarned() + (long) value);
                } else {
                    t.addBalance(value);
                }

                t.incrementItemsSold();
                plugin.getQuestManager().incrementProgress(t.getOwnerUUID(), "SELL_ITEMS", count);
                plugin.getQuestManager().incrementProgress(t.getOwnerUUID(), "EARN_MONEY", (int) value);

                Location sl = t.getSellLocation();
                if (sl != null && sl.getWorld() != null) {
                    if (plugin.getConfig().getBoolean("performance.particles", true))
                        sl.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, sl, 5, 0.3, 0.3, 0.3, 0);
                    long now = System.currentTimeMillis();
                    if (now - lastSoundTime.getOrDefault(t.getOwnerUUID(), 0L) >= SOUND_COOLDOWN_MS
                            && plugin.getSettingsManager().isSoundsEnabled(t.getOwnerUUID())) {
                        lastSoundTime.put(t.getOwnerUUID(), now);
                        sl.getWorld().playSound(sl, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.5f);
                    }
                }
                remove.add(entry.getKey());
            } else if (dist < 0.4 && idx < path.size() - 1) {
                update.put(entry.getKey(), idx + 1);
            } else if (idx < path.size() - 1) {
                double vx = (dist > 0.001) ? (dx / dist) * baseSpeed : 0;
                double vy = (dist > 0.001) ? (dy / dist) * baseSpeed : 0;
                double vz = (dist > 0.001) ? (dz / dist) * baseSpeed : 0;
                e.setVelocity(new Vector(vx, vy, vz));
            }
        }
        remove.forEach(items::remove);
        items.putAll(update);
    }

    private void checkPrestigeNotify(Player p, Tycoon t) {
        if (!plugin.getConfig().getBoolean("prestige.enabled", true)) return;
        long now = System.currentTimeMillis();
        if (now - lastPrestigeNotify.getOrDefault(p.getUniqueId(), 0L) < PRESTIGE_NOTIFY_COOLDOWN_MS) return;

        double cost = plugin.getTycoonManager().getPrestigeCost(t);
        if (!plugin.getEconomyManager().has(p.getUniqueId(), cost)) return;

        lastPrestigeNotify.put(p.getUniqueId(), now);
        MessageUtil.send(p, "prestige-available", Map.of("cost", String.format("%.0f", cost)));
    }

    private void checkRebirthNotify(Player p, Tycoon t) {
        if (!plugin.getConfig().getBoolean("rebirth.enabled", true)) return;
        int reqPrestige = plugin.getConfig().getInt("rebirth.prestige-requirement", 10);
        if (t.getPrestigeLevel() < reqPrestige) return;
        long now = System.currentTimeMillis();
        if (now - lastRebirthNotify.getOrDefault(p.getUniqueId(), 0L) < REBIRTH_NOTIFY_COOLDOWN_MS) return;

        double cost = plugin.getTycoonManager().getRebirthCost(t);
        if (!plugin.getEconomyManager().has(p.getUniqueId(), cost)) return;

        lastRebirthNotify.put(p.getUniqueId(), now);
        MessageUtil.send(p, "rebirth-available", Map.of("cost", String.format("%.0f", cost)));
    }

    private void updateBossbar(Player p, Tycoon t) {
        if (!plugin.getConfig().getBoolean("bossbar.show-on-prestige-progress", true)) return;
        double cost = plugin.getTycoonManager().getPrestigeCost(t);
        double wallet = plugin.getEconomyManager().getBalance(p.getUniqueId());
        if (wallet >= cost) {
            removeBossbar(p);
            return;
        }
        if (cost <= 0) return;
        double progress = Math.min(1.0, wallet / cost);
        String format = plugin.getConfig().getString("bossbar.format", "&6Prestige Progress: &e$%current% &7/ &a$%target%");
        final String title = format.replace("%current%", fmt(wallet)).replace("%target%", fmt(cost)).replace("&", "§");
        BossBar bar = playerBossBars.computeIfAbsent(p.getUniqueId(), u -> Bukkit.createBossBar(title, BarColor.YELLOW, BarStyle.SEGMENTED_10));
        bar.setTitle(title);
        bar.setProgress(progress);
        bar.addPlayer(p);
    }

    public void removeBossbar(Player p) {
        BossBar bar = playerBossBars.remove(p.getUniqueId());
        if (bar != null) bar.removePlayer(p);
    }

    private void updateActionbar(Player p, Tycoon t) {
        String format = plugin.getConfig().getString("actionbar.format", "&6Balance: &e$%balance% &7| &aWallet: &e$%wallet%");
        format = format.replace("%balance%", fmt(t.getBalance()))
                .replace("%wallet%", fmt(plugin.getEconomyManager().getBalance(p.getUniqueId())))
                .replace("&", "§");
        p.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(format));
    }

    private void checkMilestones(Player p, Tycoon t) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("milestones");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            long threshold = Long.parseLong(key);
            if (t.getTotalEarned() >= threshold && t.getLastMilestone() < threshold) {
                t.setLastMilestone(threshold);
                double reward = sec.getDouble(key + ".reward", 0);
                String msg = sec.getString(key + ".message", "&aMilestone reached!").replace("&", "§");
                if (reward > 0) plugin.getEconomyManager().deposit(p.getUniqueId(), reward);
                MessageUtil.sendRaw(p, "§6[MoneyTycoon] " + msg);
                if (plugin.getSettingsManager().isSoundsEnabled(p.getUniqueId()))
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void updateScoreboard(Player p, Tycoon t) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("mt", Criteria.DUMMY,
                Component.text("⚡ MoneyTycoon ⚡").color(NamedTextColor.GOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        String boosterLine = t.hasActiveBooster()
                ? "§a Booster: §e" + String.format("%.0f", t.getBoosterMultiplier()) + "x §7(" + t.getBoosterSecondsLeft() + "s)"
                : "§7 Booster: None";

        String[] lines = {
                "§8§m                    ",
                "§6 Balance: §e$" + fmt(t.getBalance()),
                "§a Wallet: §e$" + fmt(plugin.getEconomyManager().getBalance(p.getUniqueId())),
                "§d Total: §e$" + fmt(t.getTotalEarned()),
                " ",
                "§b Dropper: §f" + dropperName(t),
                "§e Prestige: §6" + t.getPrestigeLevel() + " §7(" + String.format("%.1f", t.getPrestigeMultiplier()) + "x)",
                "§5 Rebirth: §d" + t.getRebirthLevel() + " §7(" + String.format("%.1f", t.getRebirthMultiplier()) + "x)",
                boosterLine,
                "§8§m                     "
        };
        for (int i = 0; i < lines.length; i++) obj.getScore(lines[i]).setScore(lines.length - i);
        p.setScoreboard(sb);
    }

    private String dropperName(Tycoon t) {
        return plugin.getConfig().getString("dropper.types." + t.getCurrentDropperType().name() + ".display-name",
                t.getCurrentDropperType().name()).replace("&", "§");
    }

    private String fmt(double a) {
        if (a >= 1_000_000) return String.format("%.1fM", a / 1_000_000);
        if (a >= 1_000) return String.format("%.1fK", a / 1_000);
        return String.format("%.0f", a);
    }
}
