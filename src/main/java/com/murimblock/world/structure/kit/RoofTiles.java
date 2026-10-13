package com.murimblock.world.structure.kit;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** The three blocks of a {@link BuildKit#roof}: stairs for the slopes, slabs for the eaves, a full block for ridge and ceiling. */
public record RoofTiles(Block stairs, Block slab, Block ridge) {
    /** Green glazed tiles, for halls of rank: temples, sect halls, towers, gates. */
    public static final RoofTiles GREEN_GLAZED = new RoofTiles(Blocks.PRISMARINE_BRICK_STAIRS, Blocks.PRISMARINE_BRICK_SLAB, Blocks.DARK_PRISMARINE);
    /** Grey fired tiles, for ordinary buildings: dormitories, libraries, pavilions, houses. */
    public static final RoofTiles GREY = new RoofTiles(Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILES);
}
