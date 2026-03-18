package dev.xdev.moneytycoon.manager;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.IslandType;
import dev.xdev.moneytycoon.model.Tycoon;
import dev.xdev.moneytycoon.model.TycoonTheme;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;


public class TycoonManager {

    private final MoneyTycoon plugin;
    private final Map<UUID, Tycoon> tycoons = new HashMap<>();
    private final Set<Integer> usedGridIndices = new HashSet<>();

    private static final int DROPPER_HEIGHT = 8;

    public TycoonManager(MoneyTycoon plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        for (Tycoon t : plugin.getDatabaseManager().loadAllTycoons()) registerTycoon(t);
        plugin.getLogger().info(tycoons.size() + " tycoons loaded.");
    }

    public Tycoon createTycoon(Player player) {
        return createTycoon(player, "DEFAULT");
    }

    public Tycoon createTycoon(Player player, String islandTypeId) {
        if (tycoons.containsKey(player.getUniqueId())) return null;
        IslandType it = getIslandType(islandTypeId);
        int idx = getNextGridIndex();
        Location base = gridIndexToLocation(idx);
        Tycoon t = new Tycoon(player.getUniqueId(), player.getName(), base, idx);
        t.setIslandType(it.id());
        usedGridIndices.add(idx);
        buildTycoonBase(t);
        setupLocations(t);
        tycoons.put(player.getUniqueId(), t);
        plugin.getDatabaseManager().saveTycoon(t);
        plugin.getHologramManager().spawnHolograms(t);
        player.teleport(base.clone().add(it.spawnX() + .5, 1, it.spawnZ() + .5));
        return t;
    }

    public void deleteTycoon(UUID uuid) {
        Tycoon t = tycoons.remove(uuid);
        if (t == null) return;
        Player p = Bukkit.getPlayer(uuid);
        if (p != null && isInTycoon(p.getLocation(), t))
            p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        plugin.getHologramManager().removeHolograms(uuid);
        plugin.getPetManager().despawnPet(t);
        cleanupEntities(t);
        clearBase(t);
        usedGridIndices.remove(t.getGridIndex());
        plugin.getDatabaseManager().deleteTycoon(uuid);
    }

    public void registerTycoon(Tycoon t) {
        tycoons.put(t.getOwnerUUID(), t);
        usedGridIndices.add(t.getGridIndex());
        setupLocations(t);
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getHologramManager().spawnHolograms(t), 5L);
    }

    public Tycoon getTycoon(UUID uuid) { return tycoons.get(uuid); }
    public Tycoon getTycoonByOwnerName(String name) {
        if (name == null) return null;
        for (Tycoon t : tycoons.values())
            if (t.getOwnerName() != null && t.getOwnerName().equalsIgnoreCase(name)) return t;
        return null;
    }
    public Collection<Tycoon> getAllTycoons() { return tycoons.values(); }

    public Tycoon getTycoonAt(Location loc) {
        for (Tycoon t : tycoons.values()) if (isInTycoon(loc, t)) return t;
        return null;
    }

    public Tycoon findCoopTycoon(UUID memberUUID) {
        for (Tycoon t : tycoons.values())
            if (t.getCoopMembers().contains(memberUUID)) return t;
        return null;
    }

    public boolean isSellBlock(Location loc, Tycoon t) {
        Location sell = t.getSellLocation();
        if (sell == null) return false;
        return loc.getWorld().equals(sell.getWorld())
                && loc.getBlockX() == sell.getBlockX()
                && loc.getBlockY() == sell.getBlockY()
                && loc.getBlockZ() == sell.getBlockZ();
    }

    public boolean isInTycoon(Location loc, Tycoon t) {
        Location b = t.getBaseLocation();
        if (!loc.getWorld().equals(b.getWorld())) return false;
        int ps = getIslandType(t.getIslandType()).plotSize();
        return loc.getBlockX() >= b.getBlockX() && loc.getBlockX() < b.getBlockX() + ps
                && loc.getBlockZ() >= b.getBlockZ() && loc.getBlockZ() < b.getBlockZ() + ps
                && loc.getBlockY() >= b.getBlockY() - 5 && loc.getBlockY() <= b.getBlockY() + 25;
    }

    public IslandType getIslandType(String id) {
        return IslandType.fromConfig(plugin, id != null ? id : "DEFAULT");
    }

    public List<IslandType> getAvailableIslandTypes() {
        org.bukkit.configuration.ConfigurationSection sec = plugin.getConfig().getConfigurationSection("island-types");
        if (sec == null) return List.of(getIslandType("DEFAULT"));
        List<IslandType> list = new ArrayList<>();
        for (String key : sec.getKeys(false)) list.add(getIslandType(key));
        return list;
    }

    public void setupLocations(Tycoon t) {
        Location b = t.getBaseLocation();
        World w = plugin.getWorldManager().getTycoonWorld();
        if (w == null) w = b.getWorld();
        if (w == null) return;

        IslandType it = getIslandType(t.getIslandType());
        int bx = b.getBlockX(), by = b.getBlockY(), bz = b.getBlockZ();

        if (b.getWorld() != w || b.getX() != bx || b.getY() != by || b.getZ() != bz) {
            t.setBaseLocation(new Location(w, bx, by, bz));
        }

        t.setDropperLocation(new Location(w, bx + it.dropperX() + .5, by + DROPPER_HEIGHT, bz + it.dropperZ() + .5));
        t.setSellLocation(new Location(w, bx + it.dropperX() + .5, by + 1.0, bz + it.sellZ() + .5));

        List<Location> path = new ArrayList<>();
        for (int z = it.convStartZ(); z <= it.convEndZ(); z++)
            path.add(new Location(w, bx + it.dropperX() + .5, by + 1.0, bz + z + .5));
        t.setConveyorPath(path);

        int half = it.plotSize() / 2;
        List<Location> extras = new ArrayList<>();
        extras.add(new Location(w, bx + half - 8 + .5, by + DROPPER_HEIGHT, bz + it.dropperZ() + .5));
        extras.add(new Location(w, bx + half + 8 + .5, by + DROPPER_HEIGHT, bz + it.dropperZ() + .5));
        extras.add(new Location(w, bx + it.dropperX() + .5, by + DROPPER_HEIGHT, bz + it.dropperZ() - 4 + .5));
        t.setExtraDropperLocations(extras);
    }

    public void buildTycoonBase(Tycoon t) {
        Location b = t.getBaseLocation();
        World w = plugin.getWorldManager().getTycoonWorld();
        if (w == null) w = b.getWorld();
        if (w == null) return;
        IslandType it = getIslandType(t.getIslandType());
        int bx = b.getBlockX(), by = b.getBlockY(), bz = b.getBlockZ();
        int ps = it.plotSize();
        int cx = it.dropperX();
        TycoonTheme th = t.getTheme();

        for (int x = 0; x < ps; x++)
            for (int z = 0; z < ps; z++)
                for (int y = 1; y <= DROPPER_HEIGHT + 3; y++)
                    w.getBlockAt(bx + x, by + y, bz + z).setType(Material.AIR, false);

        for (int x = 0; x < ps; x++)
            for (int z = 0; z < ps; z++)
                w.getBlockAt(bx + x, by, bz + z).setType(th.floor, false);

        for (int x = 0; x < ps; x++)
            for (int z = 0; z < ps; z++)
                w.getBlockAt(bx + x, by - 1, bz + z).setType(Material.STONE, false);

        int archStart = Math.max(0, ps / 2 - 3), archEnd = Math.min(ps - 1, ps / 2 + 3);
        for (int x = 0; x < ps; x++) for (int y = 1; y <= 3; y++) {
            w.getBlockAt(bx + x, by + y, bz).setType(th.wall, false);
            if (x < archStart || x > archEnd) w.getBlockAt(bx + x, by + y, bz + ps - 1).setType(th.wall, false);
        }
        for (int z = 0; z < ps; z++) for (int y = 1; y <= 3; y++) {
            w.getBlockAt(bx, by + y, bz + z).setType(th.wall, false);
            w.getBlockAt(bx + ps - 1, by + y, bz + z).setType(th.wall, false);
        }

        int[][] corners = {{0, 0}, {ps - 1, 0}, {0, ps - 1}, {ps - 1, ps - 1}};
        for (int[] c : corners) {
            for (int y = 1; y <= 4; y++)
                w.getBlockAt(bx + c[0], by + y, bz + c[1]).setType(Material.POLISHED_BLACKSTONE, false);
            w.getBlockAt(bx + c[0], by + 5, bz + c[1]).setType(Material.LANTERN, false);
        }

        for (int x = archStart; x <= archEnd; x++)
            w.getBlockAt(bx + x, by, bz + ps - 1).setType(th.accent, false);
        w.getBlockAt(bx + archStart, by + 4, bz + ps - 1).setType(th.wall, false);
        for (int x = archStart + 1; x < archEnd; x++)
            w.getBlockAt(bx + x, by + 4, bz + ps - 1).setType(th.accent, false);
        w.getBlockAt(bx + archEnd, by + 4, bz + ps - 1).setType(th.wall, false);

        for (int z = it.convStartZ(); z <= it.sellZ() + 1; z++) {
            w.getBlockAt(bx + cx - 1, by, bz + z).setType(th.conveyorSide, false);
            w.getBlockAt(bx + cx, by, bz + z).setType(th.conveyor, false);
            w.getBlockAt(bx + cx + 1, by, bz + z).setType(th.conveyorSide, false);
        }

        buildDropperTower(w, bx + it.dropperX(), by, bz + it.dropperZ(), th);

        int extraLevel = t.getUpgradeLevel("extra-dropper");
        List<Location> extras = t.getExtraDropperLocations();
        if (extras != null)
            for (int i = 0; i < Math.min(extraLevel, extras.size()); i++)
                buildExtraDropper(t, i);

        boolean autoCollect = t.getUpgradeLevel("auto-collect") > 0
                || plugin.getPetManager().hasAutoCollectPet(t)
                || (Bukkit.getPlayer(t.getOwnerUUID()) != null
                && Bukkit.getPlayer(t.getOwnerUUID()).hasPermission("moneytycoon.vip")
                && plugin.getConfig().getBoolean("vip.auto-collect-free", true));
        Material sellBlock = autoCollect && plugin.getConfig().getBoolean("purchasables.obsidian-chest.enabled", true)
                ? Material.OBSIDIAN : Material.CHEST;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = 0; dz <= 1; dz++)
                w.getBlockAt(bx + cx + dx, by, bz + it.sellZ() + dz).setType(th.sell, false);
        w.getBlockAt(bx + cx, by + 1, bz + it.sellZ()).setType(sellBlock, false);

        int shopZ = Math.max(it.convStartZ(), it.convEndZ() / 2 - 2);
        buildAreaPlatform(w, bx + 3, by, bz + shopZ, 5, 5, Material.EMERALD_BLOCK, th.accent);
        buildAreaPlatform(w, bx + ps - 8, by, bz + shopZ, 5, 5, Material.BOOKSHELF, th.accent);
        w.getBlockAt(bx + ps - 6, by + 1, bz + shopZ + 2).setType(Material.LECTERN, false);

        int petZ = Math.min(ps - 11, it.spawnZ() - 6);
        for (int dx = 0; dx < 5; dx++)
            for (int dz = 0; dz < 5; dz++)
                w.getBlockAt(bx + 3 + dx, by, bz + petZ + dz).setType(Material.GRASS_BLOCK, false);
        w.getBlockAt(bx + 5, by + 1, bz + petZ + 2).setType(Material.DANDELION, false);
        w.getBlockAt(bx + 4, by + 1, bz + petZ + 1).setType(Material.POPPY, false);

        w.getBlockAt(bx + it.spawnX(), by, bz + it.spawnZ()).setType(Material.DIAMOND_BLOCK, false);

        for (int x = 5; x < ps; x += 7) {
            w.getBlockAt(bx + x, by + 1, bz + 1).setType(Material.OAK_FENCE, false);
            w.getBlockAt(bx + x, by + 2, bz + 1).setType(Material.LANTERN, false);
        }

        for (int z = it.convStartZ(); z <= it.spawnZ(); z++) {
            if (cx - 9 >= 0) w.getBlockAt(bx + cx - 9, by, bz + z).setType(th.accent, false);
            if (cx + 9 < ps) w.getBlockAt(bx + cx + 9, by, bz + z).setType(th.accent, false);
        }
    }

    private void buildDropperTower(World w, int x, int by, int z, TycoonTheme th) {
        for (int y = 1; y < DROPPER_HEIGHT; y++)
            w.getBlockAt(x, by + y, z).setType(th.pillar, false);
        Block dropperBlock = w.getBlockAt(x, by + DROPPER_HEIGHT, z);
        dropperBlock.setType(Material.DROPPER, false);
        BlockData data = dropperBlock.getBlockData();
        if (data instanceof Directional dir) {
            dir.setFacing(org.bukkit.block.BlockFace.DOWN);
            dropperBlock.setBlockData(data);
        }
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                w.getBlockAt(x + dx, by + DROPPER_HEIGHT, z + dz).setType(Material.SMOOTH_STONE_SLAB, false);
            }
    }

    private void buildAreaPlatform(World w, int sx, int by, int sz, int width, int depth,
                                   Material marker, Material floor) {
        for (int dx = 0; dx < width; dx++)
            for (int dz = 0; dz < depth; dz++)
                w.getBlockAt(sx + dx, by, sz + dz).setType(floor, false);
        w.getBlockAt(sx + width / 2, by, sz + depth / 2).setType(marker, false);
        w.getBlockAt(sx + width / 2, by + 1, sz + depth / 2).setType(marker, false);
    }

    public void buildExtraDropper(Tycoon t, int index) {
        if (index < 0 || index >= t.getExtraDropperLocations().size()) return;
        Location loc = t.getExtraDropperLocations().get(index);
        buildDropperTower(loc.getWorld(), loc.getBlockX(), t.getBaseLocation().getBlockY(),
                loc.getBlockZ(), t.getTheme());
    }

    public void clearBase(Tycoon t) {
        Location b = t.getBaseLocation();
        World w = b.getWorld();
        int ps = getIslandType(t.getIslandType()).plotSize();
        int bx = b.getBlockX(), by = b.getBlockY(), bz = b.getBlockZ();
        for (int x = 0; x < ps; x++)
            for (int z = 0; z < ps; z++)
                for (int y = -1; y <= DROPPER_HEIGHT + 3; y++) {
                    Block block = w.getBlockAt(bx + x, by + y, bz + z);
                    BlockState state = block.getState();
                    if (state instanceof InventoryHolder ih) ih.getInventory().clear();
                    block.setType(Material.AIR, false);
                }
    }

    public void saveConveyorAndCleanup(Tycoon t) {
        List<Object[]> toSave = new ArrayList<>();
        List<Location> path = t.getConveyorPath();
        if (path != null && !path.isEmpty()) {
            for (var entry : t.getTrackedItems().entrySet()) {
                int idx = entry.getValue();
                if (idx < 0) continue;
                Entity e = Bukkit.getEntity(entry.getKey());
                if (e instanceof Item item && !item.isDead()) {
                    ItemStack is = item.getItemStack();
                    if (is != null && !is.getType().isAir())
                        toSave.add(new Object[]{is.getType().name(), is.getAmount(), idx});
                }
                if (e != null) e.remove();
            }
        } else {
            for (UUID id : t.getTrackedItems().keySet()) {
                Entity e = Bukkit.getEntity(id);
                if (e != null) e.remove();
            }
        }
        t.getTrackedItems().clear();
        if (!toSave.isEmpty()) plugin.getDatabaseManager().saveConveyorItems(t.getOwnerUUID(), toSave);
    }

    public void clearConveyorSave(UUID uuid) {
        plugin.getDatabaseManager().saveConveyorItems(uuid, List.of());
    }

    public void restoreConveyorItems(Tycoon t) {
        List<Object[]> saved = plugin.getDatabaseManager().loadConveyorItems(t.getOwnerUUID());
        if (saved.isEmpty()) return;
        List<Location> path = t.getConveyorPath();
        if (path == null || path.isEmpty()) return;
        World w = path.get(0).getWorld();
        if (w == null) return;
        int maxItems = plugin.getConfig().getInt("performance.max-items-per-tycoon", 80);
        for (Object[] row : saved) {
            if (t.getTrackedItems().size() >= maxItems) break;
            String matName = (String) row[0];
            int amount = (Integer) row[1];
            int idx = (Integer) row[2];
            if (idx < 0 || idx >= path.size()) continue;
            Material mat;
            try { mat = Material.valueOf(matName); } catch (Exception ignored) { continue; }
            Location loc = path.get(idx).clone();
            ItemStack is = new ItemStack(mat, Math.max(1, Math.min(amount, 64)));
            Item item = w.dropItem(loc, is);
            item.setPickupDelay(Integer.MAX_VALUE);
            item.setVelocity(new Vector(0, 0, 0));
            item.setGravity(false);
            t.getTrackedItems().put(item.getUniqueId(), idx);
        }
        clearConveyorSave(t.getOwnerUUID());
    }

    public void cleanupEntities(Tycoon t) {
        for (UUID id : t.getTrackedItems().keySet()) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        t.getTrackedItems().clear();
    }

    public void cleanup() {
        for (Tycoon t : tycoons.values()) {
            plugin.getPetManager().despawnPet(t);
            cleanupEntities(t);
        }
    }

    public double getDropperValue(Tycoon t) {
        double base = plugin.getConfig().getDouble("dropper.types." + t.getCurrentDropperType().name() + ".value", 1.0);
        double petBonus = plugin.getPetManager().getPetBonus(t, "VALUE_MULT");
        int processorLevel = t.getUpgradeLevel("processor");
        double processorMult = 1.0 + processorLevel * 0.3;
        double vipMult = Bukkit.getPlayer(t.getOwnerUUID()) != null
                && Bukkit.getPlayer(t.getOwnerUUID()).hasPermission("moneytycoon.vip")
                ? plugin.getConfig().getDouble("vip.money-multiplier", 2.0) : 1.0;
        return base * t.getValueMultiplier() * (1 + petBonus) * vipMult * processorMult * t.getBoosterMultiplier();
    }

    public double getUpgradeCost(String id, int lvl) {
        double base = plugin.getConfig().getDouble("upgrades." + id + ".base-cost", 100);
        double mult = plugin.getConfig().getDouble("upgrades." + id + ".cost-multiplier", 2.0);
        return base * Math.pow(mult, lvl);
    }

    public int getUpgradeMaxLevel(String id) {
        return plugin.getConfig().getInt("upgrades." + id + ".max-level", 10);
    }

    public int getDropperInterval(Tycoon t) {
        int base = plugin.getConfig().getInt("dropper.base-interval", 40);
        int red = plugin.getConfig().getInt("dropper.speed-upgrade-reduction", 4);
        int speedLvl = t.getUpgradeLevel("dropper-speed");
        double petBonus = plugin.getPetManager().getPetBonus(t, "DROPPER_SPEED");
        int interval = base - (speedLvl * red);
        interval = (int) (interval * (1.0 - petBonus));
        return Math.max(5, interval);
    }

    public boolean purchaseUpgrade(Player p, Tycoon t, String id) {
        int cur = t.getUpgradeLevel(id);
        if (cur >= getUpgradeMaxLevel(id)) return false;
        double cost = getUpgradeCost(id, cur);
        if (!plugin.getEconomyManager().has(p.getUniqueId(), cost)) return false;
        plugin.getEconomyManager().withdraw(p.getUniqueId(), cost);
        t.setUpgradeLevel(id, cur + 1);
        if (id.equals("extra-dropper")) buildExtraDropper(t, cur);
        plugin.getDatabaseManager().saveTycoon(t);
        plugin.getQuestManager().incrementProgress(p.getUniqueId(), "UPGRADE", 1);
        return true;
    }

    public boolean purchaseDropperType(Player p, Tycoon t, Material type) {
        double cost = plugin.getConfig().getDouble("dropper.types." + type.name() + ".cost", 0);
        boolean vip = plugin.getConfig().getBoolean("dropper.types." + type.name() + ".vip", false);
        if (vip && !p.hasPermission("moneytycoon.vip")) return false;
        if (cost > 0 && !plugin.getEconomyManager().has(p.getUniqueId(), cost)) return false;
        if (cost > 0) plugin.getEconomyManager().withdraw(p.getUniqueId(), cost);
        t.setCurrentDropperType(type);
        plugin.getDatabaseManager().saveTycoon(t);
        return true;
    }

    public double getPrestigeCost(Tycoon t) {
        return plugin.getConfig().getDouble("prestige.base-cost", 100000)
                * Math.pow(plugin.getConfig().getDouble("prestige.cost-multiplier", 3.0), t.getPrestigeLevel());
    }

    public boolean performPrestige(Player p, Tycoon t) {
        double cost = getPrestigeCost(t);
        if (!plugin.getEconomyManager().has(p.getUniqueId(), cost)) return false;
        double chestBal = t.getBalance();
        if (chestBal > 0) {
            plugin.getEconomyManager().deposit(p.getUniqueId(), chestBal);
            t.setBalance(0);
        }
        plugin.getEconomyManager().withdraw(p.getUniqueId(), cost);
        t.setPrestigeLevel(t.getPrestigeLevel() + 1);
        t.getUpgrades().clear();
        t.setCurrentDropperType(Material.COBBLESTONE);
        plugin.getPetManager().despawnPet(t);
        cleanupEntities(t);
        teleportBeforeRebuild(p, t);
        clearBase(t);
        buildTycoonBase(t);
        setupLocations(t);
        plugin.getHologramManager().spawnHolograms(t);
        finishTransition(p, t);
        plugin.getDatabaseManager().saveTycoon(t);
        plugin.getQuestManager().incrementProgress(p.getUniqueId(), "PRESTIGE", 1);
        if (plugin.getLogManager() != null) plugin.getLogManager().logPrestige(p.getUniqueId(), p.getName(), t.getPrestigeLevel());
        return true;
    }

    public boolean performRebirth(Player p, Tycoon t) {
        if (!plugin.getConfig().getBoolean("rebirth.enabled", true)) return false;
        int reqPrestige = plugin.getConfig().getInt("rebirth.prestige-requirement", 10);
        if (t.getPrestigeLevel() < reqPrestige) return false;
        double cost = plugin.getConfig().getDouble("rebirth.base-cost", 10000000)
                * Math.pow(plugin.getConfig().getDouble("rebirth.cost-multiplier", 5.0), t.getRebirthLevel());
        if (!plugin.getEconomyManager().has(p.getUniqueId(), cost)) return false;
        double chestBal = t.getBalance();
        if (chestBal > 0) {
            plugin.getEconomyManager().deposit(p.getUniqueId(), chestBal);
            t.setBalance(0);
        }
        plugin.getEconomyManager().withdraw(p.getUniqueId(), cost);
        t.setRebirthLevel(t.getRebirthLevel() + 1);
        t.setPrestigeLevel(0);
        t.getUpgrades().clear();
        t.setCurrentDropperType(Material.COBBLESTONE);
        plugin.getPetManager().despawnPet(t);
        cleanupEntities(t);
        teleportBeforeRebuild(p, t);
        clearBase(t);
        buildTycoonBase(t);
        setupLocations(t);
        plugin.getHologramManager().spawnHolograms(t);
        finishTransition(p, t);
        plugin.getDatabaseManager().saveTycoon(t);
        if (plugin.getLogManager() != null) plugin.getLogManager().logRebirth(p.getUniqueId(), p.getName(), t.getRebirthLevel());
        return true;
    }

    public double getRebirthCost(Tycoon t) {
        return plugin.getConfig().getDouble("rebirth.base-cost", 10000000)
                * Math.pow(plugin.getConfig().getDouble("rebirth.cost-multiplier", 5.0), t.getRebirthLevel());
    }

    public void collectBalance(Player p, Tycoon t) {
        double bal = t.getBalance();
        if (bal <= 0) return;
        plugin.getEconomyManager().deposit(p.getUniqueId(), bal);
        t.setBalance(0);
        plugin.getQuestManager().incrementProgress(p.getUniqueId(), "COLLECT", 1);
        plugin.getDatabaseManager().saveTycoon(t);
    }

    public boolean changeTheme(Player p, Tycoon t, TycoonTheme theme) {
        double cost = plugin.getConfig().getDouble("themes." + theme.name() + ".cost", 0);
        boolean vip = plugin.getConfig().getBoolean("themes." + theme.name() + ".vip", false);
        if (vip && !p.hasPermission("moneytycoon.vip")) return false;
        if (cost > 0 && !plugin.getEconomyManager().has(p.getUniqueId(), cost)) return false;
        if (cost > 0) plugin.getEconomyManager().withdraw(p.getUniqueId(), cost);
        t.setTheme(theme);
        plugin.getPetManager().despawnPet(t);
        cleanupEntities(t);
        teleportBeforeRebuild(p, t);
        clearBase(t);
        buildTycoonBase(t);
        setupLocations(t);
        plugin.getHologramManager().spawnHolograms(t);
        finishTransition(p, t);
        plugin.getDatabaseManager().saveTycoon(t);
        return true;
    }

    private void finishTransition(Player p, Tycoon t) {
        t.resetDropperTick();
        Location base = t.getBaseLocation();
        if (base != null && base.getWorld() != null) {
            var it = getIslandType(t.getIslandType());
            Location spawn = base.clone().add(it.spawnX() + .5, 1, it.spawnZ() + .5);
            spawn.setYaw(p.getLocation().getYaw());
            spawn.setPitch(p.getLocation().getPitch());
            p.teleport(spawn);
        }
        if (t.getActivePetId() != null && p.isOnline())
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getPetManager().spawnPet(p, t), 10L);
    }

    private void teleportBeforeRebuild(Player p, Tycoon t) {
        Location base = t.getBaseLocation();
        if (base == null || base.getWorld() == null) return;
        if (!isInTycoon(p.getLocation(), t)) return;
        var it = getIslandType(t.getIslandType());
        Location safe = base.clone().add(it.spawnX() + .5, 6, it.spawnZ() + .5);
        safe.setYaw(p.getLocation().getYaw());
        safe.setPitch(p.getLocation().getPitch());
        p.teleport(safe);
    }

    private int getNextGridIndex() {
        int i = 0;
        while (usedGridIndices.contains(i)) i++;
        return i;
    }

    private Location gridIndexToLocation(int index) {
        World w = plugin.getWorldManager().getTycoonWorld();
        if (w == null) w = Bukkit.getWorlds().get(0);
        int sx = plugin.getConfig().getInt("general.start-x", 0);
        int sz = plugin.getConfig().getInt("general.start-z", 0);
        int sy = plugin.getConfig().getInt("general.start-y", 100);
        int ps = getMaxPlotSize();
        int gap = plugin.getConfig().getInt("general.plot-gap", 20);
        int gw = plugin.getConfig().getInt("general.grid-width", 10);
        return new Location(w, sx + (index % gw) * (ps + gap), sy, sz + (index / gw) * (ps + gap));
    }

    private int getMaxPlotSize() {
        var sec = plugin.getConfig().getConfigurationSection("island-types");
        if (sec == null) return plugin.getConfig().getInt("general.plot-size", 35);
        int max = 0;
        for (String key : sec.getKeys(false)) {
            int ps = sec.getInt(key + ".plot-size", 35);
            if (ps > max) max = ps;
        }
        return max > 0 ? max : plugin.getConfig().getInt("general.plot-size", 35);
    }
}
