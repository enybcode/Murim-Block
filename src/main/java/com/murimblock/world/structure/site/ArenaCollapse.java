package com.murimblock.world.structure.site;

import com.murimblock.Murimblock;
import com.murimblock.combat.ShatteredGround;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The arena in the sky goes when the Son of the Demon dies (Enzo, 17/10/2026: "it disappears after the fight"): his loot
 * falls on its floor; from {@link #RUMBLE} the meteor shakes and crumbles (its rumble, rocks falling from under it); at
 * {@link #DOWN} everyone on it and everything lying on it (his loot) is set down gently on the ground of the camp below,
 * the ground broken in the fight forgotten (it would come back in mid-air), and the meteor taken away block by block
 * (SkyArena.remove). Not saved: a server stopped meanwhile keeps the meteor.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class ArenaCollapse {
    /** Ticks after his death: the rumble begins; everyone and his loot are set down and the meteor goes. */
    public static final int RUMBLE = 100, DOWN = 160;

    private static final class Collapse {
        final ServerLevel level;
        final BlockPos middle;
        int ticks;

        Collapse(ServerLevel level, BlockPos middle) {
            this.level = level;
            this.middle = middle;
        }
    }

    private static final List<Collapse> COLLAPSES = new ArrayList<>();

    private ArenaCollapse() { }

    /** He is dead: the meteor whose floor's middle is {@code middle} will go. */
    public static void begin(ServerLevel level, BlockPos middle) {
        for (Collapse c : COLLAPSES) if (c.middle.equals(middle)) return;
        COLLAPSES.add(new Collapse(level, middle));
    }

    public static boolean collapsing() {
        return !COLLAPSES.isEmpty();
    }

    /** The meteor's space, round its floor's middle (the schematic's size, a margin round it). */
    public static AABB space(BlockPos middle) {
        return new AABB(middle).inflate(110, 0, 110).expandTowards(0, 70, 0).expandTowards(0, -125, 0);
    }

    /** The ground under the meteor: the first firm block below its cone, at its middle. */
    public static BlockPos groundBelow(ServerLevel level, BlockPos middle) {
        int y = Math.max(level.getMinBuildHeight() + 1, Math.min(middle.getY() - 125, level.getMaxBuildHeight() - 1));
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(middle.getX(), y, middle.getZ());
        if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
            // In the ground already (a mountain, a shallow world): up to its top, short of the meteor.
            while (p.getY() < middle.getY() - 4 && !level.getBlockState(p).getCollisionShape(level, p).isEmpty()) p.move(0, 1, 0);
            return p.immutable();
        }
        while (p.getY() > level.getMinBuildHeight() && level.getBlockState(p).getCollisionShape(level, p).isEmpty()) p.move(0, -1, 0);
        return p.above().immutable();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (COLLAPSES.isEmpty()) return;
        for (Collapse c : new ArrayList<>(COLLAPSES)) {
            c.ticks++;
            step(c);
            if (c.ticks >= DOWN) COLLAPSES.remove(c);
        }
    }

    private static void step(Collapse c) {
        ServerLevel level = c.level;
        BlockPos m = c.middle;
        if (c.ticks >= RUMBLE && c.ticks < DOWN) {
            // The meteor crumbling: its rumble, dust and rocks falling from under it.
            if (c.ticks % 8 == 0) level.playSound(null, m, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 6, 0.4F + level.random.nextFloat() * 0.2F);
            if (c.ticks % 12 == 0) level.playSound(null, m, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 6, 0.5F);
            for (int i = 0; i < 6; i++) {
                double a = level.random.nextDouble() * Math.PI * 2, d = level.random.nextDouble() * 70;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BASALT.defaultBlockState()),
                        m.getX() + Math.cos(a) * d, m.getY() + 1, m.getZ() + Math.sin(a) * d, 6, 1, 0.2, 1, 0.1);
            }
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, m.getX() + 0.5, m.getY() - 20, m.getZ() + 0.5, 10, 40, 20, 40, 0.02);
        }
        if (c.ticks == DOWN) {
            AABB space = space(m);
            BlockPos ground = groundBelow(level, m);
            int i = 0;
            for (ServerPlayer p : level.players()) {
                if (!space.contains(p.position())) continue;
                double a = Math.PI * 2 * i++ / 8, x = ground.getX() + 0.5 + Math.sin(a) * 4, z = ground.getZ() + 0.5 + Math.cos(a) * 4;
                p.teleportTo(level, x, ground.getY() + 12, z, p.getYRot(), p.getXRot());
                p.fallDistance = 0;
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
            }
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, space)) {
                item.teleportTo(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5);
                item.setDeltaMovement(0, 0, 0);
            }
            ShatteredGround.forget(level, space);
            SkyArena.remove(level, m);
            level.playSound(null, ground, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 4, 0.5F);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        COLLAPSES.clear();
    }
}
