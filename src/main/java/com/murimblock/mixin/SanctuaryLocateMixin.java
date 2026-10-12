package com.murimblock.mixin;

import com.mojang.datafixers.util.Pair;
import com.murimblock.world.structure.SanctuaryPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Structure searches (/locate structure, and anything else asking the chunk generator) find the Cloud Cliff Sanctuary
 * where it was built: it is placed once by SanctuaryPlacement, not by a structure set the search could follow.
 */
@Mixin(ChunkGenerator.class)
public abstract class SanctuaryLocateMixin {
    @Inject(method = "findNearestMapStructure", at = @At("HEAD"), cancellable = true)
    private void murimblock$findSanctuary(ServerLevel level, HolderSet<Structure> structures, BlockPos pos, int searchRadius,
                                         boolean skipKnownStructures,
                                         CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> callback) {
        if (structures.size() != 1 || level.dimension() != Level.OVERWORLD) return;
        Holder<Structure> structure = structures.get(0);
        if (!structure.is(SanctuaryPlacement.STRUCTURE)) return;
        BlockPos centre = SanctuaryPlacement.get(level).centre();
        callback.setReturnValue(centre == null ? null : Pair.of(centre, structure));
    }
}
