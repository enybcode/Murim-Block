package com.murimblock.world.structure;

import com.mojang.serialization.MapCodec;
import com.murimblock.world.MurimWorldgen;
import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * The Cloud Cliff Sanctuary as a registered structure, so that /locate structure knows it. It never generates by itself
 * (no structure set places it): {@link SanctuaryPlacement} builds the one sanctuary of the world, and
 * SanctuaryLocateMixin answers structure searches with its recorded position.
 */
public final class SanctuaryStructure extends Structure {
    public static final MapCodec<SanctuaryStructure> CODEC = simpleCodec(SanctuaryStructure::new);

    public SanctuaryStructure(StructureSettings settings) {
        super(settings);
    }

    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return Optional.empty();
    }

    @Override public StructureType<?> type() {
        return MurimWorldgen.SANCTUARY_STRUCTURE.get();
    }
}
