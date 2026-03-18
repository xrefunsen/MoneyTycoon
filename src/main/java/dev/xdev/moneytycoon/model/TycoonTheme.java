package dev.xdev.moneytycoon.model;

import org.bukkit.Material;

public enum TycoonTheme {
    CLASSIC(Material.POLISHED_ANDESITE, Material.DEEPSLATE_BRICKS, Material.IRON_BLOCK,
            Material.ORANGE_CONCRETE, Material.GRAY_CONCRETE, Material.GOLD_BLOCK, Material.SMOOTH_STONE),
    MINE(Material.COBBLESTONE, Material.STONE_BRICKS, Material.COAL_BLOCK,
            Material.BROWN_CONCRETE, Material.STONE, Material.DIAMOND_BLOCK, Material.ANDESITE),
    FARM(Material.GRASS_BLOCK, Material.OAK_LOG, Material.HAY_BLOCK,
            Material.LIME_CONCRETE, Material.OAK_PLANKS, Material.EMERALD_BLOCK, Material.FARMLAND),
    NETHER(Material.NETHER_BRICKS, Material.BLACKSTONE, Material.MAGMA_BLOCK,
            Material.RED_CONCRETE, Material.NETHER_BRICKS, Material.GILDED_BLACKSTONE, Material.CRIMSON_PLANKS),
    END(Material.END_STONE_BRICKS, Material.PURPUR_BLOCK, Material.OBSIDIAN,
            Material.PURPLE_CONCRETE, Material.PURPUR_SLAB, Material.END_STONE, Material.PURPUR_PILLAR),
    ROYAL(Material.QUARTZ_BLOCK, Material.QUARTZ_BRICKS, Material.GOLD_BLOCK,
            Material.YELLOW_CONCRETE, Material.QUARTZ_SLAB, Material.BEACON, Material.QUARTZ_PILLAR);

    public final Material floor;
    public final Material wall;
    public final Material pillar;
    public final Material conveyor;
    public final Material conveyorSide;
    public final Material sell;
    public final Material accent;

    TycoonTheme(Material floor, Material wall, Material pillar,
                Material conveyor, Material conveyorSide, Material sell, Material accent) {
        this.floor = floor;
        this.wall = wall;
        this.pillar = pillar;
        this.conveyor = conveyor;
        this.conveyorSide = conveyorSide;
        this.sell = sell;
        this.accent = accent;
    }
}
