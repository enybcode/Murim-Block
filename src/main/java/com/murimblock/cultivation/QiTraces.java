package com.murimblock.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.network.CultivationPayloads;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Qi vision's hidden traces, from Nascent Soul (docs/cultivation/refonte-cultivation.md, 18/10/2026): the powerful (a
 * cultivator or a creature from {@link #STRONG} on: bosses, strong players) leave a trail of Qi where they walk (a mote
 * every 4 to 8 blocks, at random), which fades in {@link #LIFE_TICKS}. A reader at {@link #REALM}, Qi Sense open, is sent those within {@link #REACH} (but their
 * own) each second, to be shown in the walker's aura colour; with them, the caches within {@link #CACHE_REACH}: a chest
 * or barrel hidden from their sight (buried, walled in) with something in it, or loot not yet drawn.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class QiTraces {
    /** The realm the hidden is seen from. */
    public static final CultivationRealm REALM = CultivationRealm.NASCENT_SOUL;
    /** From this realm on, one leaves traces. */
    public static final CultivationRealm STRONG = CultivationRealm.QI_CONDENSATION;
    /** How long a mark of a trail lasts (15 minutes). */
    public static final int LIFE_TICKS = 18000;
    /** How far they are seen (blocks). */
    public static final double REACH = 48;
    /** A mark of the trail each time one has walked this far: between these, drawn anew each time (blocks). */
    static final double STRIDE_MIN = 4, STRIDE_MAX = 8;
    /** At most this many sent at once, the newest; and kept by dimension. */
    static final int SENT = 32, KEPT = 2000;

    /** How far a hidden cache is felt (blocks). */
    public static final int CACHE_REACH = 16;

    /** A point of a trail: where, which way, when, whose (and their art, empty for a creature). */
    record Print(Vec3 at, float yaw, long tick, UUID who, String art, double gap) {
    }

    private static final Map<ResourceKey<Level>, ArrayDeque<Print>> PRINTS = new HashMap<>();
    private static final Map<UUID, Print> LAST = new HashMap<>();

    private QiTraces() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity e) || e.level().isClientSide || e.tickCount % 5 != 0) return;
        if (!e.onGround() || !e.isAlive() || e.isSpectator()) return;
        if (leavesTraces(e)) step(e, e.level().getGameTime());
    }

    /** Whether this one is powerful enough to leave traces. */
    public static boolean leavesTraces(LivingEntity e) {
        Optional<CultivationData> data = QiSense.cultivationOf(e);
        return data.isPresent() && data.get().realm().ordinal() >= STRONG.ordinal();
    }

    /** A mark of the trail where this one stands, if it has walked the gap drawn at the last. */
    public static void step(LivingEntity e, long now) {
        Print last = LAST.get(e.getUUID());
        if (last != null && last.at.distanceToSqr(e.position()) < last.gap * last.gap && now - last.tick < LIFE_TICKS) return;
        String art = e instanceof ServerPlayer p ? MeditationService.aura(p).auraArt().map(Object::toString).orElse("none") : "";
        double gap = STRIDE_MIN + e.getRandom().nextDouble() * (STRIDE_MAX - STRIDE_MIN);
        Print print = new Print(e.position(), e.getYRot(), now, e.getUUID(), art, gap);
        LAST.put(e.getUUID(), print);
        ArrayDeque<Print> prints = PRINTS.computeIfAbsent(e.level().dimension(), k -> new ArrayDeque<>());
        prints.addLast(print);
        while (prints.size() > KEPT || !prints.isEmpty() && now - prints.peekFirst().tick > LIFE_TICKS) {
            Print gone = prints.pollFirst();
            if (LAST.get(gone.who) == gone) LAST.remove(gone.who);
        }
    }

    /** The points of the trails this reader sees: within reach, not their own, the newest first; each walker numbered. */
    public static List<CultivationPayloads.Trace> seen(ServerPlayer reader) {
        List<CultivationPayloads.Trace> out = new ArrayList<>();
        ArrayDeque<Print> prints = PRINTS.get(reader.level().dimension());
        if (prints == null) return out;
        long now = reader.level().getGameTime();
        Map<UUID, Integer> walkers = new HashMap<>();
        for (Iterator<Print> it = prints.descendingIterator(); it.hasNext() && out.size() < SENT; ) {
            Print p = it.next();
            if (now - p.tick > LIFE_TICKS) break;
            if (p.who.equals(reader.getUUID()) || p.at.distanceToSqr(reader.position()) > REACH * REACH) continue;
            out.add(new CultivationPayloads.Trace((float) p.at.x, (float) p.at.y, (float) p.at.z, p.yaw, (int) (now - p.tick), p.art,
                    walkers.computeIfAbsent(p.who, k -> walkers.size())));
        }
        return out;
    }

    /** Each second, to a reader at Nascent Soul with Qi Sense open (QiSense.tick): the trails around. */
    public static void tick(ServerPlayer reader) {
        if (reader.tickCount % 20 != 0 || CultivationService.getRealm(reader).ordinal() < REALM.ordinal()) return;
        if (reader.connection != null) PacketDistributor.sendToPlayer(reader, new CultivationPayloads.Traces(seen(reader), caches(reader)));
    }

    /**
     * The caches this reader feels: chests and barrels within reach, hidden from their eyes, holding something (or loot
     * not yet drawn, never drawn here); for each, the first air above it, where its Qi comes out.
     */
    public static List<net.minecraft.core.BlockPos> caches(ServerPlayer reader) {
        List<net.minecraft.core.BlockPos> out = new ArrayList<>();
        var level = reader.serverLevel();
        Vec3 eye = reader.getEyePosition();
        int cx = reader.blockPosition().getX() >> 4, cz = reader.blockPosition().getZ() >> 4;
        for (int x = cx - 1; x <= cx + 1; x++) for (int z = cz - 1; z <= cz + 1; z++) {
            if (!level.hasChunk(x, z)) continue;
            for (var be : level.getChunk(x, z).getBlockEntities().values()) {
                if (!(be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity) && !(be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity)) continue;
                var pos = be.getBlockPos();
                Vec3 centre = Vec3.atCenterOf(pos);
                if (centre.distanceToSqr(eye) > CACHE_REACH * CACHE_REACH || !holds(be)) continue;
                var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, centre, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, reader));
                if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK || hit.getBlockPos().equals(pos)) continue;
                var up = pos.above();
                for (int i = 0; i < 12 && !level.getBlockState(up).isAir(); i++) up = up.above();
                out.add(up);
                if (out.size() >= 12) return out;
            }
        }
        return out;
    }

    /** Whether a container holds something: loot not yet drawn (left so), or an item. */
    static boolean holds(net.minecraft.world.level.block.entity.BlockEntity be) {
        if (!(be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity box)) return false;
        if (box.getLootTable() != null) return true;
        return !box.isEmpty();
    }

    public static void clearForTests() {
        PRINTS.clear();
        LAST.clear();
    }
}
