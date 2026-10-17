package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.client.MurimblockKeyMappings;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.inner.InnerWorldService;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimAnimations.Motions;
import com.murimblock.network.CultivationPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The meditation key and what the meditating player sees (docs/cultivation/qi-cultivation.md):
 * <ul>
 *   <li>a tap of the key sits down in the lotus, or gets up; held a moment and a half while seated (or anywhere in the
 *   inner world), it opens the inner world, or leaves it: a bar under the crosshair fills meanwhile;</li>
 *   <li>moving gets up;</li>
 *   <li>seated, the view leaves the eyes: it turns slowly round the body, and the mouse turns it further;</li>
 *   <li>everyone sees a meditating player seated in the lotus (Epic Fight's living motion, from {@code AuraState}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MeditationClient {
    /** How long the key is held to open the inner world (15 s), or to leave it, in ticks. */
    public static final int HOLD_TICKS = MeditationService.ENTER_HOLD_TICKS, LEAVE_TICKS = MeditationService.LEAVE_HOLD_TICKS;
    private static int held;
    private static boolean wasDown, holdSent;
    private static long lastStopSent = -100;

    private static Marker camera;
    private static double orbit;
    /**
     * The view turns round the body only while sitting down, a short cinematic (Enzo, 17/10/2026: "on reste à la première
     * personne, les cinématiques seulement durant la première partie"); then the meditation is seen through one's own
     * eyes, looking about freely, the crosshair drawing the motes in.
     */
    static final int CINEMATIC_TICKS = 60;
    private static int cinematic;
    private static boolean cinematicDone;
    /** The mote under the crosshair (its entity id, -1: none), for how many ticks, and those already asked for. */
    private static int focused = -1, focusTicks;
    private static final java.util.Set<Integer> drawn = new java.util.HashSet<>();
    /** How long the gaze rests on a mote before it comes (ticks), and how close to the crosshair it must be (degrees). */
    static final int FOCUS_TICKS = 5;
    static final double FOCUS_ANGLE = 7;
    private static float lockedYaw;

    private MeditationClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        EpicFightEventHooks.Animation.INIT_ANIMATOR.registerEvent(e -> {
            if (e.getEntityPatch() instanceof PlayerPatch<?>) e.getAnimator().addLivingAnimation(Motions.MEDITATE, MurimAnimations.MEDITATE);
        }, "murimblock:meditation_animations", 0);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_BASE.registerEvent(e -> {
            if (MeditationService.isMeditating(e.getPlayerPatch().getOriginal())) e.setMotion(Motions.MEDITATE);
        }, "murimblock:meditation_motion", 0);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_COMPOSITE.registerEvent(e -> {
            if (MeditationService.isMeditating(e.getPlayerPatch().getOriginal())) e.setMotion(Motions.MEDITATE);
        }, "murimblock:meditation_composite", 0);
    }

    private static void send(int action) {
        if (Minecraft.getInstance().getConnection() != null) PacketDistributor.sendToServer(new CultivationPayloads.Meditate(action));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            reset();
            return;
        }
        boolean meditating = MeditationService.isMeditating(player);
        boolean inner = InnerWorldService.isInside(player);
        boolean down = mc.screen == null && MurimblockKeyMappings.MEDITATE.isDown();
        if (down) {
            held++;
            if (!holdSent && (inner ? held >= LEAVE_TICKS : meditating && held >= HOLD_TICKS)) {
                send(CultivationPayloads.Meditate.HOLD);
                holdSent = true;
            }
        } else if (wasDown) {
            // Let go: a tap sits down or gets up (a hold has done its work already).
            if (!holdSent) send(meditating ? CultivationPayloads.Meditate.STOP : CultivationPayloads.Meditate.START);
            held = 0;
            holdSent = false;
        }
        wasDown = down;
        if (meditating && !cinematicDone) {
            ensureCamera(mc, player);
            if (++cinematic >= CINEMATIC_TICKS) {
                releaseCamera(mc);
                cinematicDone = true;
                // Back in one's eyes, looking where the body faces.
                player.setYRot(lockedYaw);
                player.setXRot(10);
            }
        } else if (!meditating) {
            releaseCamera(mc);
            cinematic = 0;
            cinematicDone = false;
        }
        gaze(mc, player, meditating);
    }

    /**
     * Seated, the movement speed is locked to nothing, which Minecraft would show as a narrowing of the view (as under
     * Slowness): the view keeps its width while meditating.
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void onFov(net.neoforged.neoforge.client.event.ComputeFovModifierEvent event) {
        if (MeditationService.isMeditating(event.getPlayer())) event.setNewFovModifier(1.0F);
    }

    /** The mote of Qi a meditating player's view rests on (MeditationOrbs), or -1: the renderer brightens it. */
    public static int focusedMote() {
        return focused;
    }

    /** Each tick: which of one's own waiting motes lies under the crosshair; held there long enough, it is drawn in. */
    private static void gaze(Minecraft mc, LocalPlayer player, boolean meditating) {
        if (!meditating || orbiting() || mc.level == null) {
            focused = -1;
            focusTicks = 0;
            drawn.clear();
            return;
        }
        var view = mc.gameRenderer.getMainCamera();
        Vec3 eye = view.getPosition(), look = new Vec3(view.getLookVector());
        double best = Math.cos(Math.toRadians(FOCUS_ANGLE));
        int found = -1;
        for (Entity e : mc.level.getEntities(player, player.getBoundingBox().inflate(com.murimblock.cultivation.MeditationOrbs.REACH))) {
            if (!(e instanceof com.murimblock.cultivation.inner.QiMoteEntity mote) || !mote.meditation() || mote.pulled()
                    || !player.getUUID().equals(mote.owner()) || drawn.contains(mote.getId())) continue;
            double cos = mote.position().add(0, 0.25, 0).subtract(eye).normalize().dot(look);
            if (cos > best) { best = cos; found = mote.getId(); }
        }
        focusTicks = found == focused && found >= 0 ? focusTicks + 1 : 0;
        focused = found;
        if (found >= 0 && focusTicks >= FOCUS_TICKS && mc.getConnection() != null) {
            PacketDistributor.sendToServer(new CultivationPayloads.Absorb(found));
            drawn.add(found);
            focused = -1;
            focusTicks = 0;
        }
    }

    /** Moving gets up; meanwhile the body does not move. */
    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getEntity() != player || !MeditationService.isMeditating(player)) return;
        Input input = event.getInput();
        boolean moving = input.forwardImpulse != 0 || input.leftImpulse != 0 || input.jumping;
        if (moving && player.level().getGameTime() - lastStopSent > 10) {
            lastStopSent = player.level().getGameTime();
            send(CultivationPayloads.Meditate.STOP);
        }
        input.leftImpulse = 0;
        input.forwardImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
    }

    // ---------------------------------------------------------------- the view turning round the seated body

    private static void ensureCamera(Minecraft mc, LocalPlayer player) {
        if (camera != null && mc.getCameraEntity() == camera) return;
        if (mc.getCameraEntity() != player) return;
        camera = new Marker(EntityType.MARKER, mc.level);
        lockedYaw = player.getYRot();
        // From in front and to the side of the seated body, to begin with.
        orbit = Math.toRadians(lockedYaw + 90 + 35);
        place(player, 0);
        mc.setCameraEntity(camera);
    }

    private static void releaseCamera(Minecraft mc) {
        if (camera == null) return;
        if (mc.getCameraEntity() == camera && mc.player != null) mc.setCameraEntity(mc.player);
        camera = null;
    }

    private static void place(LocalPlayer player, float partial) {
        Vec3 at = player.getPosition(partial);
        Vec3 eye = at.add(Math.cos(orbit) * 3.4, 1.35, Math.sin(orbit) * 3.4);
        Vec3 d = at.add(0, 0.65, 0).subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        camera.setPos(eye);
        camera.xo = camera.xOld = eye.x;
        camera.yo = camera.yOld = eye.y;
        camera.zo = camera.zOld = eye.z;
        camera.setYRot(yaw);
        camera.setXRot(pitch);
        camera.yRotO = yaw;
        camera.xRotO = pitch;
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (camera == null || player == null || mc.getCameraEntity() != camera) return;
        // The mouse turns the view round the body, not the body itself.
        float turned = player.getYRot() - lockedYaw;
        orbit += Math.toRadians(turned) + 0.0012;
        player.setYRot(lockedYaw);
        player.yRotO = lockedYaw;
        player.setYHeadRot(lockedYaw);
        player.setXRot(0);
        player.xRotO = 0;
        place(player, event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (camera != null && Minecraft.getInstance().getCameraEntity() == camera) event.setCanceled(true);
    }

    /** Holding the key: a thin bar under the crosshair fills, with what it is going to do. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || held < 4 || holdSent) return;
        boolean inner = InnerWorldService.isInside(mc.player);
        if (!inner && !MeditationService.isMeditating(mc.player)) return;
        GuiGraphics g = event.getGuiGraphics();
        int w = 80, x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() / 2 + 18;
        int needed = inner ? LEAVE_TICKS : HOLD_TICKS;
        float t = Math.min(1, held / (float) needed);
        g.fill(x - 1, y - 1, x + w + 1, y + 4, 0x90000000);
        g.fill(x, y, x + Math.round(w * t), y + 3, 0xFFEAD27A);
        // Entering takes a long, still breath: the seconds left are shown.
        Component label = inner ? Component.translatable("gui.murimblock.meditation.leave")
                : Component.translatable("gui.murimblock.meditation.enter_in", (needed - held + 19) / 20);
        g.drawCenteredString(mc.font, label, g.guiWidth() / 2, y + 7, 0xFFF4EEDF);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    private static void reset() {
        held = 0;
        wasDown = holdSent = false;
        camera = null;
    }

    /** Whether the view is the meditation's turning one (for the tours). */
    public static boolean orbiting() {
        return camera != null && Minecraft.getInstance().getCameraEntity() == camera;
    }
}
