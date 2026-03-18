package dev.xdev.moneytycoon.manager;

import dev.xdev.moneytycoon.MoneyTycoon;
import dev.xdev.moneytycoon.model.Tycoon;
import dev.xdev.moneytycoon.model.TycoonPet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;

import java.util.UUID;

public class PetManager {

    private final MoneyTycoon plugin;

    public PetManager(MoneyTycoon plugin) {
        this.plugin = plugin;
    }

    public void spawnPet(Player owner, Tycoon tycoon) {
        despawnPet(tycoon);

        String petId = tycoon.getActivePetId();
        if (petId == null) return;

        TycoonPet pet;
        try { pet = TycoonPet.valueOf(petId); }
        catch (IllegalArgumentException e) { return; }

        Location spawnLoc = owner.getLocation().add(1, 0, 1);
        Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, pet.mobType);

        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.customName(Component.text("★ " + pet.displayName + " ★").color(NamedTextColor.GOLD));
        entity.setCustomNameVisible(true);

        if (entity instanceof Mob mob) {
            mob.setAware(false);
        }
        if (entity instanceof Ageable ageable) {
            ageable.setBaby();
            ageable.setAgeLock(true);
        }
        if (entity instanceof Tameable tameable) {
            tameable.setTamed(true);
            tameable.setOwner(owner);
        }

        tycoon.setPetEntityId(entity.getUniqueId());
    }

    public void despawnPet(Tycoon tycoon) {
        UUID petId = tycoon.getPetEntityId();
        if (petId != null) {
            Entity entity = Bukkit.getEntity(petId);
            if (entity != null) entity.remove();
            tycoon.setPetEntityId(null);
        }
    }

    public void teleportPetToOwner(Tycoon tycoon) {
        UUID petId = tycoon.getPetEntityId();
        if (petId == null) return;

        Entity entity = Bukkit.getEntity(petId);
        Player owner = Bukkit.getPlayer(tycoon.getOwnerUUID());
        if (entity == null || owner == null) return;

        if (entity.getLocation().distanceSquared(owner.getLocation()) > 64) {
            entity.teleport(owner.getLocation().add(1, 0, 1));
        }
    }

    public boolean purchasePet(Player player, Tycoon tycoon, String petId) {
        double cost = plugin.getConfig().getDouble("pets." + petId + ".cost", 0);
        boolean vip = plugin.getConfig().getBoolean("pets." + petId + ".vip", false);

        if (vip && !player.hasPermission("moneytycoon.vip")) return false;
        if (tycoon.getOwnedPets().contains(petId)) return false;
        if (cost > 0 && !plugin.getEconomyManager().has(player.getUniqueId(), cost)) return false;

        if (cost > 0) plugin.getEconomyManager().withdraw(player.getUniqueId(), cost);
        tycoon.getOwnedPets().add(petId);
        plugin.getDatabaseManager().saveOwnedPet(player.getUniqueId(), petId);
        return true;
    }

    public void activatePet(Player player, Tycoon tycoon, String petId) {
        if (!tycoon.getOwnedPets().contains(petId)) return;
        tycoon.setActivePetId(petId);
        spawnPet(player, tycoon);
    }

    public double getPetBonus(Tycoon tycoon, String bonusType) {
        String petId = tycoon.getActivePetId();
        if (petId == null) return 0;

        TycoonPet pet;
        try { pet = TycoonPet.valueOf(petId); }
        catch (IllegalArgumentException e) { return 0; }

        if (pet.bonusType.equals("ALL_BONUS")) return pet.bonusValue;
        if (pet.bonusType.equals(bonusType)) return pet.bonusValue;
        return 0;
    }

    public boolean hasAutoCollectPet(Tycoon tycoon) {
        return getPetBonus(tycoon, "AUTO_COLLECT") > 0;
    }
}
