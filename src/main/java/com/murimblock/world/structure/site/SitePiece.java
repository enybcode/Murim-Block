package com.murimblock.world.structure.site;

import com.murimblock.world.MurimWorldgen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One part of a Murim site. Its box covers every block the part places (so it is drawn whole, chunk by chunk) and
 * starts one block above its level, where the beard_thin adaptation lays the ground.
 */
public final class SitePiece extends StructurePiece {
    private final SitePlan plan;
    private final int part, ox, oz;
    private final Rotation rotation;
    private final long seed;
    private final int[] levels;

    public SitePiece(SitePlan plan, int part, int ox, int oz, Rotation rotation, long seed, int[] levels) {
        super(MurimWorldgen.SITE_PIECE.get(), 0, box(plan.site(), part, ox, oz, rotation, levels));
        this.plan = plan;
        this.part = part;
        this.ox = ox;
        this.oz = oz;
        this.rotation = rotation;
        this.seed = seed;
        this.levels = levels.clone();
    }

    public SitePiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(MurimWorldgen.SITE_PIECE.get(), tag);
        this.plan = SitePlan.byName(tag.getString("Plan"));
        this.part = tag.getInt("Part");
        this.ox = tag.getInt("OX");
        this.oz = tag.getInt("OZ");
        this.rotation = Rotation.valueOf(tag.getString("Rotation"));
        this.seed = tag.getLong("Seed");
        this.levels = tag.getIntArray("Levels");
    }

    static BoundingBox box(Site site, int part, int ox, int oz, Rotation rotation, int[] levels) {
        Site.Part p = site.parts().get(part);
        int y = levels[p.level()];
        return Canvas.dry(ox, oz, rotation).box(p.x0(), y + 1, p.z0(), p.x1(), y + p.height(), p.z1());
    }

    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Plan", plan.getSerializedName());
        tag.putInt("Part", part);
        tag.putInt("OX", ox);
        tag.putInt("OZ", oz);
        tag.putString("Rotation", rotation.name());
        tag.putLong("Seed", seed);
        tag.putIntArray("Levels", levels);
    }

    @Override public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                                      BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        plan.site().build(new Canvas(level, chunkBox, ox, oz, rotation, seed, null), part, levels);
    }

    public SitePlan plan() { return plan; }
    public int part() { return part; }
    public Rotation rotation() { return rotation; }
    public int[] levels() { return levels.clone(); }
    public BlockPos origin() { return new BlockPos(ox, levels[0], oz); }

    /** The world box of one part of a site placed here. */
    public static BoundingBox partBox(SitePlan plan, int part, int ox, int oz, Rotation rotation, int[] levels) {
        return box(plan.site(), part, ox, oz, rotation, levels);
    }

    /** What the checks must verify for this part (rooms to be roofed, footings to be carried), in world coordinates. */
    public List<Canvas.Check> checks() {
        List<Canvas.Check> checks = new ArrayList<>();
        plan.site().build(Canvas.dry(ox, oz, rotation, seed, checks), part, levels);
        return checks;
    }

    /** Builds a whole site at once in a live level (tests): its checks and the mobs it spawned. */
    public static Built buildWhole(ServerLevel level, SitePlan plan, int ox, int oz, Rotation rotation, long seed, int[] levels) {
        List<Canvas.Check> checks = new ArrayList<>();
        Canvas c = new Canvas(level, null, ox, oz, rotation, seed, checks);
        for (int i = 0; i < plan.site().parts().size(); i++) plan.site().build(c, i, levels);
        c.finish();
        return new Built(checks, c.spawned());
    }

    /** A site built whole (tests): what its checks must verify, and the mobs it brought. */
    public record Built(List<Canvas.Check> checks, List<Mob> spawned) { }

    /** The world box of every part of a site (tests). */
    public static BoundingBox wholeBox(SitePlan plan, int ox, int oz, Rotation rotation, int[] levels) {
        BoundingBox all = null;
        for (int i = 0; i < plan.site().parts().size(); i++) {
            BoundingBox b = box(plan.site(), i, ox, oz, rotation, levels);
            all = all == null ? b : BoundingBox.encapsulatingBoxes(List.of(all, b)).orElseThrow();
        }
        return all;
    }
}
