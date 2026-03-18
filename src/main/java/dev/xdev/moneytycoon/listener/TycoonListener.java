package dev.xdev.moneytycoon.listener;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.gui.GUIManager;
import dev.xdev.moneytycoon.model.Tycoon;
import dev.xdev.moneytycoon.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.configuration.ConfigurationSection;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class TycoonListener implements Listener {

    private final MoneyTycoon plugin;
    public TycoonListener(MoneyTycoon plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t != null) {
            t.setOwnerName(p.getName());
            t.setActive(true);
            plugin.getQuestManager().loadPlayer(p.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) {
                    plugin.getTycoonManager().restoreConveyorItems(t);
                    if (t.getActivePetId() != null) plugin.getPetManager().spawnPet(p, t);
                }
            }, 40L);
        }
        handleDailyBonus(p);
        handleOfflineEarnings(p, t);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.getTradeManager().handleDisconnect(p.getUniqueId());
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t != null) {
            t.setActive(false);
            plugin.getPetManager().despawnPet(t);
            plugin.getTycoonManager().saveConveyorAndCleanup(t);
            plugin.getDatabaseManager().saveTycoon(t);
            plugin.getDatabaseManager().setLastOffline(p.getUniqueId(), System.currentTimeMillis());
            plugin.getQuestManager().unloadPlayer(p.getUniqueId());
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            if (plugin.getTycoonTask() != null) plugin.getTycoonTask().removeBossbar(p);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getTradeManager().pendingMoneyInput.containsKey(p.getUniqueId())) return;

        e.setCancelled(true);
        plugin.getTradeManager().pendingMoneyInput.remove(p.getUniqueId());

        String msg = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        if (msg.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                var session = plugin.getTradeManager().getSession(p.getUniqueId());
                if (session != null) plugin.getGuiManager().openTradeMenu(p, session);
                else MessageUtil.sendRaw(p, "§6[Trade] §cTrade session expired.");
            });
            return;
        }

        try {
            double amount = Double.parseDouble(msg.replace(",", "").replace("$", ""));
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getTradeManager().addMoney(p, amount));
        } catch (NumberFormatException ex) {
            MessageUtil.sendRaw(p, "§6[Trade] §cInvalid amount! Try again or type §ecancel§c.");
            plugin.getTradeManager().pendingMoneyInput.put(p.getUniqueId(), true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        if (e.getPlayer().hasPermission("moneytycoon.admin")) return;
        Tycoon t = plugin.getTycoonManager().getTycoonAt(e.getBlock().getLocation());
        if (t != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        if (e.getPlayer().hasPermission("moneytycoon.admin")) return;
        Tycoon t = plugin.getTycoonManager().getTycoonAt(e.getBlock().getLocation());
        if (t != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.getTycoonManager().getTycoonAt(b.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(b -> plugin.getTycoonManager().getTycoonAt(b.getLocation()) != null);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Item item = e.getItem();
        for (Tycoon t : plugin.getTycoonManager().getAllTycoons())
            if (t.getTrackedItems().containsKey(item.getUniqueId())) { e.setCancelled(true); return; }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof GUIManager.H)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != null && e.getClickedInventory().getHolder() instanceof GUIManager.H)
            plugin.getGuiManager().handleClick(e);
    }

    @EventHandler
    public void onInvDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof GUIManager.H) e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        if (b == null) return;

        Tycoon any = plugin.getTycoonManager().getTycoonAt(b.getLocation());
        if (any == null) return;

        Player p = e.getPlayer();
        Tycoon own = plugin.getTycoonManager().getTycoon(p.getUniqueId());

        if (b.getType() == Material.DROPPER || b.getType() == Material.HOPPER) {
            if (!p.hasPermission("moneytycoon.admin")) e.setCancelled(true);
            return;
        }

        if (own != null && any.getOwnerUUID().equals(p.getUniqueId())) {
            if (b.getType() == Material.CHEST) {
                e.setCancelled(true);
                if (own.getBalance() > 0) {
                    double a = own.getBalance();
                    plugin.getTycoonManager().collectBalance(p, own);
                    MessageUtil.sendRaw(p, "§6[MoneyTycoon] §a$" + String.format("%.2f", a) + " §acollected!");
                } else MessageUtil.sendRaw(p, "§6[MoneyTycoon] §cNo money to collect yet!");
            }
            if (b.getType() == Material.OBSIDIAN && plugin.getTycoonManager().isSellBlock(b.getLocation(), own)) {
                e.setCancelled(true);
                MessageUtil.sendRaw(p, "§6[MoneyTycoon] §aAuto-collect active! Money goes to wallet.");
            }
            if (b.getType() == Material.EMERALD_BLOCK) { e.setCancelled(true); plugin.getGuiManager().openUpgradeMenu(p); }
            if (b.getType() == Material.BOOKSHELF || b.getType() == Material.LECTERN) { e.setCancelled(true); plugin.getGuiManager().openQuestMenu(p); }
        } else {
            if (b.getType() == Material.CHEST || b.getType() == Material.LECTERN) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (plugin.getTycoonManager().getTycoonAt(p.getLocation()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (plugin.getTycoonManager().getTycoonAt(p.getLocation()) != null) { e.setCancelled(true); e.setFoodLevel(20); }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Tycoon t = plugin.getTycoonManager().getTycoon(e.getPlayer().getUniqueId());
        if (t == null) return;
        Location base = t.getBaseLocation();
        if (base == null || base.getWorld() == null) return;
        var it = plugin.getTycoonManager().getIslandType(t.getIslandType());
        Location spawn = base.clone().add(it.spawnX() + .5, 1, it.spawnZ() + .5);
        e.setRespawnLocation(spawn);
    }

    @EventHandler
    public void onVoidFall(PlayerMoveEvent e) {
        if (e.getTo() == null || e.getFrom().getBlockY() == e.getTo().getBlockY()) return;
        if (e.getTo().getY() >= 0) return;
        Player p = e.getPlayer();
        Tycoon t = plugin.getTycoonManager().getTycoon(p.getUniqueId());
        if (t == null) return;
        World tycoonWorld = plugin.getWorldManager().getTycoonWorld();
        if (tycoonWorld == null || !tycoonWorld.equals(e.getTo().getWorld())) return;
        Location base = t.getBaseLocation();
        if (base == null || base.getWorld() == null) return;
        var it = plugin.getTycoonManager().getIslandType(t.getIslandType());
        Location safe = base.clone().add(it.spawnX() + .5, 1, it.spawnZ() + .5);
        safe.setYaw(p.getLocation().getYaw());
        safe.setPitch(p.getLocation().getPitch());
        Bukkit.getScheduler().runTask(plugin, () -> p.teleport(safe));
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Tycoon t = plugin.getTycoonManager().getTycoon(e.getPlayer().getUniqueId());
        if (t == null) return;
        boolean wasIn = plugin.getTycoonManager().isInTycoon(e.getFrom(), t);
        boolean willBe = plugin.getTycoonManager().isInTycoon(e.getTo(), t);
        if (!wasIn && willBe && t.getActivePetId() != null)
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getPetManager().spawnPet(e.getPlayer(), t), 20L);
        if (wasIn && !willBe) plugin.getPetManager().despawnPet(t);
    }

    private void handleDailyBonus(Player p) {
        if (!plugin.getConfig().getBoolean("daily-bonus.enabled", true)) return;
        ConfigurationSection rewards = plugin.getConfig().getConfigurationSection("daily-bonus.rewards");
        if (rewards == null) return;
        int maxStreak = plugin.getConfig().getInt("daily-bonus.max-streak", 7);
        boolean resetIfMissed = plugin.getConfig().getBoolean("daily-bonus.reset-if-missed", true);

        long now = System.currentTimeMillis();
        int[] data = plugin.getDatabaseManager().getLoginData(p.getUniqueId());
        long lastLogin = data[0];
        int streak = data[1];

        long dayMs = 24 * 60 * 60 * 1000L;
        int newStreak;
        if (lastLogin == 0) {
            newStreak = 1;
        } else {
            long diff = now - lastLogin;
            if (diff < dayMs / 2) return;
            if (diff < dayMs * 2) newStreak = Math.min(streak + 1, maxStreak);
            else if (resetIfMissed) newStreak = 1;
            else newStreak = streak;
        }

        int reward = rewards.getInt(String.valueOf(newStreak), 0);
        if (reward > 0) {
            plugin.getEconomyManager().deposit(p.getUniqueId(), reward);
            MessageUtil.send(p, "daily-bonus", Map.of("day", String.valueOf(newStreak), "reward", String.valueOf(reward)));
        }
        plugin.getDatabaseManager().saveLoginData(p.getUniqueId(), now, newStreak);
    }

    private void handleOfflineEarnings(Player p, Tycoon t) {
        if (t == null || !plugin.getConfig().getBoolean("offline-earnings.enabled", true)) return;
        long lastOff = plugin.getDatabaseManager().getLastOffline(p.getUniqueId());
        if (lastOff == 0) return;
        long now = System.currentTimeMillis();
        long diffMs = now - lastOff;
        int maxHours = plugin.getConfig().getInt("offline-earnings.max-hours", 24);
        double ratePerHour = plugin.getConfig().getDouble("offline-earnings.rate-per-hour", 0.05);
        int minMinutes = plugin.getConfig().getInt("offline-earnings.min-interval-minutes", 5);
        if (diffMs < minMinutes * 60 * 1000L) return;
        double hours = Math.min(diffMs / (3600.0 * 1000), maxHours);
        double earned = (t.getTotalEarned() / 1000.0) * ratePerHour * hours;
        if (earned > 0) {
            earned = Math.min(earned, t.getTotalEarned() * 0.1);
            plugin.getEconomyManager().deposit(p.getUniqueId(), earned);
            MessageUtil.sendRaw(p, "§6[MoneyTycoon] §aOffline earnings: §e$" + String.format("%.0f", earned));
        }
        plugin.getDatabaseManager().setLastOffline(p.getUniqueId(), now);
    }
}
