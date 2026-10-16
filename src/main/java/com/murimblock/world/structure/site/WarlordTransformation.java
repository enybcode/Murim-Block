package com.murimblock.world.structure.site;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.BanditWarlordAnimations;
import com.murimblock.integration.epicfight.ColossusAnimations;
import com.murimblock.mob.BanditWarlordEntity;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.SonOfTheDemonEntity;
import com.murimblock.network.TransformationPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * The Bandit Warlord becomes the Son of the Demon (Enzo, 16/10/2026; docs/mobs/bandit-warlord.md, the storyboard): a
 * thirty-four-second film, everyone near frozen and unhurt while it plays, every sound of it (the terrifying scream, the
 * ground struck, the head struck, the armour bursting, the flash, the chest beaten, the aura, the earth tearing, the wind
 * of the rise, the title). The server keeps the time; each player in it sees the film (client/TransformationCinematic).
 * <p>In ticks from his fall to zero: the ground struck at {@link #PUNCH}, his head at {@link #HEAD_HITS}, his heart from
 * {@link #HEART} (its red beats, stopped at {@link #HEART_STOP}, turned to gold at {@link #HEART_GOLD}, its golden beats), the scream from
 * {@link #SCREAM}, his armour bursting at {@link #BURST}, the colossus at {@link #SWAP} (the flash), its chest beaten from
 * {@link #BEAT}, the aura at {@link #AURA}; then (Enzo, 17/10/2026) he grabs the viewer at {@link #GRIP}, leaps with him
 * at {@link #LIFT}, flies round the meteor ({@link #flight}) and throws him onto it at {@link #THROW} (each player in his
 * own film: the players are carried there, unseen, at {@link #CARRY}, and land at {@link #IMPACT}); he drops from the sky
 * from {@link #DROP} and lands in the middle of the bowl at {@link #SLAM}; the title at {@link #TITLE}, the fight at
 * {@link #END}. Server side he waits in the crater until his landing (his flight is drawn by the film itself, on this
 * same path), so he never leaves the loaded world.
 * The meteor (SkyArena) is placed in the sky over the camp from the start.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class WarlordTransformation {
    public static final int PUNCH = 37, HEART = 100, HEART_STOP = 142, HEART_GOLD = 158, SCREAM = 186, BURST = 236, SWAP = 286,
            BEAT = 348, AURA = 426, APPROACH = 456, GRAB = 496, GRIP = 508, LEAP = 520, LIFT = 534, THROW = 600, CARRY = 614,
            IMPACT = 618, DROP = 620, SLAM = 636, TITLE = 656, END = 716;
    /** How far he walks at the viewer before his blow (APPROACH to GRAB), slowly. */
    public static final double WALK = 5.5;
    public static final int[] HEAD_HITS = {69, 77, 85};
    /** His heart's last red beats, slowing, and its golden beats, quickening (the heart's shot: HEART to SCREAM). */
    public static final int[] HEART_BEATS = {104, 116, 130}, GOLD_BEATS = {168, 174, 180};
    /** His skin tearing, three stages (client/BanditWarlordRenderer: textures bandit_warlord_tear_1..3). */
    public static final int[] TEARS = {SCREAM + 10, SCREAM + 30, BURST};
    /** Who is in it: the players this near him when he falls. */
    public static final double REACH = 64;
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.25F), 2.5F);

    private static final class Film {
        final ServerLevel level;
        final UUID warlord;
        final List<UUID> players = new ArrayList<>();
        final BlockPos ground;
        final long camp;
        final boolean arena;
        final BlockPos arenaMiddle;
        int ticks;
        /** The way he faces (degrees): his leap goes that way, the viewer is thrown before him. */
        float facing;
        SonOfTheDemonEntity colossus;

        Film(ServerLevel level, BanditWarlordEntity w, long camp, boolean arena, BlockPos arenaMiddle) {
            this.level = level;
            this.warlord = w.getUUID();
            this.ground = w.blockPosition();
            this.camp = camp;
            this.arena = arena;
            this.arenaMiddle = arenaMiddle;
            this.facing = w.getYRot();
        }
    }

    private static final List<Film> FILMS = new ArrayList<>();

    private WarlordTransformation() { }

    public static boolean playing(BanditWarlordEntity w) {
        return FILMS.stream().anyMatch(f -> f.warlord.equals(w.getUUID()));
    }

    /** His life falls to zero: the film begins (the arena placed in the sky over his camp, or over him). */
    public static void begin(ServerLevel level, BanditWarlordEntity w) {
        if (playing(w)) return;
        Long campKey = w.camp();
        BlockPos at = campKey != null ? BlockPos.of(campKey) : w.blockPosition();
        boolean arena = SkyArena.place(level, at.getX(), at.getZ());
        Film film = new Film(level, w, campKey == null ? BanditCampCount.camp(at.getX(), at.getZ()) : campKey, arena,
                SkyArena.floorMiddle(level, at.getX(), at.getZ()));
        FILMS.add(film);
        w.beginTransformation();
        play(w, BanditWarlordAnimations.TRANSFORM);
        // Its tick 1 is this tick's end (onServerTick): tick zero is one before the world's time now.
        TransformationPayload payload = new TransformationPayload(w.getId(), film.ground, film.arenaMiddle, arena, level.getGameTime() - 1);
        for (ServerPlayer p : level.players()) if (p.distanceTo(w) <= REACH) {
            film.players.add(p.getUUID());
            freeze(p);
            PacketDistributor.sendToPlayer(p, payload);
        }
        // Everything stops.
        sound(level, film.ground, SoundEvents.WARDEN_HEARTBEAT, 3, 0.6F);
        sound(level, film.ground, SoundEvents.BEACON_DEACTIVATE, 3, 0.5F);
    }

    /** Still and unhurt for the whole film (their inputs are held by the film itself). */
    private static void freeze(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, END + 20, 10, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, END + 60, 4, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, END + 20, 10, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, END + 100, 0, false, false));
        // The meteor over the camp shades the crater: the film is lit as by day (long enough never to flicker).
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, END + 400, 0, false, false));
    }

    private static void play(net.minecraft.world.entity.LivingEntity e, yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<? extends yesman.epicfight.api.animation.types.StaticAnimation> a) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(e, LivingEntityPatch.class);
        if (patch != null) patch.playAnimationSynchronized(a, 0.0F);
    }

    private static void sound(ServerLevel level, BlockPos at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at, sound, SoundSource.HOSTILE, volume, pitch);
    }

    private static void sound(ServerLevel level, BlockPos at, Holder<SoundEvent> sound, float volume, float pitch) {
        level.playSound(null, at, sound.value(), SoundSource.HOSTILE, volume, pitch);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (FILMS.isEmpty()) return;
        for (Film f : new ArrayList<>(FILMS)) {
            f.ticks++;
            try {
                step(f);
            } catch (RuntimeException e) {
                FILMS.remove(f);
                throw e;
            }
            if (f.ticks >= END) FILMS.remove(f);
        }
    }

    private static void step(Film f) {
        ServerLevel level = f.level;
        int t = f.ticks;
        BlockPos g = f.ground;
        Vec3 c = Vec3.atBottomCenterOf(g);
        BanditWarlordEntity w = level.getEntity(f.warlord) instanceof BanditWarlordEntity e ? e : null;
        if (w != null) w.setTransformTicks(t);
        // 2. The ground struck: a shock, golden cracks.
        if (t == PUNCH) {
            sound(level, g, SoundEvents.GENERIC_EXPLODE, 4, 0.45F);
            sound(level, g, SoundEvents.ANVIL_LAND, 3, 0.4F);
            BlockState under = level.getBlockState(g.below());
            if (!under.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), c.x, c.y + 0.2, c.z, 200, 3, 0.2, 3, 0.3);
            for (int i = 0; i < 40; i++) {
                double a = Math.PI * 2 * i / 40;
                level.sendParticles(GOLD, c.x + Math.cos(a) * 4, c.y + 0.1, c.z + Math.sin(a) * 4, 2, 0.2, 0, 0.2, 0);
            }
        }
        // 3. His own head, three times: a dull blow, a heartbeat.
        for (int hit : HEAD_HITS) if (t == hit) {
            sound(level, g, SoundEvents.ANVIL_PLACE, 2.5F, 0.5F);
            sound(level, g, SoundEvents.WARDEN_HEARTBEAT, 3, 0.8F);
        }
        // 4. His heart (the camera is inside his chest): its last red beats, slower and lower; it stops, silence; it turns to
        // gold with a sound of rebirth; it beats again, stronger and quicker, each beat a blow.
        for (int i = 0; i < HEART_BEATS.length; i++) if (t == HEART_BEATS[i]) sound(level, g, SoundEvents.WARDEN_HEARTBEAT, 5, 0.85F - 0.12F * i);
        if (t == HEART_STOP) sound(level, g, SoundEvents.BEACON_DEACTIVATE, 2, 0.5F);
        if (t == HEART_GOLD) {
            sound(level, g, SoundEvents.TOTEM_USE, 4, 0.6F);
            sound(level, g, SoundEvents.BEACON_ACTIVATE, 4, 0.7F);
            sound(level, g, SoundEvents.AMETHYST_BLOCK_RESONATE, 4, 0.5F);
        }
        for (int i = 0; i < GOLD_BEATS.length; i++) if (t == GOLD_BEATS[i]) {
            sound(level, g, SoundEvents.WARDEN_HEARTBEAT, 7, 0.95F + 0.12F * i);
            sound(level, g, SoundEvents.GENERIC_EXPLODE, 1.5F + i, 0.5F);
        }
        // His skin tearing as he screams: a wet rip, shreds of skin flying, gold light leaking out.
        for (int tear : TEARS) if (t == tear) {
            sound(level, g, SoundEvents.SLIME_SQUISH, 4, 0.5F);
            sound(level, g, SoundEvents.HONEY_BLOCK_BREAK, 4, 0.5F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TERRACOTTA.defaultBlockState()), c.x, c.y + 3, c.z, 50, 1.0, 1.4, 1.0, 0.25);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_TERRACOTTA.defaultBlockState()), c.x, c.y + 3, c.z, 30, 1.0, 1.4, 1.0, 0.25);
            level.sendParticles(GOLD, c.x, c.y + 3, c.z, 40, 1.2, 1.6, 1.2, 0.08);
        }
        // 5. The terrifying scream, rising, layered: the ravager, the warden, the dragon, the wither.
        if (t == SCREAM) {
            sound(level, g, SoundEvents.RAVAGER_ROAR, 5, 0.45F);
            sound(level, g, SoundEvents.WARDEN_ROAR, 5, 0.6F);
        }
        if (t == SCREAM + 20) sound(level, g, SoundEvents.ENDER_DRAGON_GROWL, 6, 0.55F);
        if (t == SCREAM + 40) { sound(level, g, SoundEvents.WARDEN_ROAR, 6, 0.5F); sound(level, g, SoundEvents.WITHER_AMBIENT, 4, 0.5F); }
        if (t == SCREAM + 70) sound(level, g, SoundEvents.ENDER_DRAGON_GROWL, 7, 0.45F);
        if (t > SCREAM && t < SWAP && t % 10 == 0) sound(level, g, SoundEvents.WARDEN_HEARTBEAT, 4, 0.9F + (t - SCREAM) / 200F);
        if (t > SCREAM && t < SWAP && t % 3 == 0) level.sendParticles(GOLD, c.x, c.y + 3, c.z, 8, 1.2, 1.5, 1.2, 0.05);
        // His armour bursting: iron plates flying off.
        if (t == BURST) {
            sound(level, g, SoundEvents.ITEM_BREAK, 4, 0.5F);
            sound(level, g, SoundEvents.GENERIC_EXPLODE, 3, 0.8F);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_CHESTPLATE)), c.x, c.y + 3, c.z, 60, 1, 1, 1, 0.4);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.RED_WOOL)), c.x, c.y + 3, c.z, 40, 1, 1, 1, 0.35);
        }
        // 6. The flash: the colossus bursts out of him.
        if (t == SWAP) {
            sound(level, g, SoundEvents.LIGHTNING_BOLT_THUNDER, 6, 0.6F);
            sound(level, g, SoundEvents.LIGHTNING_BOLT_IMPACT, 5, 0.5F);
            sound(level, g, SoundEvents.BEACON_ACTIVATE, 4, 0.5F);
            level.sendParticles(ParticleTypes.FLASH, c.x, c.y + 3, c.z, 3, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
            SonOfTheDemonEntity s = MurimEntities.SON_OF_THE_DEMON.get().create(level);
            if (s != null) {
                float yaw = f.facing;
                s.moveTo(c.x, c.y, c.z, yaw, 0);
                s.setYBodyRot(yaw);
                s.setYHeadRot(yaw);
                s.finalizeSpawn(level, level.getCurrentDifficultyAt(g), MobSpawnType.EVENT, null);
                s.setAwakening(true);
                s.setCamp(f.camp);
                level.addFreshEntity(s);
                f.colossus = s;
                play(s, ColossusAnimations.AWAKEN);
                sound(level, g, SoundEvents.RAVAGER_ROAR, 6, 0.4F);
            }
            if (w != null) w.discard();
        }
        // 7. The roar, then the chest beaten like a gorilla.
        if (t == SWAP + 25) { sound(level, g, SoundEvents.WARDEN_ROAR, 6, 0.45F); sound(level, g, SoundEvents.ENDER_DRAGON_GROWL, 5, 0.7F); }
        if (t == BEAT && f.colossus != null) play(f.colossus, ColossusAnimations.CHEST_BEAT);
        for (float beat : ColossusAnimations.BEATS) if (t == BEAT + Math.round(beat * 20)) {
            sound(level, g, SoundEvents.WARDEN_ATTACK_IMPACT, 4, 0.55F);
            sound(level, g, SoundEvents.ANVIL_LAND, 1.5F, 0.35F);
            for (int i = 0; i < 24; i++) {
                double a = Math.PI * 2 * i / 24;
                level.sendParticles(ParticleTypes.CLOUD, c.x + Math.cos(a) * 3, c.y + 4.5, c.z + Math.sin(a) * 3, 1, 0, 0, 0, 0.05);
            }
        }
        if (t == BEAT + 60) sound(level, g, SoundEvents.RAVAGER_ROAR, 6, 0.4F);
        // 8. The golden aura, the ground trembling.
        if (t == AURA) {
            sound(level, g, SoundEvents.BEACON_POWER_SELECT, 5, 0.5F);
            sound(level, g, SoundEvents.BLAZE_SHOOT, 4, 0.4F);
            // His power rising: a roar, arms flung wide, the light round him.
            if (f.colossus != null) play(f.colossus, ColossusAnimations.ROAR);
        }
        if (t >= AURA && t < GRAB && t % 2 == 0) {
            level.sendParticles(GOLD, c.x, c.y + 4, c.z, 30, 3, 4, 3, 0.1);
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 2, c.z, 10, 3, 2, 3, 0.15);
            if (t % 8 == 0) sound(level, g, SoundEvents.RAVAGER_STEP, 4, 0.4F);
        }
        // 9. He grabs the viewer (each in his own film), leaps and flies: the film shows it (its own sounds, heard by the
        // camera wherever it is); the crater only feels his leap.
        // (Each of his moves chained to the next: he is never seen standing idle.)
        // He walks at the viewer, slowly, the ground shaking under each step.
        if (t == APPROACH && f.colossus != null) play(f.colossus, ColossusAnimations.WALK);
        if (t > APPROACH && t <= GRAB && f.colossus != null && f.colossus.isAlive()) {
            Vec3 from = c.add(ahead(f).scale(WALK * (t - APPROACH - 1) / (GRAB - APPROACH))), to = c.add(ahead(f).scale(WALK * (t - APPROACH) / (GRAB - APPROACH)));
            f.colossus.setPos(to.x, f.colossus.getY(), to.z);
            if ((t - APPROACH) % 14 == 7) sound(level, BlockPos.containing(from), SoundEvents.RAVAGER_STEP, 3, 0.4F);
        }
        if (t == GRAB && f.colossus != null) play(f.colossus, ColossusAnimations.SMASH);
        if (t == GRIP && f.colossus != null) play(f.colossus, ColossusAnimations.GRAB);
        if (t == LEAP && f.colossus != null) play(f.colossus, ColossusAnimations.LEAP);
        if (t == LIFT) {
            sound(level, g, SoundEvents.GENERIC_EXPLODE, 6, 0.5F);
            BlockState under = level.getBlockState(g.below());
            if (!under.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), c.x, c.y + 0.3, c.z, 300, 4, 0.3, 4, 0.5);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.5, c.z, 2, 2, 0.2, 2, 0);
            for (int i = 0; i < 24; i++) throwRock(level, c);
        }
        if (t == LIFT + 4 && f.colossus != null) play(f.colossus, ColossusAnimations.FLY);
        if (t == THROW - 8 && f.colossus != null) play(f.colossus, ColossusAnimations.THROW);
        if (t == THROW + 14 && f.colossus != null) play(f.colossus, ColossusAnimations.DIVE);
        // 10. The players carried onto the meteor, unseen, before they land in their films; their landing, hurled down
        // by him, breaks the floor: a small crater under each (it comes back like all broken ground).
        if (t == CARRY && f.arena) carry(f);
        if (t == IMPACT && f.arena) for (UUID id : f.players) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p == null || p.level() != level) continue;
            BlockPos under = p.blockPosition().below();
            BlockState floor = level.getBlockState(under);
            com.murimblock.combat.ShatteredGround.crater(level, under, 3.6, 1);
            level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.5, p.getZ(), 3, 1, 0.2, 1, 0);
            if (!floor.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), p.getX(), p.getY() + 0.3, p.getZ(), 160, 1.6, 0.3, 1.6, 0.4);
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY() + 0.5, p.getZ(), 20, 1.5, 0.3, 1.5, 0.03);
        }
        // His landing in the middle of the bowl: the blast.
        if (t == SLAM && f.colossus != null && f.colossus.isAlive()) {
            BlockPos at = f.arena ? f.arenaMiddle : g;
            Vec3 m = Vec3.atBottomCenterOf(at);
            f.colossus.teleportTo(level, m.x, m.y, m.z, java.util.Set.of(), f.facing, 0);
            f.colossus.setYBodyRot(f.facing);
            f.colossus.setYHeadRot(f.facing);
            play(f.colossus, ColossusAnimations.LAND);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, m.x, m.y + 0.5, m.z, 3, 2, 0.2, 2, 0);
            BlockState under = level.getBlockState(at.below());
            if (!under.isAir()) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), m.x, m.y + 0.3, m.z, 400, 6, 0.3, 6, 0.6);
            for (int i = 0; i < 48; i++) {
                double r = Math.PI * 2 * i / 48;
                level.sendParticles(ParticleTypes.CLOUD, m.x + Math.cos(r) * 4, m.y + 0.4, m.z + Math.sin(r) * 4, 0, Math.cos(r), 0.02, Math.sin(r), 1.2);
            }
        }
        if (t == SLAM + 26 && f.colossus != null) play(f.colossus, ColossusAnimations.ROAR);
        // 11. The title: a roar, a horn.
        if (t == TITLE) {
            if (f.colossus != null) f.colossus.revealBar();
            BlockPos at = f.arena ? f.arenaMiddle : g;
            sound(level, at, SoundEvents.ENDER_DRAGON_GROWL, 8, 0.4F);
            sound(level, at, SoundEvents.WITHER_SPAWN, 6, 0.6F);
        }
        if (t == END) {
            if (f.colossus != null && f.colossus.isAlive()) f.colossus.setAwakening(false);
            for (UUID id : f.players) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
                if (p == null) continue;
                p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                p.removeEffect(MobEffects.WEAKNESS);
                p.removeEffect(MobEffects.NIGHT_VISION);
                // Facing him as the fight begins.
                if (f.colossus != null && f.colossus.isAlive() && p.level() == level) {
                    Vec3 d = f.colossus.position().add(0, 4, 0).subtract(p.getEyePosition());
                    p.teleportTo(level, p.getX(), p.getY(), p.getZ(), (float) Math.toDegrees(Math.atan2(-d.x, d.z)),
                            (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
                }
            }
        }
    }

    /** A rock torn out of the crater and thrown up (it vanishes where it falls: nothing is placed). */
    private static void throwRock(ServerLevel level, Vec3 c) {
        double a = level.random.nextDouble() * Math.PI * 2, d = 10 + level.random.nextDouble() * 22;
        BlockPos from = BlockPos.containing(c.x + Math.cos(a) * d, c.y, c.z + Math.sin(a) * d);
        BlockState state = level.random.nextBoolean() ? Blocks.STONE.defaultBlockState() : level.random.nextBoolean() ? Blocks.DIRT.defaultBlockState() : Blocks.BASALT.defaultBlockState();
        FallingBlockEntity rock = new FallingBlockEntity(EntityType.FALLING_BLOCK, level);
        rock.moveTo(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5);
        rock.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.4, 1.2 + level.random.nextDouble() * 1.1, (level.random.nextDouble() - 0.5) * 0.4);
        // Its block and its drop are only written through its save: a rock that vanishes where it lands.
        net.minecraft.nbt.CompoundTag tag = rock.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        tag.put("BlockState", net.minecraft.nbt.NbtUtils.writeBlockState(state));
        tag.putInt("Time", 1);
        tag.putBoolean("DropItem", false);
        tag.putBoolean("CancelDrop", true);
        rock.load(tag);
        level.addFreshEntity(rock);
    }

    /**
     * Onto the meteor: the players round the middle of the bowl (where the colossus will land), some 22 blocks off, the
     * first before him (the way he faces: where the film throws the viewer), facing the middle, each on firm rock (never
     * in a fissure of lava: the floor is crossed by them).
     */
    private static void carry(Film f) {
        ServerLevel level = f.level;
        BlockPos m = f.arenaMiddle;
        double ahead = Math.toRadians(-f.facing);
        int n = Math.max(1, f.players.size()), i = 0;
        for (UUID id : f.players) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p == null || p.level() != level) continue;
            double a = ahead + Math.PI * 2 * i++ / n;
            BlockPos at = firm(level, m, 20, 30, a, 0);
            double x = at.getX() + 0.5, z = at.getZ() + 0.5;
            float yaw = (float) Math.toDegrees(Math.atan2(-(m.getX() + 0.5 - x), m.getZ() + 0.5 - z));
            p.clearFire();
            p.teleportTo(level, x, at.getY(), z, yaw, 0);
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false));
        }
    }

    /** The way he faces (flat): he walks, leaps and flies that way. */
    static Vec3 ahead(Film f) {
        return Vec3.directionFromRotation(0, f.facing).multiply(1, 0, 1).normalize();
    }

    /** Where he leaps from: where he stood in the crater, after his walk at the viewer. */
    public static Vec3 leapFrom(Vec3 ground, Vec3 ahead) {
        return ground.add(ahead.scale(WALK));
    }

    /**
     * His flight with the viewer in his fist, from his leap ({@code u} 0) to his throw (1): out of the crater ahead of
     * him, up and wide round the meteor (never through its rock: it stands 97 blocks wide round its middle, its cone below),
     * over its rim and above its bowl. {@code start} where he stands in the crater, {@code middle} the bowl's middle,
     * {@code ahead} the way he faces (flat). The film draws him and the viewer on it.
     */
    public static Vec3 flight(Vec3 start, Vec3 middle, Vec3 ahead, double u) {
        // Never more than some 100 blocks from the bowl's middle (the game keeps the meteor drawn round the camera), wide
        // of its widest (97) while beside it, high over its walls (47) when over them.
        // (Checked against the schematic's blocks: some ten blocks clear of them at the closest, the crater 130 to 300
        // blocks below the floor; its rise out of the crater scaled to that drop.)
        Vec3 p1 = start.add(ahead.scale(150)).add(0, Math.max(30, 0.4 * (middle.y - start.y)), 0);
        Vec3 p2 = middle.add(ahead.scale(140)).add(0, 160, 0);
        Vec3 p3 = middle.add(ahead.scale(45)).add(0, 62, 0);
        double v = 1 - u;
        return start.scale(v * v * v).add(p1.scale(3 * u * v * v)).add(p2.scale(3 * u * u * v)).add(p3.scale(u * u * u));
    }

    /**
     * Where he is at the film's tick {@code t} from his leap to his landing, never still: his leap out of the crater,
     * quick at first and slowing as he climbs ({@link #flight}, LIFT to THROW), then on without a pause, a little higher
     * still and falling ever faster into the middle of the bowl (THROW to SLAM). The film draws him there, the server
     * keeps the same clock.
     */
    public static Vec3 airborne(Vec3 start, Vec3 middle, Vec3 ahead, double t) {
        if (t <= THROW) {
            double k = Math.max(0, (t - LIFT) / (THROW - LIFT));
            return flight(start, middle, ahead, 1 - (1 - k) * (1 - k) * 0.7 - (1 - k) * 0.3);
        }
        Vec3 top = flight(start, middle, ahead, 1);
        double k = Math.min(1, (t - THROW) / (SLAM - THROW));
        return new Vec3(top.x + (middle.x - top.x) * k, top.y + 14 * k * (1 - k) + (middle.y - top.y) * k * k, top.z + (middle.z - top.z) * k);
    }

    /**
     * The nearest place to stand on firm rock, from {@code from}, {@code r0} to {@code r1} blocks off, about angle
     * {@code a} (any angle if r0 is 0): rock under it (no lava, magma or fire) for {@code half} blocks round, air for
     * nine blocks above. Falls back on {@code from}.
     */
    static BlockPos firm(ServerLevel level, BlockPos from, int r0, int r1, double a, int half) {
        for (int r = r0; r <= r1; r++) {
            int steps = r == 0 ? 1 : Math.max(8, r * 6);
            for (int k = 0; k < steps; k++) {
                // Outward from the angle wanted, alternately to either side.
                double turn = (k + 1) / 2 * (Math.PI * 2 / steps) * (k % 2 == 0 ? 1 : -1);
                BlockPos at = BlockPos.containing(from.getX() + 0.5 + Math.sin(a + turn) * r, from.getY(), from.getZ() + 0.5 + Math.cos(a + turn) * r);
                if (firmAt(level, at, half)) return at;
            }
        }
        return from;
    }

    private static boolean firmAt(ServerLevel level, BlockPos at, int half) {
        for (int dx = -half; dx <= half; dx++) for (int dz = -half; dz <= half; dz++) {
            BlockPos p = at.offset(dx, 0, dz);
            BlockState under = level.getBlockState(p.below());
            if (!under.isSolidRender(level, p.below()) || under.is(Blocks.MAGMA_BLOCK) || !under.getFluidState().isEmpty()) return false;
            for (int y = 0; y < 9; y++) {
                BlockState above = level.getBlockState(p.above(y));
                if (!above.isAir()) return false;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FILMS.clear();
    }
}
