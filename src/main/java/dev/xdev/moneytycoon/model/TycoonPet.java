package dev.xdev.moneytycoon.model;

import org.bukkit.entity.EntityType;

public enum TycoonPet {
    COLLECTOR("Collector Cat", EntityType.CAT, "AUTO_COLLECT", 1.0),
    SPEED("Speed Rabbit", EntityType.RABBIT, "DROPPER_SPEED", 0.15),
    VALUE("Value Fox", EntityType.FOX, "VALUE_MULT", 0.20),
    LUCKY("Lucky Parrot", EntityType.PARROT, "DOUBLE_DROP", 0.10),
    VIP_DRAGON("VIP Bee", EntityType.BEE, "ALL_BONUS", 0.15);

    public final String displayName;
    public final EntityType mobType;
    public final String bonusType;
    public final double bonusValue;

    TycoonPet(String displayName, EntityType mobType, String bonusType, double bonusValue) {
        this.displayName = displayName;
        this.mobType = mobType;
        this.bonusType = bonusType;
        this.bonusValue = bonusValue;
    }
}
