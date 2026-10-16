package com.murimblock.combat;

import com.murimblock.Murimblock;
import com.murimblock.world.structure.site.BanditCampCount;
import com.murimblock.world.structure.site.SitePiece;
import com.murimblock.world.structure.site.SitePlan;
import com.murimblock.world.structure.site.SiteStructure;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Ground broken by the Mountain-Shattering Fist and its Bandit Warlord (Enzo, 16/10/2026: "the blocks come back"; "the crater
 * stays fifteen seconds all the same"): a blow breaks real blocks (the ground cracks, walls burst), and each one comes back as it was
 * {@link #RESTORE_TICKS} later, bottom first, as soon as nothing stands where it was. Nothing drops: the art does not mine.
 * Saved per world, so a stop before they are back loses nothing.
 * <p>What never breaks ({@link #breakable}): anything that holds things or has a block entity, what nothing can break
 * (bedrock) or what is as hard as obsidian, fluids; nothing inside a Murim site (a temple, a camp, an arena...), except
 * the crater of a bandit camp whose warlord has fallen (his arena); nothing at all with the game rule
 * {@link #RULE} off (a server protecting its players' bases).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class ShatteredGround extends SavedData {
    /** The game rule: whether Murim blows break the ground (on by default). */
    public static final GameRules.Key<GameRules.BooleanValue> RULE =
            GameRules.register("murimDestruction", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
    /** How long a broken block stays broken (ticks): fifteen seconds. */
    public static final int RESTORE_TICKS = 15 * 20;
    static final String NAME = "murimblock_shattered_ground";
    public static final SavedData.Factory<ShatteredGround> FACTORY = new SavedData.Factory<>(ShatteredGround::new, ShatteredGround::load);

    private record Broken(BlockPos pos, BlockState state, long at) { }

    /** The broken blocks, by place (a block broken twice keeps its first state). */
    private final Map<BlockPos, Broken> broken = new HashMap<>();

    /** Loads the rule (the class) at mod construction, before any world. */
    public static void init() { }

    public static ShatteredGround get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    /** Whether a Murim blow may break this block. */
    public static boolean breakable(ServerLevel level, BlockPos pos) {
        if (!level.getGameRules().getBoolean(RULE) || !level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) return false;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness >= 50) return false;
        return !protectedSite(level, pos);
    }

    /** Inside a Murim site, but for a bandit camp whose warlord has fallen (its crater is his arena). */
    static boolean protectedSite(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos,
                h -> h.value() instanceof SiteStructure);
        if (!start.isValid()) return false;
        if (start.getStructure() instanceof SiteStructure site && site.plan() == SitePlan.BANDIT_CAMP) {
            for (StructurePiece p : start.getPieces())
                if (p instanceof SitePiece piece)
                    return !BanditCampCount.warlordCame(level, BanditCampCount.camp(piece.origin().getX(), piece.origin().getZ()));
        }
        return true;
    }

    /**
     * Breaks this block if a blow may (no drop; a burst of its dust), to come back {@link #RESTORE_TICKS} later. Returns whether it broke.
     */
    public static boolean smash(ServerLevel level, BlockPos pos) {
        if (!breakable(level, pos)) return false;
        BlockState state = level.getBlockState(pos);
        ShatteredGround data = get(level);
        BlockPos at = pos.immutable();
        data.broken.putIfAbsent(at, new Broken(at, state, level.getGameTime()));
        data.setDirty();
        level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.15);
        return true;
    }

    /**
     * A bowl {@code radius} blocks round and {@code depth} deep under {@code ground} (the block a body stands on), and
     * whatever stands in it up to two blocks above: broken. Returns how many blocks broke.
     */
    public static int crater(ServerLevel level, BlockPos ground, double radius, int depth) {
        int n = 0, r = (int) Math.ceil(radius);
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d > radius) continue;
            int h = (int) Math.round(depth * (1 - (d / radius) * (d / radius)));
            for (int y = 2; y > -h; y--) if (smash(level, ground.offset(x, y, z))) n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- coming back

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 10 != 0) return;
        ShatteredGround data = get(level);
        if (data.broken.isEmpty()) return;
        long now = level.getGameTime();
        List<Broken> due = new ArrayList<>();
        for (Broken b : data.broken.values()) if (now - b.at >= RESTORE_TICKS && level.isLoaded(b.pos)) due.add(b);
        if (due.isEmpty()) return;
        due.sort(Comparator.comparingInt(b -> b.pos.getY()));
        for (Broken b : due) {
            BlockState there = level.getBlockState(b.pos);
            // Something put there since (a player's block): it stays; someone standing there: later.
            if (!there.isAir() && !there.canBeReplaced()) { data.broken.remove(b.pos); continue; }
            if (!level.getEntitiesOfClass(LivingEntity.class, new AABB(b.pos)).isEmpty()) continue;
            level.setBlock(b.pos, b.state, 3);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, b.state), b.pos.getX() + 0.5, b.pos.getY() + 0.5, b.pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
            data.broken.remove(b.pos);
        }
        data.setDirty();
    }

    /** How many blocks wait to come back (tests). */
    public static int waiting(ServerLevel level) {
        return get(level).broken.size();
    }

    /** Tests: brings every broken block back now. */
    public static void restoreAllForTests(ServerLevel level) {
        ShatteredGround data = get(level);
        List<Broken> all = new ArrayList<>(data.broken.values());
        all.sort(Comparator.comparingInt(b -> b.pos.getY()));
        for (Broken b : all) if (level.getBlockState(b.pos).isAir()) level.setBlock(b.pos, b.state, 3);
        data.broken.clear();
        data.setDirty();
    }

    // ---------------------------------------------------------------- saving

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Broken b : broken.values()) {
            CompoundTag c = new CompoundTag();
            c.put("Pos", NbtUtils.writeBlockPos(b.pos));
            c.put("State", NbtUtils.writeBlockState(b.state));
            c.putLong("At", b.at);
            list.add(c);
        }
        tag.put("broken", list);
        return tag;
    }

    private static ShatteredGround load(CompoundTag tag, HolderLookup.Provider registries) {
        ShatteredGround data = new ShatteredGround();
        var blocks = registries.lookupOrThrow(Registries.BLOCK);
        for (Tag t : tag.getList("broken", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            BlockPos pos = NbtUtils.readBlockPos(c, "Pos").orElse(null);
            if (pos == null) continue;
            data.broken.put(pos, new Broken(pos, NbtUtils.readBlockState(blocks, c.getCompound("State")), c.getLong("At")));
        }
        return data;
    }
}
