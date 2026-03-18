package dev.xdev.moneytycoon.manager;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.IslandType;
import dev.xdev.moneytycoon.model.Tycoon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.*;

public class HologramManager {

    private final MoneyTycoon plugin;
    private final Map<UUID, List<UUID>> tycoonHolograms = new HashMap<>();

    private static final double LINE_GAP = 0.3;

    public HologramManager(MoneyTycoon plugin) {
        this.plugin = plugin;
    }

    public void spawnHolograms(Tycoon t) {
        removeHolograms(t.getOwnerUUID());

        Location base = t.getBaseLocation();
        World w = base.getWorld();
        if (w == null) return;

        List<UUID> ids = new ArrayList<>();
        int bx = base.getBlockX(), by = base.getBlockY(), bz = base.getBlockZ();
        IslandType it = plugin.getTycoonManager().getIslandType(t.getIslandType());
        int cx = it.dropperX(), ps = it.plotSize();

        Location entrance = new Location(w, bx + cx + .5, by + 6.5, bz + ps - .5);
        ids.addAll(spawnLines(entrance,
                gradient("⚡ MONEY TYCOON ⚡", 0xFFAA00, 0xFFFF55),
                Component.text(t.getOwnerName()).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD),
                Component.text("Welcome to the factory!").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)
        ));

        Location dropper = new Location(w, bx + cx + .5, by + 10.5, bz + it.dropperZ() + .5);
        String dropperType = plugin.getConfig().getString(
                "dropper.types." + t.getCurrentDropperType().name() + ".display-name",
                t.getCurrentDropperType().name()).replace("&", "");
        ids.addAll(spawnLines(dropper,
                gradient("⛏ DROPPER ⛏", 0x55FFFF, 0x5555FF),
                Component.text("Type: ").color(NamedTextColor.GRAY)
                        .append(Component.text(dropperType).color(NamedTextColor.AQUA))
                        .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text("Speed: Lv." + t.getUpgradeLevel("dropper-speed")).color(NamedTextColor.YELLOW))
        ));

        Location sell = new Location(w, bx + cx + .5, by + 3.5, bz + it.sellZ() + .5);
        boolean autoCollect = t.getUpgradeLevel("auto-collect") > 0
                || plugin.getPetManager().hasAutoCollectPet(t)
                || (Bukkit.getPlayer(t.getOwnerUUID()) != null
                && Bukkit.getPlayer(t.getOwnerUUID()).hasPermission("moneytycoon.vip")
                && plugin.getConfig().getBoolean("vip.auto-collect-free", true));
        if (autoCollect) {
            double wallet = plugin.getEconomyManager().getBalance(t.getOwnerUUID());
            ids.addAll(spawnLines(sell,
                    gradient("💰 SELL POINT 💰", 0x55FF55, 0xFFFF55),
                    Component.text("Auto-collect: ").color(NamedTextColor.GRAY)
                            .append(Component.text("✓").color(NamedTextColor.GREEN))
                            .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text("Wallet: $" + fmt(wallet)).color(NamedTextColor.GOLD))
                            .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text("Total: $" + fmt(t.getTotalEarned())).color(NamedTextColor.YELLOW))
            ));
        } else {
            ids.addAll(spawnLines(sell,
                    gradient("💰 SELL POINT 💰", 0x55FF55, 0xFFFF55),
                    Component.text("Balance: ").color(NamedTextColor.GRAY)
                            .append(Component.text("$" + fmt(t.getBalance())).color(NamedTextColor.GREEN))
                            .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text("Total: $" + fmt(t.getTotalEarned())).color(NamedTextColor.GOLD))
            ));
        }

        int shopZ = Math.max(it.convStartZ(), it.convEndZ() / 2 - 2);
        Location upgrade = new Location(w, bx + 5.5, by + 3.2, bz + shopZ + 2.5);
        ids.addAll(spawnLines(upgrade,
                gradient("⚙ UPGRADES ⚙", 0xFFFF55, 0xFFAA00),
                Component.text("Right-click to open!").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)
        ));

        Location quest = new Location(w, bx + ps - 6.5, by + 3.2, bz + shopZ + 2.5);
        ids.addAll(spawnLines(quest,
                gradient("📜 QUESTS 📜", 0xFFAA00, 0xFF5555),
                Component.text("Check your daily quests!").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC)
        ));

        int petZ = Math.min(ps - 11, it.spawnZ() - 6);
        Location pet = new Location(w, bx + 5.5, by + 2.8, bz + petZ + 2.5);
        String activePet = t.getActivePetId() != null ? t.getActivePetId() : "None";
        ids.addAll(spawnLines(pet,
                gradient("🐾 PET ZONE 🐾", 0xFF55FF, 0xAA00AA),
                Component.text("Active: ").color(NamedTextColor.GRAY)
                        .append(Component.text(activePet).color(
                                t.getActivePetId() != null ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.DARK_GRAY))
        ));

        Location spawn = new Location(w, bx + it.spawnX() + .5, by + 2.5, bz + it.spawnZ() + .5);
        ids.addAll(spawnLines(spawn,
                gradient("★ SPAWN ★", 0x55FFFF, 0x55FF55)
        ));

        tycoonHolograms.put(t.getOwnerUUID(), ids);
    }

    public void updateDynamic(Tycoon t) {
        List<UUID> ids = tycoonHolograms.get(t.getOwnerUUID());
        if (ids == null || ids.isEmpty()) return;

        int sellInfoIndex = 6;
        if (ids.size() <= sellInfoIndex) return;

        Entity sellInfo = Bukkit.getEntity(ids.get(sellInfoIndex));
        if (!(sellInfo instanceof ArmorStand as)) return;

        boolean autoCollect = t.getUpgradeLevel("auto-collect") > 0
                || plugin.getPetManager().hasAutoCollectPet(t)
                || (Bukkit.getPlayer(t.getOwnerUUID()) != null
                && Bukkit.getPlayer(t.getOwnerUUID()).hasPermission("moneytycoon.vip")
                && plugin.getConfig().getBoolean("vip.auto-collect-free", true));

        if (autoCollect) {
            double wallet = plugin.getEconomyManager().getBalance(t.getOwnerUUID());
            as.customName(
                    Component.text("Auto-collect: ").color(NamedTextColor.GRAY)
                            .append(Component.text("✓").color(NamedTextColor.GREEN))
                            .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text("Wallet: $" + fmt(wallet)).color(NamedTextColor.GOLD))
                            .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text("Total: $" + fmt(t.getTotalEarned())).color(NamedTextColor.YELLOW))
            );
        } else {
            as.customName(
                    Component.text("Balance: ").color(NamedTextColor.GRAY)
                            .append(Component.text("$" + fmt(t.getBalance())).color(NamedTextColor.GREEN))
                            .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text("Total: $" + fmt(t.getTotalEarned())).color(NamedTextColor.GOLD))
            );
        }
    }

    public void removeHolograms(UUID ownerUUID) {
        List<UUID> ids = tycoonHolograms.remove(ownerUUID);
        if (ids == null) return;
        for (UUID id : ids) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
        }
    }

    public void removeAll() {
        for (UUID owner : new ArrayList<>(tycoonHolograms.keySet())) {
            removeHolograms(owner);
        }
    }

    private List<UUID> spawnLines(Location topLoc, Component... lines) {
        List<UUID> ids = new ArrayList<>();
        World w = topLoc.getWorld();
        if (w == null) return ids;

        for (int i = 0; i < lines.length; i++) {
            Location lineLoc = topLoc.clone().subtract(0, i * LINE_GAP, 0);
            ArmorStand as = w.spawn(lineLoc, ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setMarker(true);
                stand.setSmall(true);
                stand.setCustomNameVisible(true);
                stand.setInvulnerable(true);
                stand.setCollidable(false);
                stand.setSilent(true);
                stand.setPersistent(true);
            });
            as.customName(lines[i]);
            ids.add(as.getUniqueId());
        }
        return ids;
    }

    private Component gradient(String text, int colorStart, int colorEnd) {
        if (text.isEmpty()) return Component.empty();

        var builder = Component.text();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            float ratio = len > 1 ? (float) i / (len - 1) : 0;
            int r = lerp((colorStart >> 16) & 0xFF, (colorEnd >> 16) & 0xFF, ratio);
            int g = lerp((colorStart >> 8) & 0xFF, (colorEnd >> 8) & 0xFF, ratio);
            int b = lerp(colorStart & 0xFF, colorEnd & 0xFF, ratio);
            builder.append(Component.text(text.charAt(i))
                    .color(TextColor.color(r, g, b))
                    .decoration(TextDecoration.BOLD, true));
        }
        return builder.build();
    }

    private int lerp(int a, int b, float t) {
        return (int) (a + (b - a) * t);
    }

    private String fmt(double a) {
        if (a >= 1_000_000_000) return String.format("%.1fB", a / 1_000_000_000);
        if (a >= 1_000_000) return String.format("%.1fM", a / 1_000_000);
        if (a >= 1_000) return String.format("%.1fK", a / 1_000);
        return String.format("%.0f", a);
    }
}
