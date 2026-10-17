package com.murimblock.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.ColossusAnimations;
import com.murimblock.mob.BanditWarlordEntity;
import com.murimblock.mob.SonOfTheDemonEntity;
import com.murimblock.network.TransformationPayload;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import static com.murimblock.world.structure.site.WarlordTransformation.*;

/**
 * The Bandit Warlord becoming the Son of the Demon, as the players near him see it: a film (Enzo, 16/10/2026: "see each
 * shot as a film, not an animation; you are the director"), on the server's timing (WarlordTransformation), 34 s:
 * <ol>
 *   <li><b>Defeat</b>: on his knees, beaten; low, a slow push in; the world drained and still.</li>
 *   <li><b>The fist</b>: in profile; his fist driven into the ground, the view jolts and punches in.</li>
 *   <li><b>The head</b>: close on his face as he strikes his own head, a white flash at each blow.</li>
 *   <li><b>The heart</b>: the camera plunges into his chest; in the dark his heart, a block of flesh, beats, slower,
 *   stops; silence; it turns to gold and beats again, each beat a shockwave of light.</li>
 *   <li><b>The scream</b>: out of a white flash, circling him as he screams like Hulk, swells, his skin tearing on gold
 *   light, his armour bursting.</li>
 *   <li><b>The flash</b>: white and gold; the colossus bursts out.</li>
 *   <li><b>The colossus</b>: low, a hero's angle, his whole height: the roar, the gorilla's chest beat.</li>
 *   <li><b>The aura</b>: the camera craning up with the golden light.</li>
 *   <li><b>The earth tears</b>: wide from outside the camp, the camera sinking so that the ground seems to rise, rocks
 *   thrown up.</li>
 *   <li><b>The rise</b>: far out in the sky, the camera sinking past the meteor through the sea of clouds: it seems to
 *   rise out of them (the players are carried up meanwhile, unseen); the wind.</li>
 *   <li><b>The title</b>: a hero's shot of him in the meteor's bowl, his title over his bar; then the camera glides back
 *   into the player's own eyes and the fight begins.</li>
 * </ol>
 * No player is drawn in it (no names floating in the shots), nothing of the game's screen but his bar at the end; the
 * camera never enters the ground (it rises over what would hide the subject).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class TransformationCinematic {
    /** Shot changes (ticks), on the server's beats. */
    private static final int FIST = PUNCH - 6, FACE = 66, PLUNGE = HEART - 10, EMERGE = SCREAM - 4, FLASH_END = SWAP + 20, HOME = END - 22, HOMED = END - 6, LAST = END + 8;
    /**
     * His punch in the viewer's face (Enzo, 17/10/2026): the fist coming at him from GRAB, the blow at KO, then black (a
     * knock-out: meanwhile, unseen, the camera shows the meteor to the game from above and from its side, so that it is
     * drawn whole before it is seen); he wakes from WAKE in the colossus's fist, already leaping, to AWAKE.
     */
    private static final int KO = GRAB + 9, WARM = KO + 14, WAKE = LIFT - 4, AWAKE = LIFT + 4;
    /**
     * Seen from outside (Enzo: "a third-person shot at the top, him carrying us, throwing us into the arena, our body
     * limp, falling"): from SEEN, the viewer's own body hanging limp in his fist high over the bowl, hurled down at THROW,
     * tumbling, crashing into the floor at IMPACT in a burst of rock; back in the viewer's eyes, dazed, from DAZED.
     */
    private static final int SEEN = THROW - 22, DAZED = IMPACT + 8;
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/gui/cinematic/glow.png");

    @Nullable private static Marker camera;
    private static BlockPos ground = BlockPos.ZERO, arena = BlockPos.ZERO;
    private static boolean rises;
    private static int warlordId, ticks;
    /** The world's time at the film's tick zero: its clock. */
    private static long start;
    private static float shake, flash, fov = 70, pulse, roll;
    private static Vec3 facing = new Vec3(0, 0, 1);
    /** The way his flight sets out (his facing when he grabs the viewer), as the server's path (WarlordTransformation.flight). */
    @Nullable private static Vec3 heading;
    @Nullable private static Vec3 smoothEye, smoothLook;
    @Nullable private static SonOfTheDemonEntity colossus;
    @Nullable private static SimpleSoundInstance wind;

    private TransformationCinematic() { }

    public static boolean active() {
        return camera != null;
    }

    public static void start(TransformationPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (FinishCinematic.active() || GrabCinematic.active() || WarlordCinematic.active()) return;
        ground = p.ground();
        arena = p.arena();
        rises = p.rises();
        warlordId = p.warlordId();
        start = p.start();
        ticks = 0;
        shake = flash = pulse = 0;
        fov = 55;
        colossus = null;
        heading = null;
        smoothEye = smoothLook = null;
        facing = mc.level.getEntity(warlordId) instanceof BanditWarlordEntity w
                ? Vec3.directionFromRotation(0, w.getYRot()).multiply(1, 0, 1).normalize()
                : mc.player.position().subtract(Vec3.atBottomCenterOf(ground)).multiply(1, 0, 1).normalize();
        if (facing.lengthSqr() < 1e-4) facing = new Vec3(0, 0, 1);
        camera = new Marker(EntityType.MARKER, mc.level);
        mc.setCameraEntity(camera);
    }

    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (camera == null) return;
        camera = null;
        colossus = null;
        shake = flash = pulse = 0;
        if (wind != null) { mc.getSoundManager().stop(wind); wind = null; }
        if (mc.player != null) mc.setCameraEntity(mc.player);
        fadeIn = FADE;
    }

    /** Ticks of the fade back into the game, after the film (it hides the game redrawing round the player). */
    private static final int FADE = 8;
    private static int fadeIn;

    @SubscribeEvent
    public static void onTickAfter(ClientTickEvent.Post event) {
        if (camera == null && fadeIn > 0) fadeIn--;
    }

    @SubscribeEvent
    public static void onGuiAfter(RenderGuiEvent.Post event) {
        if (camera != null || fadeIn <= 0) return;
        float a = (fadeIn - event.getPartialTick().getGameTimeDeltaPartialTick(false)) / FADE;
        event.getGuiGraphics().fill(0, 0, event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight(), Math.round(Mth.clamp(a, 0, 1) * 255) << 24);
    }

    private static boolean is(int[] beats, int t) {
        for (int b : beats) if (b == t) return true;
        return false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (camera == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || camera.level() != mc.level) { stop(); return; }
        // On the world's clock: every beat passed since the last tick is played, none twice.
        int now = (int) Math.min(LAST + 1, mc.level.getGameTime() - start);
        while (ticks < now) beat(mc, ++ticks);
        if (ticks > LAST) stop();
    }

    /** What happens at the film's tick {@code t}: its jolts, flashes, sounds, and the decay of each. */
    private static void beat(Minecraft mc, int t) {
        if (t == PUNCH) { shake = 11; fov -= 9; }
        if (is(HEAD_HITS, t)) { shake = 6; flash = 0.45F; fov -= 3; }
        if (is(HEART_BEATS, t)) { pulse = 1; shake = 2; }
        if (t == HEART_GOLD) pulse = 0.6F;
        if (is(GOLD_BEATS, t)) { pulse = 1; shake = 7; }
        if (t == EMERGE) flash = 1;
        if (t > SCREAM && t < SWAP) shake = Math.max(shake, 1.2F + 3.0F * (t - SCREAM) / (SWAP - SCREAM));
        if (is(TEARS, t)) { shake = 7; flash = Math.max(flash, 0.25F); }
        if (t == SWAP) { flash = 1; shake = 16; }
        for (float beat : ColossusAnimations.BEATS) if (t == BEAT + Math.round(beat * 20)) { shake = 6; fov -= 2.5F; }
        if (t >= AURA && t < GRAB) shake = Math.max(shake, 1.0F);
        if (rises) carried(mc, t);
        if (t == TITLE) shake = 5;
        if (t >= SWAP && (colossus == null || colossus.isRemoved())) colossus = find(mc);
        shake *= 0.86F;
        flash *= t >= EMERGE && t < EMERGE + 12 ? 0.92F : 0.86F;
        pulse *= 0.8F;
    }

    /**
     * The grab, the flight, the throw and the fall, heard by the camera wherever it is (the world's sounds would be heard
     * at the players' feet, far below): a roar, the fist closing, the leap's blast, the wind, his scream in the air, the
     * throw, the landing, his fall and his landing.
     */
    private static void carried(Minecraft mc, int t) {
        if (t >= APPROACH && t < GRAB) {
            int since = t - APPROACH, every = Math.max(6, 14 - since / 5);
            if (since % every == 0) ui(mc, SoundEvents.WARDEN_HEARTBEAT, 1.0F + since / 80F, 0.9F);
            if (since % 18 == 4) ui(mc, SoundEvents.PLAYER_BREATH, 1.25F, 0.8F);
            if (since % 14 == 7) { shake = Math.max(shake, 4); ui(mc, SoundEvents.RAVAGER_STEP, 0.45F, 1.0F); }
        }
        if (t == GRAB) ui(mc, SoundEvents.RAVAGER_ROAR, 0.5F, 1.0F);
        if (t == KO - 3) ui(mc, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.0F);
        if (t == KO) {
            shake = 18;
            // The blow in the face: a crack, a thud, then the ringing and a muffled heart.
            ui(mc, SoundEvents.PLAYER_ATTACK_CRIT, 0.6F, 1.0F);
            ui(mc, SoundEvents.WARDEN_ATTACK_IMPACT, 0.5F, 1.0F);
            ui(mc, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.6F);
            ui(mc, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
        }
        if (t == KO + 10 || t == KO + 22) ui(mc, SoundEvents.WARDEN_HEARTBEAT, 0.6F, 0.7F);
        if (t == LIFT) {
            shake = 12;
            ui(mc, SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 1.0F);
            ui(mc, SoundEvents.WARDEN_SONIC_BOOM, 0.6F, 0.9F);
            wind = SimpleSoundInstance.forUI(SoundEvents.ELYTRA_FLYING, 0.8F, 1.0F);
            mc.getSoundManager().play(wind);
        }
        if (t == LIFT + 30) { ui(mc, SoundEvents.RAVAGER_ROAR, 0.45F, 1.0F); ui(mc, SoundEvents.WARDEN_ROAR, 0.6F, 0.8F); }
        if (t >= LIFT && t < THROW) shake = Math.max(shake, 1.6F);
        if (t == WAKE) shake = 8;
        if (t == THROW) { shake = 10; ui(mc, SoundEvents.ENDER_DRAGON_FLAP, 0.6F, 1.0F); ui(mc, SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 1.0F); }
        if (t == IMPACT) {
            shake = 16;
            flash = 0.35F;
            if (wind != null) { mc.getSoundManager().stop(wind); wind = null; }
            ui(mc, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.9F);
            ui(mc, SoundEvents.ANVIL_LAND, 0.5F, 0.8F);
            ui(mc, SoundEvents.PLAYER_BIG_FALL, 0.8F, 1.0F);
        }
        if (t == DROP + 4) ui(mc, SoundEvents.ENDER_DRAGON_FLAP, 0.45F, 1.0F);
        if (t == SLAM) {
            shake = 20;
            flash = 0.6F;
            ui(mc, SoundEvents.GENERIC_EXPLODE.value(), 0.4F, 1.0F);
            ui(mc, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.0F);
            ui(mc, SoundEvents.ANVIL_LAND, 0.35F, 0.8F);
        }
        if (t == SLAM + 28) { ui(mc, SoundEvents.RAVAGER_ROAR, 0.4F, 1.0F); ui(mc, SoundEvents.WARDEN_ROAR, 0.5F, 0.9F); ui(mc, SoundEvents.ENDER_DRAGON_GROWL, 0.6F, 0.8F); }
        // His aura goes with him in the air (the world's is at his place in the crater).
        if (colossus != null && t >= LIFT && t < SLAM) {
            Vec3 at = colossus.position();
            var r = mc.level.random;
            for (int i = 0; i < 6; i++)
                mc.level.addParticle(ParticleTypes.END_ROD, at.x + (r.nextDouble() - 0.5) * 4, at.y + r.nextDouble() * 7, at.z + (r.nextDouble() - 0.5) * 4, 0, -0.3, 0);
        }
    }

    private static void ui(Minecraft mc, net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    @Nullable private static SonOfTheDemonEntity find(Minecraft mc) {
        SonOfTheDemonEntity best = null;
        double nearest = Double.MAX_VALUE;
        Vec3 c = Vec3.atBottomCenterOf(ground);
        AABB box = new AABB(c, c).inflate(48).expandTowards(0, Math.max(0, arena.getY() - ground.getY()) + 16, 0);
        for (SonOfTheDemonEntity s : mc.level.getEntitiesOfClass(SonOfTheDemonEntity.class, box)) {
            double d = s.distanceToSqr(c);
            if (d < nearest) { nearest = d; best = s; }
        }
        return best;
    }

    /** Whoever is the subject now: the warlord, then the colossus. */
    @Nullable private static LivingEntity subject(Minecraft mc) {
        if (colossus != null && !colossus.isRemoved()) return colossus;
        return mc.level.getEntity(warlordId) instanceof BanditWarlordEntity w ? w : null;
    }

    private static float eased(float perFrame, float dt) {
        return 1 - (float) Math.pow(1 - perFrame, dt * 3);
    }

    private static float ease(float a, float b, float t) {
        float k = Mth.clamp((t - a) / (b - a), 0, 1);
        return k * k * (3 - 2 * k);
    }

    /** A point round {@code base}: {@code angle} degrees from his front (positive to his left), {@code dist} off, {@code height} up. */
    private static Vec3 orbit(Vec3 base, double dist, double angle, double height) {
        double a = Math.toRadians(angle);
        Vec3 side = new Vec3(-facing.z, 0, facing.x);
        return base.add(facing.scale(Math.cos(a) * dist)).add(side.scale(Math.sin(a) * dist)).add(0, height, 0);
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        if (camera == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { stop(); return; }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false), t = ticks + partial;
        LivingEntity who = subject(mc);
        roll = 0;
        if (heading == null && t >= APPROACH && colossus != null) heading = Vec3.directionFromRotation(0, colossus.getYRot()).multiply(1, 0, 1).normalize();
        if (rises && colossus != null && heading != null && t >= LIFT && t < SLAM) fly(colossus, t);
        Vec3 base = who == null ? Vec3.atBottomCenterOf(ground) : who.getPosition(partial);
        // Framed by the way he faces, now (he may be turned after the film began).
        if (who != null) facing = Vec3.directionFromRotation(0, who.getViewYRot(partial)).multiply(1, 0, 1).normalize();
        float swell = who instanceof BanditWarlordEntity w ? BanditWarlordRenderer.swell(w.transformTicks() + partial) : 1;
        Vec3 eye, look;
        float targetFov;
        boolean cut = false, clear = true;
        if (t < FIST) {
            // 1. Defeat: low before him, a little aside, pushing in slowly.
            float k = ease(0, FIST, t);
            eye = orbit(base, 8.5 - 2.0 * k, 28, 0.9);
            look = base.add(0, 1.5, 0);
            targetFov = 55;
        } else if (t < FACE) {
            // 2. The fist: in profile, close, low.
            float k = ease(FIST, FACE, t);
            eye = orbit(base, 6.4 - 0.6 * k, 84, 1.3);
            look = base.add(0, 1.1, 0).add(facing.scale(0.8));
            targetFov = 56;
            cut = t < FIST + 1;
        } else if (t < PLUNGE) {
            // 3. The head: close on his face, a little below it.
            eye = orbit(base, 3.8, 12, 3.0);
            look = base.add(0, 3.3, 0);
            targetFov = 52;
            cut = t < FACE + 1;
        } else if (t < HEART + 2) {
            // 4. Into his chest (the film goes black: the heart is drawn over it).
            float k = ease(PLUNGE, HEART, t);
            eye = orbit(base, 3.8 - 3.0 * k, 12 - 12 * k, 3.0 - 0.4 * k);
            look = base.add(0, 2.6, 0);
            targetFov = 52 - 14 * k;
            clear = false;
        } else if (t < EMERGE) {
            // (The heart: the world is not seen; the camera waits where the scream begins.)
            eye = orbit(base, 7.5, -60, 1.0);
            look = base.add(0, 2.7, 0);
            targetFov = 62;
            cut = true;
        } else if (t < SWAP) {
            // 5. The scream: circling him, drawing back and up as he swells.
            float k = ease(EMERGE, SWAP, t);
            eye = orbit(base, 7.5 + 4 * k, -60 + 200 * k, 1.0 + 1.6 * k);
            look = base.add(0, 2.7 * swell, 0);
            targetFov = 62 + 10 * k;
        } else if (t < FLASH_END) {
            // 6. The flash: wide, the colossus bursting out of the light.
            eye = orbit(base, 17, 20, 2.0);
            look = base.add(0, 4.2, 0);
            targetFov = 72;
            cut = t < SWAP + 1;
        } else if (t < BEAT - 4) {
            // 7. The colossus: low, his whole height, his roar.
            float k = ease(FLASH_END, BEAT - 4, t);
            eye = orbit(base, 13 - 1.5 * k, 16, 0.7);
            look = base.add(0, 5.0, 0);
            targetFov = 74;
            cut = t < FLASH_END + 1;
        } else if (t < AURA) {
            // 7b. The gorilla: before him at his chest's height, close, each blow on his chest a jolt.
            float k = ease(BEAT - 4, AURA, t);
            eye = orbit(base, 13 - 1.5 * k, -14, 3.6);
            look = base.add(0, 4.4, 0);
            targetFov = 66;
            cut = t < BEAT - 3;
        } else if (t < APPROACH || !rises && t < TITLE) {
            // 8. The aura: craning up and round with the light.
            float k = ease(AURA, rises ? APPROACH : TITLE, t);
            eye = orbit(base, 14, 16 - 40 * k, 1 + 8 * k);
            look = base.add(0, 4.6, 0);
            targetFov = 70;
        } else if (t < KO) {
            // 9. Through the viewer's own eyes, a man's height off the ground: he walks at us, slowly, towering, the view
            // trembling with each of his steps and our heart; we back away a little; then he raises his fist and brings
            // it down on us, filling the view.
            Vec3 start = Vec3.atBottomCenterOf(ground), out = heading == null ? facing : heading;
            float k = ease(APPROACH, GRAB, t), blow = ease(GRAB, KO, t);
            Vec3 stand = start.add(out.scale(9.6 + 0.7 * k));
            eye = stand.add(0, groundAt(mc, stand, base.y + 12) - stand.y + 1.7, 0)
                    .add(new Vec3(-out.z, 0, out.x).scale(0.08 * Mth.sin(t * 0.9F))).add(0, 0.05 * Mth.sin(t * 1.3F), 0);
            look = base.add(0, 5.4 - 2.4 * blow, 0);
            targetFov = 72 - 22 * blow;
            cut = t < APPROACH + 1;
            clear = false;
        } else if (t < WAKE) {
            // (Knocked out, the screen black: unseen, the meteor shown to the game from above, then from its side.)
            Vec3 m = Vec3.atBottomCenterOf(arena), out = heading == null ? facing : heading;
            if (t < WARM) { eye = m.add(out.scale(30)).add(0, 130, 0); look = m; }
            else { eye = m.add(out.scale(150)).add(0, -10, 0); look = m.add(0, -20, 0); }
            targetFov = 80;
            cut = true;
            clear = false;
            fov = 80;
        } else if (t < LIFT) {
            // Waking in his fist as he springs: ahead, the way he leaps.
            eye = hand(base, partial);
            look = base.add(facing.scale(40)).add(0, 14, 0);
            targetFov = 84;
            cut = t < WAKE + 1;
            clear = false;
        } else if (t < SEEN) {
            // 10. Carried through the sky in his fist: the crater falling away below, then the sky and the meteor ahead,
            // coming at us; the view swaying with his flight.
            float k = (t - LIFT) / (THROW - LIFT);
            eye = hand(base, partial);
            Vec3 down = Vec3.atBottomCenterOf(ground), from = leapFrom(down, heading), middle = Vec3.atBottomCenterOf(arena);
            // Ahead: the way he flies (never at the meteor from under its bowl).
            Vec3 ahead = eye.add(airborne(from, middle, heading, t + 3).subtract(airborne(from, middle, heading, t)).normalize().scale(30));
            Vec3 forth = base.add(facing.scale(40)).add(0, 14, 0);
            look = k < 0.1 ? forth.lerp(down, ease(0, 0.1F, k)) : down.lerp(ahead, ease(0.4F, 0.62F, k));
            roll = 12 * Mth.sin(t * 0.21F) + 6 * Mth.sin(t * 0.53F);
            targetFov = 84;
            clear = false;
        } else if (t < DAZED) {
            // 11. Seen from outside, beside him high over the bowl: the viewer limp in his fist; the throw; the body
            // hurled down, the camera following it (closing in), its crash into the floor.
            Vec3 side = new Vec3(-heading.z, 0, heading.x);
            Vec3 body = body(mc, base, partial);
            if (t < THROW) {
                // Close beside him: the viewer limp in his fist, his arm drawn back.
                eye = base.add(side.scale(10)).add(heading.scale(3)).add(0, 4, 0);
                look = body.lerp(base.add(0, 5, 0), 0.35);
                targetFov = 62;
            } else {
                // Chasing the body down, behind and above it; stopping over where it strikes.
                float fall = Math.min(t, IMPACT);
                Vec3 at = body(mc, base, fall - ticks), from = hand(base, partial), to = landing(mc, partial).add(0, -1.62, 0);
                Vec3 back = from.subtract(to).normalize();
                eye = at.add(back.scale(8)).add(side.scale(2)).add(0, 3, 0);
                look = at.add(0, 0.6, 0);
                targetFov = 70;
            }
            if (t >= IMPACT) shake = Math.max(shake, 6);
            cut = t < SEEN + 1;
            clear = false;
        } else if (t < TITLE) {
            // 12. Back in our own eyes, in the crater, dazed, the view tilted; looking up at him falling from the sky, his
            // landing, his roar.
            Vec3 own = landing(mc, partial);
            float k = ease(DAZED, DAZED + 14, t);
            eye = own;
            look = base.add(0, 4.0, 0);
            roll = (24 - 24 * k) + 3 * Mth.sin(t * 0.4F) * (1 - k);
            targetFov = 76;
            cut = t < DAZED + 1;
            clear = false;
        } else {
            // 13. The title: back from the player's eyes to over his shoulder, the colossus before him and his title over
            // his bar; then gliding forward into his eyes again, and the fight begins.
            Vec3 own = mc.player.getEyePosition(partial);
            Vec3 toHim = base.subtract(own).multiply(1, 0, 1);
            Vec3 d = toHim.lengthSqr() < 1e-4 ? facing.scale(-1) : toHim.normalize();
            Vec3 side = new Vec3(-d.z, 0, d.x);
            float k = ease(TITLE, HOME, t);
            // (Into the bowl by a cut under the title's flash.)
            Vec3 shoulder = own.subtract(d.scale(4.6 - 0.8 * k)).add(side.scale(1.7)).add(0, 0.45, 0);
            eye = shoulder;
            look = base.add(0, 5.0 - 1.2 * k, 0);
            targetFov = 66;
            cut = t < TITLE + 1;
            clear = false;
            if (t > HOME) {
                // Home: in the player's eyes some ticks before the film ends.
                float h = ease(HOME, HOMED, t);
                eye = shoulder.lerp(own, h);
                look = look.lerp(own.add(mc.player.getViewVector(partial).scale(10)), h);
                targetFov = Mth.lerp(h, 66, mc.options.fov().get());
            }
        }
        if (clear) eye = clear(mc, look, eye);
        float dt = Math.max(0.02F, event.getPartialTick().getGameTimeDeltaTicks());
        fov += (targetFov - fov) * eased(0.12F, dt);
        if (cut || smoothEye == null) { smoothEye = eye; smoothLook = look; }
        smoothEye = smoothEye.lerp(eye, eased(0.22F, dt));
        smoothLook = smoothLook.lerp(look, eased(0.3F, dt));
        Vec3 dir = smoothLook.subtract(smoothEye);
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z)));
        camera.setPos(smoothEye);
        camera.xo = camera.xOld = smoothEye.x;
        camera.yo = camera.yOld = smoothEye.y;
        camera.zo = camera.zOld = smoothEye.z;
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
    }

    /**
     * His flight with the viewer, drawn on the path the server keeps (WarlordTransformation.flight): from his leap out of
     * the crater round the meteor to the top of it; held there as he throws; then his fall into the middle of the bowl.
     */
    private static void fly(SonOfTheDemonEntity him, float t) {
        Vec3 from = leapFrom(Vec3.atBottomCenterOf(ground), heading), middle = Vec3.atBottomCenterOf(arena);
        Vec3 at = place(from, middle, t), next = place(from, middle, t + 0.5F);
        him.setPos(at);
        // Turned the way he flies (so the viewer in his fist sees where they go).
        Vec3 way = next.subtract(at).multiply(1, 0, 1);
        if (way.lengthSqr() > 1e-4) {
            float yaw = (float) Math.toDegrees(Math.atan2(-way.x, way.z));
            him.setYRot(yaw);
            him.yRotO = yaw;
            him.yBodyRot = him.yBodyRotO = him.yHeadRot = him.yHeadRotO = yaw;
        }
        him.xo = him.xOld = at.x;
        him.yo = him.yOld = at.y;
        him.zo = him.zOld = at.z;
    }

    private static Vec3 place(Vec3 from, Vec3 middle, float t) {
        return airborne(from, middle, heading, Math.min(t, SLAM));
    }

    /** Where the viewer is held: in his right fist, before his chest (lower while he crouches to leap). */
    private static Vec3 hand(Vec3 base, float partial) {
        float t = ticks + partial;
        double crouch = t >= LEAP && t < LIFT ? 1.6 * Math.sin(Math.PI * Mth.clamp((t - LEAP) / (LIFT + 4 - LEAP), 0, 1)) : 0;
        return base.add(facing.scale(3.6)).add(new Vec3(-facing.z, 0, facing.x).scale(-0.4)).add(0, 4.6 - crouch, 0);
    }

    /** The top of the ground under {@code at}, looking down from {@code from} (under the meteor, not on it). */
    private static double groundAt(Minecraft mc, Vec3 at, double from) {
        BlockPos.MutableBlockPos p = BlockPos.containing(at.x, from, at.z).mutable();
        for (int i = 0; i < 40 && p.getY() > mc.level.getMinBuildHeight(); i++, p.move(0, -1, 0))
            if (!mc.level.getBlockState(p).getCollisionShape(mc.level, p).isEmpty()) return p.getY() + 1;
        return at.y;
    }

    /**
     * The viewer's body in the shot from outside: hanging limp below his fist, swinging; then hurled down, quickening, to
     * where he lands; then lying in the crater.
     */
    private static Vec3 body(Minecraft mc, Vec3 base, float partial) {
        float t = ticks + partial;
        if (t < THROW) return hand(base, partial).add(0, -1.4, 0);
        Vec3 to = landing(mc, partial).add(0, -1.62, 0);
        if (t >= IMPACT) return to.add(0, -0.8, 0);
        Vec3 top = airborne(leapFrom(Vec3.atBottomCenterOf(ground), heading), Vec3.atBottomCenterOf(arena), heading, THROW);
        Vec3 from = top.add(heading.scale(3.6)).add(0, 3.2, 0);
        float k = (t - THROW) / (IMPACT - THROW);
        return from.lerp(to, k * k).add(0, 5 * Mth.sin(Mth.PI * k), 0);
    }

    /** The viewer's own body drawn where the film puts it, in the shot from outside (the world's is elsewhere, unseen). */
    @SubscribeEvent
    public static void onRenderStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (camera == null || event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (ticks < SEEN || ticks >= DAZED || !rises || heading == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false), t = ticks + partial;
        LivingEntity who = subject(mc);
        if (who == null) return;
        Vec3 at = body(mc, who.getPosition(partial), partial), cam = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        // Limp: lying across his fist, swinging; hurled: tumbling head over heels; landed: flat on its back.
        pose.translate(0, 0.9, 0);
        float tumble = t < THROW ? 80 + 14 * Mth.sin(t * 0.5F) : t < IMPACT ? 80 + 720 * (t - THROW) / (IMPACT - THROW) : 90;
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) -Math.toDegrees(Math.atan2(heading.x, heading.z))));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(tumble));
        pose.translate(0, -0.9, 0);
        // Drawn with the camera's turn (the stage's model-view), whatever this stage leaves in place.
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        drawingBody = true;
        try {
            int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, BlockPos.containing(at.add(0, 1, 0)));
            mc.getEntityRenderDispatcher().render(mc.player, 0, 0, 0, 0, partial, pose, buffers, light);
        } finally {
            drawingBody = false;
        }
        buffers.endBatch();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        pose.popPose();
    }

    private static boolean drawingBody;

    /** Where the viewer lands on the meteor: his own eyes once carried there, before that where he will be (before him). */
    private static Vec3 landing(Minecraft mc, float partial) {
        Vec3 own = mc.player.getEyePosition(partial), middle = Vec3.atBottomCenterOf(arena);
        if (Math.abs(own.y - middle.y) < 8) return own;
        return middle.add((heading == null ? facing : heading).scale(22)).add(0, 1.62, 0);
    }

    /**
     * The eye kept in the open, with the subject in sight: raised over what would hide it (a crater's step, a wall), up
     * to fourteen blocks; else drawn in front of it.
     */
    private static Vec3 clear(Minecraft mc, Vec3 look, Vec3 eye) {
        for (int up = 0; up <= 14; up++) {
            Vec3 e = eye.add(0, up, 0);
            if (mc.level.clip(new ClipContext(look, e, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, camera)).getType() == HitResult.Type.MISS
                    && mc.level.getBlockState(BlockPos.containing(e)).isAir()) return e;
        }
        var hit = mc.level.clip(new ClipContext(look, eye, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, camera));
        Vec3 back = look.subtract(hit.getLocation());
        return back.lengthSqr() < 1e-4 ? look : hit.getLocation().add(back.normalize().scale(Math.min(0.6, back.length())));
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (camera == null) return;
        double n = System.nanoTime() / 1.6e7;
        event.setRoll(event.getRoll() + roll + shake * 0.7F * Mth.sin((float) n));
        event.setPitch(event.getPitch() + shake * 0.5F * Mth.cos((float) (n * 1.3)));
        event.setYaw(event.getYaw() + shake * 0.3F * Mth.sin((float) (n * 0.7)));
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (camera != null) event.setFOV(fov);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (camera == null) return;
        var input = event.getInput();
        input.leftImpulse = 0;
        input.forwardImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
    }

    // ---------------------------------------------------------------- a film, not a game screen

    /** No player in the shots: no body where the camera does not expect it, no name floating over the scene. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onRenderLiving(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> event) {
        if (camera == null || drawingBody || !(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        // Seen only over his shoulder at the end, and not once the camera is entering his head.
        if (ticks < TITLE || ticks >= HOME && player == Minecraft.getInstance().player) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (camera != null) event.setCanRender(TriState.FALSE);
    }

    /** No block, water or fire over the screen: the camera is not where the player stands. */
    @SubscribeEvent
    public static void onScreenEffect(RenderBlockScreenEffectEvent event) {
        if (camera != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (camera != null) event.setCanceled(true);
    }

    /** Nothing of the game's screen but, in the last shot, his bar and its title. */
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (camera == null) return;
        if (ticks >= TITLE && event.getName().equals(VanillaGuiLayers.BOSS_OVERLAY)) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (camera == null) return;
        GuiGraphics g = event.getGuiGraphics();
        float t = ticks + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth(), h = g.guiHeight();
        // The world stopped and drained while he kneels and strikes himself.
        if (t < FACE) g.fill(0, 0, w, h, (Math.round(0.14F * 255) << 24) | 0x9AA3AE);
        // Knocked out: the blow's frame, then black, waking in his fist.
        if (rises && t >= KO && t < AWAKE) {
            if (t < KO + 3) impact(g, w, h, t - KO);
            else g.fill(0, 0, w, h, (Math.round(255 * (t < WAKE ? 1 : 1 - ease(WAKE, AWAKE, t))) << 24));
        }
        // The golden light of the aura.
        if (t >= AURA && t < GRAB) g.fill(0, 0, w, h, (Math.round(46 * Mth.sin((t - AURA) / (GRAB - AURA) * Mth.PI)) << 24) | 0xFFC640);
        // Into his chest: to black.
        if (t >= PLUNGE + 4 && t < HEART + 2) g.fill(0, 0, w, h, Math.round(255 * ease(PLUNGE + 4, HEART, t)) << 24);
        if (t >= HEART && t < EMERGE) heart(g, w, h, t);
        if (flash > 0.02F) g.fill(0, 0, w, h, (Math.round(Math.min(1, flash) * 245) << 24) | (t >= SWAP - 2 ? 0xFFF1C4 : 0xFFFFFF));
        // The bars of the screen: the top one opens for his bar and title, both slide away as the camera comes home.
        // Dipping to black at the very end, the fade back into the game follows.
        if (t > LAST - 5) g.fill(0, 0, w, h, Math.round(255 * ease(LAST - 5, LAST, t)) << 24);
        int bar = Math.round(h * 0.11F);
        float home = ease(HOME, HOMED, t);
        int top = Math.round(bar * (1 - ease(TITLE - 4, TITLE + 10, t))), bottom = Math.round(bar * (1 - home));
        g.fill(0, 0, w, top, 0xFF000000);
        g.fill(0, h - bottom, w, h, 0xFF000000);
    }

    /**
     * The blow in the face, as a manga draws it: a white frame, black speed lines bursting from where the fist struck,
     * ragged, the burst's heart black; three ticks, then black.
     */
    private static void impact(GuiGraphics g, int w, int h, float k) {
        g.fill(0, 0, w, h, 0xFFF8F6F0);
        float cx = w / 2F, cy = h / 2F, reach = (float) Math.hypot(w, h) / 2;
        var r = new java.util.Random(4761);
        for (int i = 0; i < 72; i++) {
            double a = Math.PI * 2 * i / 72 + r.nextDouble() * 0.05;
            float from = reach * (0.12F + 0.3F * r.nextFloat()) * (1 - 0.3F * k), thick = 1 + r.nextInt(3);
            for (float d = from; d < reach; d += 2) {
                float x = cx + (float) Math.cos(a) * d, y = cy + (float) Math.sin(a) * d;
                int size = Math.round(thick * (0.6F + d / reach));
                g.fill(Math.round(x), Math.round(y), Math.round(x) + size, Math.round(y) + size, 0xFF0A0806);
            }
        }
        // The burst where it struck: a black star, its white core.
        int core = Math.round(h * (0.16F + 0.08F * k));
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            float len = core * (i % 2 == 0 ? 1.4F : 0.8F);
            for (float d = 0; d < len; d += 1.5F) {
                int s = Math.max(2, Math.round((len - d) / 5));
                g.fill(Math.round(cx + (float) Math.cos(a) * d - s / 2F), Math.round(cy + (float) Math.sin(a) * d - s / 2F),
                        Math.round(cx + (float) Math.cos(a) * d + s / 2F), Math.round(cy + (float) Math.sin(a) * d + s / 2F), 0xFF0A0806);
            }
        }
        int c = core / 3;
        g.fill(Math.round(cx - c), Math.round(cy - c / 2F), Math.round(cx + c), Math.round(cy + c / 2F), 0xFFF8F6F0);
    }

    /**
     * His heart in the dark: a block of flesh, turning slowly, beating; slower; it stops (its light fades, its flesh
     * greys); it turns to gold in a rising light and beats again, each beat a shockwave; embers drifting all the while.
     */
    private static void heart(GuiGraphics g, int w, int h, float t) {
        g.fill(0, 0, w, h, 0xFF000000);
        boolean gold = t >= HEART_GOLD + 5;
        float stopped = t >= HEART_STOP && t < HEART_GOLD ? ease(HEART_STOP, HEART_STOP + 10, t) : 0;
        float rebirth = t >= HEART_GOLD ? ease(HEART_GOLD, HEART_GOLD + 10, t) * (1 - ease(HEART_GOLD + 6, HEART_GOLD + 22, t)) : 0;
        float r = gold ? 1.0F : 0.78F, gr = gold ? 0.74F : 0.07F, b = gold ? 0.22F : 0.06F;
        float jx = shake * Mth.sin(t * 3.1F) * 0.8F, jy = shake * Mth.cos(t * 2.3F) * 0.8F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        // Embers, drifting up through the dark.
        for (int i = 0; i < 48; i++) {
            float sx = (i * 73.7F) % w, speed = 0.25F + (i % 7) * 0.09F;
            float sy = h - ((i * 131.3F + t * speed * h / 60F) % (h + 20));
            int size = 1 + i % 3, a = Math.round((0.25F + 0.35F * ((i * 17) % 10) / 10F) * (1 - stopped * 0.85F) * 255);
            g.fill(Math.round(sx), Math.round(sy), Math.round(sx) + size, Math.round(sy) + size, (a << 24) | (gold ? 0xFFD060 : 0xC22018));
        }
        // Its light, beating with it.
        float glow = (0.32F + 0.55F * pulse) * (1 - stopped * 0.85F) + rebirth * 0.9F;
        int gs = Math.round(h * (1.05F + 0.25F * pulse));
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, gr, b, Mth.clamp(glow, 0, 1));
        g.blit(GLOW, Math.round(w / 2F - gs / 2F + jx), Math.round(h / 2F - gs / 2F + jy), gs, gs, 0, 0, 128, 128, 128, 128);
        // The shockwaves of its golden beats.
        for (int beat : GOLD_BEATS) if (t >= beat && t < beat + 16) {
            float k = (t - beat) / 16F;
            int rs = Math.round(h * (0.4F + 2.2F * k));
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 0.85F, 0.4F, 0.7F * (1 - k));
            g.blit(GLOW, Math.round(w / 2F - rs / 2F), Math.round(h / 2F - rs / 2F), rs, rs, 0, 0, 128, 128, 128, 128);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.flush();
        // The heart itself, a block, turning slowly (it all but stops with it); greyed while stopped.
        float half = h * 0.15F * (1 + 0.16F * pulse - 0.04F * stopped);
        float spin = 35 + (t - HEART) * (gold ? 2.4F : 1.3F) * (1 - stopped * 0.9F);
        cube(g, w / 2F + jx, h / 2F + jy, half, spin, 24, gold ? HEART_GOLD_SIDE : HEART_SIDE, gold ? HEART_GOLD_TOP : HEART_TOP, 1 - 0.5F * stopped);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // The instant it turns: gold light flooding out of it.
        if (t >= HEART_GOLD && t < HEART_GOLD + 14) {
            float k = 1 - Math.abs((t - HEART_GOLD - 5) / 9F);
            g.fill(0, 0, w, h, (Math.round(Mth.clamp(k, 0, 1) * 200) << 24) | 0xFFE9A8);
        }
        RenderSystem.disableBlend();
    }

    private static final ResourceLocation HEART_SIDE = tex("demon_heart"), HEART_TOP = tex("demon_heart_top"),
            HEART_GOLD_SIDE = tex("golden_demon_heart"), HEART_GOLD_TOP = tex("golden_demon_heart_top");

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/item/" + name + ".png");
    }

    /** Corners of the faces, as seen from outside (top left, top right, bottom right, bottom left); corner i is (x, y, z) = bits 0, 1, 2. */
    private static final int[][] FACES = {{6, 7, 5, 4}, {3, 2, 0, 1}, {7, 3, 1, 5}, {2, 6, 4, 0}, {2, 3, 7, 6}, {4, 5, 1, 0}};
    private static final float[][] NORMALS = {{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};

    /**
     * A textured cube drawn on the screen, turned {@code yaw} degrees about its upright and its top tipped {@code pitch}
     * degrees towards the viewer, lit from above left: only its faces towards the viewer (a cube hides none of itself).
     */
    private static void cube(GuiGraphics g, float cx, float cy, float half, float yaw, float pitch, ResourceLocation side, ResourceLocation top, float grey) {
        float cy0 = Mth.cos(yaw * Mth.DEG_TO_RAD), sy0 = Mth.sin(yaw * Mth.DEG_TO_RAD), cp = Mth.cos(pitch * Mth.DEG_TO_RAD), sp = Mth.sin(pitch * Mth.DEG_TO_RAD);
        float[][] p = new float[8][];
        for (int i = 0; i < 8; i++) p[i] = turn((i & 1) != 0 ? 1 : -1, (i & 2) != 0 ? 1 : -1, (i & 4) != 0 ? 1 : -1, cy0, sy0, cp, sp);
        float[] light = {-0.45F, 0.75F, 0.5F};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        var matrix = g.pose().last().pose();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        for (int f = 0; f < 6; f++) {
            float[] n = turn(NORMALS[f][0], NORMALS[f][1], NORMALS[f][2], cy0, sy0, cp, sp);
            if (n[2] <= 0.001F) continue;
            float lit = (0.5F + 0.5F * Math.max(0, n[0] * light[0] + n[1] * light[1] + n[2] * light[2])) * grey;
            RenderSystem.setShaderTexture(0, f >= 4 ? top : side);
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int k = 0; k < 4; k++) {
                float[] c = p[FACES[f][k]];
                buffer.addVertex(matrix, cx + c[0] * half, cy - c[1] * half, 300).setUv(uv[k][0], uv[k][1]).setColor(lit, lit, lit, 1);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
        RenderSystem.enableCull();
    }

    private static float[] turn(float x, float y, float z, float cy, float sy, float cp, float sp) {
        float x1 = x * cy + z * sy, z1 = -x * sy + z * cy;
        return new float[] {x1, y * cp - z1 * sp, y * sp + z1 * cp};
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        camera = null;
        colossus = null;
        wind = null;
        shake = flash = pulse = 0;
    }
}
