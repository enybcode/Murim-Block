package com.murimblock.client.cultivation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.SwordFlight;
import com.murimblock.network.CultivationPayloads;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Flying on the sword, the client's side (SwordFlight):
 * <ul>
 *   <li>mounting: the jump key pressed again in the air and held; past a moment the server is told (the Qi gathers under
 *   the feet, the fall slowed), and at {@link SwordFlight#CHARGE_TICKS} the sword comes;</li>
 *   <li>steering one's own flight: along the look while moving forward (faster sprinting), up with jump, down sneaking,
 *   hovering otherwise; jump pressed twice quickly gets off;</li>
 *   <li>for every flyer seen: their sword in hand drawn big and flat under their feet, pointing their way, a trail of
 *   their aura behind it.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SwordFlightClient {
    /** Held this long, the second jump is a charge (not a mere leap): the server is told. */
    static final int CHARGE_FROM = 8;
    /** Speeds (blocks a tick): along the look, sprinting, up or down; how fast the flight turns to them. */
    static final double SPEED = 0.8, FAST = 2.4, CLIMB = 0.45, EASE = 0.12, FAST_EASE = 0.04;
    /** Very fast now (for the view's widening and the wind), and was last tick (for the burst of air as it starts). */
    private static boolean fast, wasFast;
    /**
     * One's own flight's speed (blocks a tick), kept here and given to the player before each tick: Minecraft slows a
     * body in the air after it moves (x 0.91), which would cap the flight far under the speeds asked for.
     */
    private static Vec3 flightSpeed;
    /** Two presses of jump this close (ticks) in flight: off the sword. */
    static final int DOUBLE_TAP = 7;

    private static final IntSet FLYING = new IntOpenHashSet();
    private static boolean wasDown, charging;
    private static int airTicks, held = -1, sinceTap = 100;

    private SwordFlightClient() {
    }

    /** Epic Fight: the rider's pose (side-on, like a skater) as a living motion, shown while flying. */
    @SubscribeEvent
    public static void onClientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        yesman.epicfight.api.client.event.EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_BASE.registerEvent(e -> {
            if (flying(e.getPlayerPatch().getOriginal())) e.setMotion(com.murimblock.integration.epicfight.MurimAnimations.Motions.SWORD_RIDE);
        }, "murimblock:sword_ride_motion", 0);
        yesman.epicfight.api.client.event.EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_COMPOSITE.registerEvent(e -> {
            if (flying(e.getPlayerPatch().getOriginal())) e.setMotion(com.murimblock.integration.epicfight.MurimAnimations.Motions.SWORD_RIDE);
        }, "murimblock:sword_ride_composite", 0);
        yesman.epicfight.api.event.EpicFightEventHooks.Animation.INIT_ANIMATOR.registerEvent(e -> {
            if (!(e.getEntityPatch() instanceof yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch<?>)) return;
            e.getAnimator().addLivingAnimation(com.murimblock.integration.epicfight.MurimAnimations.Motions.SWORD_RIDE, com.murimblock.integration.epicfight.MurimAnimations.SWORD_RIDE);
        }, "murimblock:sword_ride_animations", 0);
    }

    public static void state(CultivationPayloads.SwordFlightState payload) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.flying()) FLYING.add(payload.entityId());
        else FLYING.remove(payload.entityId());
        if (mc.player != null && payload.entityId() == mc.player.getId()) mc.player.setNoGravity(payload.flying());
    }

    public static boolean flying(Player player) {
        return FLYING.contains(player.getId());
    }

    /** For the tours: how long the second jump has been held (-1: not). */
    public static int held() {
        return held;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            FLYING.clear();
            return;
        }
        boolean down = mc.screen == null && mc.options.keyJump.isDown();
        boolean pressed = down && !wasDown;
        wasDown = down;
        sinceTap++;
        if (flying(player)) {
            held = -1;
            if (pressed) {
                if (sinceTap <= DOUBLE_TAP) send(CultivationPayloads.SwordFlight.LAND);
                sinceTap = 0;
            }
            if (fast) {
                if (mc.level.getGameTime() % 50 == 0) player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.ELYTRA_FLYING, net.minecraft.sounds.SoundSource.PLAYERS, 0.35F, 1.2F, false);
            }
            return;
        }
        // Mounting: a fresh press in the air, then held.
        airTicks = player.onGround() ? 0 : airTicks + 1;
        if (pressed && airTicks >= QinggongClient.AIR_TICKS) held = 0;
        else if (held >= 0 && down && !player.onGround()) held++;
        else {
            if (charging) send(CultivationPayloads.SwordFlight.CANCEL);
            charging = false;
            held = -1;
        }
        if (held == CHARGE_FROM && SwordFlight.able(player)) {
            charging = true;
            send(CultivationPayloads.SwordFlight.CHARGE);
        }
        if (charging) {
            // The fall slowed while the Qi gathers under the feet.
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x * 0.85, Math.max(v.y, -0.04), v.z * 0.85);
            player.resetFallDistance();
            gather(mc, player, held / (float) SwordFlight.CHARGE_TICKS);
            if (held >= SwordFlight.CHARGE_TICKS) {
                send(CultivationPayloads.SwordFlight.MOUNT);
                charging = false;
                held = -1;
            }
        }
    }

    /** Before each tick, one's own flight: its speed eased toward the one asked for, and given to the player. */
    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !flying(player)) {
            flightSpeed = null;
            fast = wasFast = false;
            return;
        }
        if (flightSpeed == null) flightSpeed = player.getDeltaMovement();
        steer(mc, player);
    }

    /** One's own flight: toward the speed asked for, eased. */
    static void steer(Minecraft mc, LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        boolean sprint = mc.options.keyUp.isDown() && (player.isSprinting() || mc.options.keySprint.isDown());
        double speed = mc.options.keyUp.isDown() ? (sprint ? FAST : SPEED) : 0;
        double climb = mc.options.keyJump.isDown() ? CLIMB : mc.options.keyShift.isDown() ? -CLIMB : 0;
        Vec3 want = look.scale(speed).add(0, climb, 0);
        // Speeding up to very fast takes a couple of seconds; slowing down, as quick as ever.
        boolean accelerating = sprint && flightSpeed.length() < FAST;
        flightSpeed = flightSpeed.lerp(want, accelerating ? FAST_EASE : EASE);
        // Stopped by a wall or the ground, the flight's speed is lost too.
        if (player.horizontalCollision) flightSpeed = new Vec3(0, flightSpeed.y, 0);
        player.setDeltaMovement(flightSpeed);
        player.resetFallDistance();
        wasFast = fast;
        fast = flightSpeed.length() > SwordFlight.FAST_FROM;
        if (fast && !wasFast) burst(mc, player);
    }

    /** Going very fast: a ring of air bursting round the rider. */
    static void burst(Minecraft mc, LocalPlayer player) {
        Vec3 dir = player.getDeltaMovement().normalize();
        Vec3 side = dir.cross(new Vec3(0, 1, 0)).normalize(), up = side.cross(dir).normalize();
        Vec3 at = player.position().add(0, 0.9, 0);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            Vec3 out = side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD, at.x + out.x * 0.6, at.y + out.y * 0.6, at.z + out.z * 0.6,
                    out.x * 0.35 - dir.x * 0.2, out.y * 0.35 - dir.y * 0.2, out.z * 0.35 - dir.z * 0.2);
        }
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 0.8F, false);
    }

    /**
     * Very fast: speed lines over the screen, as drawn in manga: thin white strokes from the edges toward the middle,
     * flickering, more and brighter the faster one goes. Drawn on the screen: next to nothing to draw.
     */
    @SubscribeEvent
    public static void onGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !flying(mc.player)) return;
        float r = rush();
        if (r <= 0.15F) return;
        var g = event.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        float reach = (float) Math.hypot(w, h) / 2;
        // A new set of strokes every tick, the same through the tick's frames.
        RandomSource random = RandomSource.create(mc.level.getGameTime());
        int count = Math.round(16 + 44 * r);
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * 360, from = reach * (0.55F + random.nextFloat() * 0.3F);
            int alpha = Math.round((60 + random.nextInt(90)) * r);
            g.pose().pushPose();
            g.pose().translate(w / 2F, h / 2F, 0);
            g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
            g.fill(Math.round(from), 0, Math.round(reach + 4), 1 + random.nextInt(2), alpha << 24 | 0xFFFFFF);
            g.pose().popPose();
        }
    }

    /** How fast one's own flight goes, 0 (flying normally) to 1 (as fast as it goes). */
    static float rush() {
        if (flightSpeed == null) return 0;
        return (float) Math.max(0, Math.min(1, (flightSpeed.length() - SPEED) / (FAST - SPEED)));
    }

    /** Very fast: the view widens with the speed. */
    @SubscribeEvent
    public static void onFov(net.neoforged.neoforge.client.event.ComputeFovModifierEvent event) {
        if (!flying(event.getPlayer()) || event.getPlayer() != Minecraft.getInstance().player) return;
        event.setNewFovModifier(1.0F + 0.3F * rush());
    }

    /** Very fast: the view trembles a little in the wind. */
    @SubscribeEvent
    public static void onCamera(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !flying(mc.player)) return;
        float r = rush();
        if (r <= 0.2F) return;
        double t = mc.level.getGameTime() + event.getPartialTick();
        float shake = (r - 0.2F) * 0.9F;
        event.setRoll(event.getRoll() + (float) Math.sin(t * 1.7) * shake);
        event.setPitch(event.getPitch() + (float) Math.sin(t * 2.3 + 1) * shake * 0.5F);
    }

    /** The Qi drawn in under the feet, more and brighter as the sword nears. */
    static void gather(Minecraft mc, Player player, float near) {
        RandomSource random = mc.level.random;
        float[] c = colour(player);
        Vec3 under = player.position().add(0, -0.15, 0);
        for (int i = 0; i < 2 + Math.round(near * 4); i++) {
            double a = random.nextDouble() * Math.PI * 2, d = 1.6 - near;
            Vec3 from = under.add(Math.cos(a) * d, (random.nextDouble() - 0.3) * d, Math.sin(a) * d);
            Vec3 in = under.subtract(from).scale(0.2);
            AuraParticle p = AuraClient.spark(mc.level, from.x, from.y, from.z, in.x, in.y, in.z, c, false);
            if (p != null) p.big(0.12F + 0.1F * near, 6);
        }
        AuraParticle core = AuraClient.spark(mc.level, under.x, under.y, under.z, 0, 0, 0, c, false);
        if (core != null) core.big(0.2F + 0.8F * near, 2);
    }

    static float[] colour(Player player) {
        var art = MeditationService.aura(player).auraArt();
        return art.isPresent() ? AuraClient.colour(art) : new float[]{0.35F, 0.75F, 1.0F};
    }

    static void send(int action) {
        if (Minecraft.getInstance().getConnection() != null) PacketDistributor.sendToServer(new CultivationPayloads.SwordFlight(action));
    }

    /** The swords taken out of the flyers' hands while the world is drawn (it is under their feet), by entity id. */
    private static final java.util.Map<Integer, net.minecraft.world.item.ItemStack> UNDERFOOT = new java.util.HashMap<>();

    /**
     * The world drawn: before the creatures, each flyer's sword leaves their hand (it is under their feet, not in it);
     * after them, it is drawn under their feet and put back in their hand.
     */
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || FLYING.isEmpty() && UNDERFOOT.isEmpty()) return;
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            for (Player p : mc.level.players())
                if (flying(p) && SwordFlight.isSword(p.getMainHandItem())) {
                    UNDERFOOT.put(p.getId(), p.getMainHandItem());
                    p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
                }
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || UNDERFOOT.isEmpty()) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        for (Player p : mc.level.players()) {
            var sword = UNDERFOOT.remove(p.getId());
            if (sword == null) continue;
            Vec3 at = p.getPosition(partial).subtract(camera);
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            pose.mulPose(Axis.YP.rotationDegrees(-p.getViewYRot(partial)));
            FlyingSwordRenderer.render(mc, sword, pose, buffers, colour(p), mc.level.getGameTime() + partial);
            pose.popPose();
            p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
        }
        buffers.endBatch();
        // Any left (gone from the world meanwhile): nothing to put back.
        UNDERFOOT.clear();
    }

    /** In one's own hand, in first person: no sword while it is under the feet. */
    @SubscribeEvent
    public static void onHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && flying(mc.player) && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) event.setCanceled(true);
    }

    /** The trail of each flyer's aura behind the sword, carried along nearly as fast (left in the air, it would fill the view behind). */
    @SubscribeEvent
    public static void onTrail(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || FLYING.isEmpty()) return;
        for (Player p : mc.level.players()) {
            if (!flying(p)) continue;
            Vec3 back = Vec3.directionFromRotation(0, p.getYRot()).scale(-1.1), carried = p.getDeltaMovement().scale(0.8);
            float[] c = colour(p);
            for (int i = 0; i < 2; i++) {
                AuraParticle t = AuraClient.spark(mc.level, p.getX() + back.x + (mc.level.random.nextDouble() - 0.5) * 0.2, p.getY() - 0.1,
                        p.getZ() + back.z + (mc.level.random.nextDouble() - 0.5) * 0.2, carried.x, carried.y, carried.z, c, false);
                if (t != null) t.big(0.14F, 8);
            }
        }
    }
}
