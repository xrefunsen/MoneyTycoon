package dev.xdev.moneytycoon.model;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Tycoon {

    private final UUID ownerUUID;
    private String ownerName;
    private Location baseLocation;
    private final int gridIndex;
    private double balance;
    private Material currentDropperType;
    private final Map<String, Integer> upgrades;
    private int prestigeLevel;
    private int rebirthLevel;
    private TycoonTheme theme;
    private String islandType = "DEFAULT";
    private boolean active;
    private long totalEarned;
    private long itemsSold;

    private String activePetId;
    private final Set<String> ownedPets;
    private final Set<UUID> coopMembers;

    private double boosterMultiplier = 1.0;
    private long boosterEndTime = 0;
    private long lastMilestone = 0;

    private transient final Map<UUID, Integer> trackedItems = new ConcurrentHashMap<>();
    private transient List<Location> conveyorPath = new ArrayList<>();
    private transient Location dropperLocation;
    private transient Location sellLocation;
    private transient List<Location> extraDropperLocations = new ArrayList<>();
    private transient int dropperTickCounter;
    private transient UUID petEntityId;

    public Tycoon(UUID ownerUUID, String ownerName, Location baseLocation, int gridIndex) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.baseLocation = baseLocation;
        this.gridIndex = gridIndex;
        this.balance = 0;
        this.currentDropperType = Material.COBBLESTONE;
        this.upgrades = new HashMap<>();
        this.prestigeLevel = 0;
        this.rebirthLevel = 0;
        this.theme = TycoonTheme.CLASSIC;
        this.active = true;
        this.totalEarned = 0;
        this.itemsSold = 0;
        this.dropperTickCounter = 0;
        this.ownedPets = new HashSet<>();
        this.coopMembers = new HashSet<>();
    }

    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String n) { this.ownerName = n; }
    public Location getBaseLocation() { return baseLocation; }
    public void setBaseLocation(Location l) { this.baseLocation = l; }
    public int getGridIndex() { return gridIndex; }
    public double getBalance() { return balance; }
    public void setBalance(double b) { this.balance = b; }
    public void addBalance(double amt) { this.balance += amt; this.totalEarned += (long) amt; }
    public Material getCurrentDropperType() { return currentDropperType; }
    public void setCurrentDropperType(Material m) { this.currentDropperType = m; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public long getTotalEarned() { return totalEarned; }
    public void setTotalEarned(long t) { this.totalEarned = t; }
    public long getItemsSold() { return itemsSold; }
    public void setItemsSold(long i) { this.itemsSold = i; }
    public void incrementItemsSold() { this.itemsSold++; }

    public int getPrestigeLevel() { return prestigeLevel; }
    public void setPrestigeLevel(int l) { this.prestigeLevel = l; }
    public int getRebirthLevel() { return rebirthLevel; }
    public void setRebirthLevel(int l) { this.rebirthLevel = l; }

    public double getPrestigeMultiplier() { return 1.0 + (prestigeLevel * 0.5); }
    public double getRebirthMultiplier() { return 1.0 + (rebirthLevel * 1.0); }
    public double getValueMultiplier() {
        int level = getUpgradeLevel("value-multiplier");
        return (1.0 + level * 0.5) * getPrestigeMultiplier() * getRebirthMultiplier();
    }

    public TycoonTheme getTheme() { return theme; }
    public void setTheme(TycoonTheme t) { this.theme = t; }
    public String getIslandType() { return islandType != null ? islandType : "DEFAULT"; }
    public void setIslandType(String t) { this.islandType = t != null ? t : "DEFAULT"; }

    public int getUpgradeLevel(String id) { return upgrades.getOrDefault(id, 0); }
    public void setUpgradeLevel(String id, int lvl) { upgrades.put(id, lvl); }
    public Map<String, Integer> getUpgrades() { return upgrades; }

    public String getActivePetId() { return activePetId; }
    public void setActivePetId(String id) { this.activePetId = id; }
    public Set<String> getOwnedPets() { return ownedPets; }
    public UUID getPetEntityId() { return petEntityId; }
    public void setPetEntityId(UUID id) { this.petEntityId = id; }

    public Set<UUID> getCoopMembers() { return coopMembers; }
    public void addCoopMember(UUID uuid) { coopMembers.add(uuid); }
    public void removeCoopMember(UUID uuid) { coopMembers.remove(uuid); }

    public double getBoosterMultiplier() {
        if (System.currentTimeMillis() > boosterEndTime) { boosterMultiplier = 1.0; boosterEndTime = 0; }
        return boosterMultiplier;
    }
    public void setBooster(double mult, int durationSeconds) {
        this.boosterMultiplier = mult;
        this.boosterEndTime = System.currentTimeMillis() + (durationSeconds * 1000L);
    }
    public long getBoosterEndTime() { return boosterEndTime; }
    public void setBoosterEndTime(long t) { this.boosterEndTime = t; }
    public void setBoosterMultiplier(double m) { this.boosterMultiplier = m; }
    public boolean hasActiveBooster() { return System.currentTimeMillis() < boosterEndTime && boosterMultiplier > 1.0; }
    public int getBoosterSecondsLeft() {
        long left = (boosterEndTime - System.currentTimeMillis()) / 1000;
        return left > 0 ? (int) left : 0;
    }

    public long getLastMilestone() { return lastMilestone; }
    public void setLastMilestone(long m) { this.lastMilestone = m; }

    public Map<UUID, Integer> getTrackedItems() { return trackedItems; }
    public List<Location> getConveyorPath() { return conveyorPath; }
    public void setConveyorPath(List<Location> p) { this.conveyorPath = p; }
    public Location getDropperLocation() { return dropperLocation; }
    public void setDropperLocation(Location l) { this.dropperLocation = l; }
    public Location getSellLocation() { return sellLocation; }
    public void setSellLocation(Location l) { this.sellLocation = l; }
    public List<Location> getExtraDropperLocations() { return extraDropperLocations; }
    public void setExtraDropperLocations(List<Location> l) { this.extraDropperLocations = l; }
    public int getDropperTickCounter() { return dropperTickCounter; }
    public void incrementDropperTick() { this.dropperTickCounter++; }
    public void resetDropperTick() { this.dropperTickCounter = 0; }
}
