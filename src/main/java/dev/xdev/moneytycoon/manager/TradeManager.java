package dev.xdev.moneytycoon.manager;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.Tycoon;
import dev.xdev.moneytycoon.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final MoneyTycoon plugin;
    private final Map<UUID, TradeSession> activeTrades = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    public final Map<UUID, Boolean> pendingMoneyInput = new ConcurrentHashMap<>();

    private static final long REQUEST_TIMEOUT_MS = 30_000;
    private static final long TRADE_TIMEOUT_MS = 120_000;

    public TradeManager(MoneyTycoon plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpired, 100L, 100L);
    }

    public static class TradeSession {
        public final UUID player1, player2;
        public double moneyOffer1, moneyOffer2;
        public String petOffer1, petOffer2;
        public boolean confirmed1, confirmed2;
        public final long createdAt;

        public TradeSession(UUID p1, UUID p2) {
            this.player1 = p1;
            this.player2 = p2;
            this.createdAt = System.currentTimeMillis();
        }

        public UUID getOther(UUID self) { return self.equals(player1) ? player2 : player1; }
        public boolean isPlayer1(UUID uuid) { return uuid.equals(player1); }

        public double getMyMoney(UUID self) { return isPlayer1(self) ? moneyOffer1 : moneyOffer2; }
        public double getTheirMoney(UUID self) { return isPlayer1(self) ? moneyOffer2 : moneyOffer1; }
        public String getMyPet(UUID self) { return isPlayer1(self) ? petOffer1 : petOffer2; }
        public String getTheirPet(UUID self) { return isPlayer1(self) ? petOffer2 : petOffer1; }
        public boolean isMyConfirm(UUID self) { return isPlayer1(self) ? confirmed1 : confirmed2; }
        public boolean isTheirConfirm(UUID self) { return isPlayer1(self) ? confirmed2 : confirmed1; }

        public void setMyMoney(UUID self, double amount) {
            if (isPlayer1(self)) moneyOffer1 = amount; else moneyOffer2 = amount;
            resetConfirms();
        }
        public void setMyPet(UUID self, String pet) {
            if (isPlayer1(self)) petOffer1 = pet; else petOffer2 = pet;
            resetConfirms();
        }
        public void setMyConfirm(UUID self, boolean val) {
            if (isPlayer1(self)) confirmed1 = val; else confirmed2 = val;
        }
        public boolean bothConfirmed() { return confirmed1 && confirmed2; }
        private void resetConfirms() { confirmed1 = false; confirmed2 = false; }

        public boolean hasAnyOffer() {
            return moneyOffer1 > 0 || moneyOffer2 > 0 || petOffer1 != null || petOffer2 != null;
        }
    }

    public boolean sendRequest(Player sender, Player target) {
        UUID sid = sender.getUniqueId(), tid = target.getUniqueId();

        if (sid.equals(tid)) {
            MessageUtil.sendRaw(sender, "§6[Trade] §cYou can't trade with yourself!");
            return false;
        }
        long cd = plugin.getConfig().getLong("trade.cooldown", 30) * 1000;
        Long lastTrade = cooldowns.get(sid);
        if (lastTrade != null && System.currentTimeMillis() - lastTrade < cd) {
            int left = (int) ((cd - (System.currentTimeMillis() - lastTrade)) / 1000);
            MessageUtil.sendRaw(sender, "§6[Trade] §cCooldown! Wait §e" + left + "s§c.");
            return false;
        }
        if (activeTrades.containsKey(sid)) {
            MessageUtil.sendRaw(sender, "§6[Trade] §cYou already have an active trade!");
            return false;
        }
        if (activeTrades.containsKey(tid)) {
            MessageUtil.sendRaw(sender, "§6[Trade] §cThat player is already in a trade!");
            return false;
        }
        if (plugin.getTycoonManager().getTycoon(sid) == null) {
            MessageUtil.sendRaw(sender, "§6[Trade] §cYou need a tycoon to trade!");
            return false;
        }
        if (plugin.getTycoonManager().getTycoon(tid) == null) {
            MessageUtil.sendRaw(sender, "§6[Trade] §cThat player doesn't have a tycoon!");
            return false;
        }
        if (!plugin.getSettingsManager().isTradeRequestsEnabled(tid)) {
            MessageUtil.sendRaw(sender, "§6[Trade] §cThat player has disabled trade requests!");
            return false;
        }

        pendingRequests.put(tid, sid);

        MessageUtil.sendRaw(sender, "§6[Trade] §aTrade request sent to §e" + target.getName() + "§a! (30s)");
        MessageUtil.sendRaw(target, "§8§m──────────────────────────────");
        MessageUtil.sendRaw(target, "§6[Trade] §e" + sender.getName() + " §awants to trade with you!");
        MessageUtil.sendRaw(target, "§a  /tycoon trade accept §7- Accept");
        MessageUtil.sendRaw(target, "§c  /tycoon trade deny §7- Deny");
        MessageUtil.sendRaw(target, "§7  Expires in 30 seconds.");
        MessageUtil.sendRaw(target, "§8§m──────────────────────────────");
        if (plugin.getSettingsManager().isSoundsEnabled(tid))
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.containsKey(tid) && pendingRequests.get(tid).equals(sid)) {
                pendingRequests.remove(tid);
                Player s = Bukkit.getPlayer(sid);
                if (s != null) MessageUtil.sendRaw(s, "§6[Trade] §cTrade request to §e" + target.getName() + " §cexpired.");
            }
        }, REQUEST_TIMEOUT_MS / 50);

        return true;
    }

    public boolean acceptRequest(Player acceptor) {
        UUID aid = acceptor.getUniqueId();
        UUID senderId = pendingRequests.remove(aid);
        if (senderId == null) {
            MessageUtil.sendRaw(acceptor, "§6[Trade] §cNo pending trade request!");
            return false;
        }
        Player sender = Bukkit.getPlayer(senderId);
        if (sender == null) {
            MessageUtil.sendRaw(acceptor, "§6[Trade] §cThe other player is no longer online!");
            return false;
        }

        TradeSession session = new TradeSession(senderId, aid);
        activeTrades.put(senderId, session);
        activeTrades.put(aid, session);

        MessageUtil.sendRaw(sender, "§6[Trade] §a" + acceptor.getName() + " §aaccepted your trade! Opening trade window...");
        MessageUtil.sendRaw(acceptor, "§6[Trade] §aTrade accepted! Opening trade window...");

        plugin.getGuiManager().openTradeMenu(sender, session);
        plugin.getGuiManager().openTradeMenu(acceptor, session);
        return true;
    }

    public void denyRequest(Player denier) {
        UUID did = denier.getUniqueId();
        UUID senderId = pendingRequests.remove(did);
        if (senderId == null) {
            MessageUtil.sendRaw(denier, "§6[Trade] §cNo pending trade request!");
            return;
        }
        Player sender = Bukkit.getPlayer(senderId);
        MessageUtil.sendRaw(denier, "§6[Trade] §cTrade request denied.");
        if (sender != null) MessageUtil.sendRaw(sender, "§6[Trade] §c" + denier.getName() + " §cdenied your trade request.");
    }

    public void addMoney(Player player, double amount) {
        TradeSession session = activeTrades.get(player.getUniqueId());
        if (session == null) return;

        double maxAmount = plugin.getEconomyManager().getBalance(player.getUniqueId());
        double maxTrade = plugin.getConfig().getDouble("trade.max-money", 10000000);
        amount = Math.min(amount, Math.min(maxAmount, maxTrade));
        amount = Math.max(0, amount);

        session.setMyMoney(player.getUniqueId(), amount);
        refreshBothGUIs(session);
    }

    public void addPet(Player player, String petId) {
        TradeSession session = activeTrades.get(player.getUniqueId());
        if (session == null) return;

        Tycoon t = plugin.getTycoonManager().getTycoon(player.getUniqueId());
        if (t == null || !t.getOwnedPets().contains(petId)) {
            MessageUtil.sendRaw(player, "§6[Trade] §cYou don't own that pet!");
            return;
        }
        if (petId.equals(t.getActivePetId())) {
            plugin.getPetManager().despawnPet(t);
            t.setActivePetId(null);
        }

        session.setMyPet(player.getUniqueId(), petId);
        refreshBothGUIs(session);
    }

    public void removePet(Player player) {
        TradeSession session = activeTrades.get(player.getUniqueId());
        if (session == null) return;
        session.setMyPet(player.getUniqueId(), null);
        refreshBothGUIs(session);
    }

    public void confirmTrade(Player player) {
        TradeSession session = activeTrades.get(player.getUniqueId());
        if (session == null) return;

        if (!session.hasAnyOffer()) {
            MessageUtil.sendRaw(player, "§6[Trade] §cAdd something to the trade first!");
            return;
        }

        session.setMyConfirm(player.getUniqueId(), true);

        if (session.bothConfirmed()) {
            executeTrade(session);
        } else {
            Player other = Bukkit.getPlayer(session.getOther(player.getUniqueId()));
            MessageUtil.sendRaw(player, "§6[Trade] §aYou confirmed! Waiting for the other player...");
            if (other != null) {
                MessageUtil.sendRaw(other, "§6[Trade] §e" + player.getName() + " §aconfirmed the trade! Your turn.");
                if (plugin.getSettingsManager().isSoundsEnabled(other.getUniqueId()))
                    other.playSound(other.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            }
            refreshBothGUIs(session);
        }
    }

    public void cancelTrade(UUID playerUUID, String reason) {
        TradeSession session = activeTrades.remove(playerUUID);
        if (session == null) return;

        UUID otherId = session.getOther(playerUUID);
        activeTrades.remove(otherId);

        Player p1 = Bukkit.getPlayer(playerUUID);
        Player p2 = Bukkit.getPlayer(otherId);
        String msg = "§6[Trade] §cTrade cancelled" + (reason != null ? ": " + reason : "!");

        if (p1 != null) { MessageUtil.sendRaw(p1, msg); p1.closeInventory(); }
        if (p2 != null) { MessageUtil.sendRaw(p2, msg); p2.closeInventory(); }
    }

    private void executeTrade(TradeSession session) {
        Player p1 = Bukkit.getPlayer(session.player1);
        Player p2 = Bukkit.getPlayer(session.player2);
        if (p1 == null || p2 == null) {
            cancelTrade(session.player1, "Player went offline");
            return;
        }

        Tycoon t1 = plugin.getTycoonManager().getTycoon(session.player1);
        Tycoon t2 = plugin.getTycoonManager().getTycoon(session.player2);
        if (t1 == null || t2 == null) {
            cancelTrade(session.player1, "Tycoon not found");
            return;
        }

        if (session.moneyOffer1 > 0 && !plugin.getEconomyManager().has(session.player1, session.moneyOffer1)) {
            cancelTrade(session.player1, "§e" + p1.getName() + " §cdoesn't have enough money");
            return;
        }
        if (session.moneyOffer2 > 0 && !plugin.getEconomyManager().has(session.player2, session.moneyOffer2)) {
            cancelTrade(session.player1, "§e" + p2.getName() + " §cdoesn't have enough money");
            return;
        }
        if (session.petOffer1 != null && !t1.getOwnedPets().contains(session.petOffer1)) {
            cancelTrade(session.player1, "§e" + p1.getName() + " §cno longer owns that pet");
            return;
        }
        if (session.petOffer2 != null && !t2.getOwnedPets().contains(session.petOffer2)) {
            cancelTrade(session.player1, "§e" + p2.getName() + " §cno longer owns that pet");
            return;
        }

        if (session.moneyOffer1 > 0) {
            plugin.getEconomyManager().withdraw(session.player1, session.moneyOffer1);
            plugin.getEconomyManager().deposit(session.player2, session.moneyOffer1);
        }
        if (session.moneyOffer2 > 0) {
            plugin.getEconomyManager().withdraw(session.player2, session.moneyOffer2);
            plugin.getEconomyManager().deposit(session.player1, session.moneyOffer2);
        }

        if (session.petOffer1 != null) {
            t1.getOwnedPets().remove(session.petOffer1);
            if (session.petOffer1.equals(t1.getActivePetId())) t1.setActivePetId(null);
            t2.getOwnedPets().add(session.petOffer1);
            plugin.getDatabaseManager().saveOwnedPet(session.player2, session.petOffer1);
        }
        if (session.petOffer2 != null) {
            t2.getOwnedPets().remove(session.petOffer2);
            if (session.petOffer2.equals(t2.getActivePetId())) t2.setActivePetId(null);
            t1.getOwnedPets().add(session.petOffer2);
            plugin.getDatabaseManager().saveOwnedPet(session.player1, session.petOffer2);
        }

        plugin.getDatabaseManager().saveTycoon(t1);
        plugin.getDatabaseManager().saveTycoon(t2);
        plugin.getDatabaseManager().logTrade(session.player1, session.player2,
                session.moneyOffer1, session.moneyOffer2, session.petOffer1, session.petOffer2);

        activeTrades.remove(session.player1);
        activeTrades.remove(session.player2);
        cooldowns.put(session.player1, System.currentTimeMillis());
        cooldowns.put(session.player2, System.currentTimeMillis());

        String summary1 = buildSummary(session, session.player1);
        String summary2 = buildSummary(session, session.player2);

        p1.closeInventory();
        p2.closeInventory();

        MessageUtil.sendRaw(p1, "§8§m──────────────────────────────");
        MessageUtil.sendRaw(p1, "§6[Trade] §a§lTrade Completed!");
        MessageUtil.sendRaw(p1, summary1);
        MessageUtil.sendRaw(p1, "§8§m──────────────────────────────");

        MessageUtil.sendRaw(p2, "§8§m──────────────────────────────");
        MessageUtil.sendRaw(p2, "§6[Trade] §a§lTrade Completed!");
        MessageUtil.sendRaw(p2, summary2);
        MessageUtil.sendRaw(p2, "§8§m──────────────────────────────");

        if (plugin.getSettingsManager().isSoundsEnabled(p1.getUniqueId()))
            p1.playSound(p1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        if (plugin.getSettingsManager().isSoundsEnabled(p2.getUniqueId()))
            p2.playSound(p2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private String buildSummary(TradeSession s, UUID viewer) {
        StringBuilder sb = new StringBuilder();
        double gave = s.isPlayer1(viewer) ? s.moneyOffer1 : s.moneyOffer2;
        double got = s.isPlayer1(viewer) ? s.moneyOffer2 : s.moneyOffer1;
        String gavePet = s.isPlayer1(viewer) ? s.petOffer1 : s.petOffer2;
        String gotPet = s.isPlayer1(viewer) ? s.petOffer2 : s.petOffer1;

        if (gave > 0) sb.append("§c  Gave: §e$").append(String.format("%.2f", gave)).append("\n");
        if (gavePet != null) sb.append("§c  Gave Pet: §d").append(gavePet).append("\n");
        if (got > 0) sb.append("§a  Received: §e$").append(String.format("%.2f", got)).append("\n");
        if (gotPet != null) sb.append("§a  Received Pet: §d").append(gotPet).append("\n");
        return sb.toString().trim();
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        pendingRequests.entrySet().removeIf(e -> false);
        Set<UUID> toCancel = new HashSet<>();
        for (var entry : activeTrades.entrySet()) {
            if (now - entry.getValue().createdAt > TRADE_TIMEOUT_MS) {
                toCancel.add(entry.getKey());
            }
        }
        for (UUID uid : toCancel) cancelTrade(uid, "Trade timed out (2 min)");
    }

    public void handleDisconnect(UUID uuid) {
        pendingRequests.remove(uuid);
        pendingRequests.values().removeIf(v -> v.equals(uuid));
        if (activeTrades.containsKey(uuid)) cancelTrade(uuid, "Player disconnected");
    }

    public TradeSession getSession(UUID uuid) { return activeTrades.get(uuid); }
    public boolean hasActiveSession(UUID uuid) { return activeTrades.containsKey(uuid); }
    public boolean hasPendingRequest(UUID uuid) { return pendingRequests.containsKey(uuid); }

    private void refreshBothGUIs(TradeSession session) {
        Player p1 = Bukkit.getPlayer(session.player1);
        Player p2 = Bukkit.getPlayer(session.player2);
        if (p1 != null) plugin.getGuiManager().openTradeMenu(p1, session);
        if (p2 != null) plugin.getGuiManager().openTradeMenu(p2, session);
    }
}
