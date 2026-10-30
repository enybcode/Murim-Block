package com.murimblock.client.combat;

import com.mojang.blaze3d.platform.InputConstants;
import com.murimblock.Murimblock;
import com.murimblock.client.MurimKeys;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.combat.CombatMoves;
import com.murimblock.combat.CombatPayloads;
import com.murimblock.combat.CombatState;
import com.murimblock.combat.Moveset;
import com.murimblock.shenfa.Animation;
import com.murimblock.shenfa.Animator;
import com.murimblock.shenfa.ShenfaLibrary;
import com.murimblock.shenfa.client.PlayerAnimators;
import com.murimblock.shenfa.client.SkeletalClips;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The fight on the client. In combat mode (B, D4) the attack key strikes instead of mining, the use key raises the
 * guard and Left Alt dodges the way the movement keys point. Moves start on the key press, predicted with the
 * server's own rules ({@link CombatMoves}); the server decides and corrects when it disagrees. While a strike cannot
 * be broken the body does not walk. Messages from the server play moves on the bodies they name and the feel of each
 * blow ({@link CombatEffects}).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class CombatClient {
    public static final KeyMapping MODE = new KeyMapping("key.murimblock.combat_mode", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, MurimKeys.CATEGORY);
    public static final KeyMapping DODGE = new KeyMapping("key.murimblock.dodge", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, MurimKeys.CATEGORY);
    public static final KeyMapping LOCK_ON = new KeyMapping("key.murimblock.lock_on", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, MurimKeys.CATEGORY);

    /** The local player's fight, predicted. */
    private static final CombatState LOCAL = new CombatState();
    /** When the local player last started a move by itself (game tick). */
    private static long started = Long.MIN_VALUE / 2;
    /** Dev and checks: inputs as if the keys were pressed. */
    private static boolean attackPressed, guardHeld;

    private CombatClient() {
    }

    static {
        PlayerAnimators.rootMotionElsewhere = () -> LOCAL.playing();
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(MODE);
        event.register(DODGE);
        event.register(LOCK_ON);
    }

    /** The local player's fight (for the HUD, the camera and checks). */
    public static CombatState local() {
        return LOCAL;
    }

    /** The last body the local player struck (lock-on takes it even when it is not a monster). */
    private static LivingEntity lastStruck;

    public static LivingEntity lastStruck() {
        return lastStruck;
    }

    static void struck(LivingEntity body) {
        lastStruck = body;
    }

    /** Dev and checks: presses the attack key once. */
    public static void pressAttack() {
        attackPressed = true;
    }

    /** Dev and checks: holds the guard up or lets it down. */
    public static void holdGuard(boolean held) {
        guardHeld = held;
    }

    /** Turns combat mode on or off for the local player, as the key does. */
    public static void setMode(boolean on) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || LOCAL.mode == on) {
            return;
        }
        LOCAL.mode = on;
        PlayerAnimators.mode(mc.player, on);
        PacketDistributor.sendToServer(new CombatPayloads.Input(on ? CombatPayloads.Action.MODE_ON
                : CombatPayloads.Action.MODE_OFF, 0));
        if (!on) {
            guard(mc.player, false);
        }
        mc.player.displayClientMessage(Component.translatable(on ? "message.murimblock.combat_on"
                : "message.murimblock.combat_off"), true);
    }

    /** The keys, read before vanilla reads them: in combat mode the attack and use keys are the fight's. */
    @SubscribeEvent
    public static void keys(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        while (MODE.consumeClick()) {
            if (mc.screen == null) {
                setMode(!LOCAL.mode);
            }
        }
        if (!LOCAL.mode || player.isSpectator()) {
            return;
        }
        boolean free = mc.screen == null && !player.isPassenger();
        while (mc.options.keyAttack.consumeClick()) {
            if (free) {
                act(player, CombatState.Input.ATTACK, 0);
            }
        }
        if (attackPressed) {
            attackPressed = false;
            act(player, CombatState.Input.ATTACK, 0);
        }
        while (DODGE.consumeClick()) {
            if (free) {
                act(player, CombatState.Input.DODGE, direction(mc));
            }
        }
        while (mc.options.keyUse.consumeClick()) {
            // The use key is the guard in combat mode: no item is used.
        }
        boolean wanted = free && (mc.options.keyUse.isDown() || guardHeld) && !LOCAL.playing() && LOCAL.stunTicks == 0;
        if (wanted != LOCAL.guarding) {
            guard(player, wanted);
        }
    }

    /** The way the movement keys point: forward, back, left, right (back when none is held). */
    private static int direction(Minecraft mc) {
        if (mc.options.keyUp.isDown()) {
            return 0;
        }
        if (mc.options.keyLeft.isDown()) {
            return 2;
        }
        if (mc.options.keyRight.isDown()) {
            return 3;
        }
        return 1;
    }

    private static void guard(LocalPlayer player, boolean up) {
        long now = player.level().getGameTime();
        LOCAL.guard(up, now);
        if (up) {
            Animation guard = SkeletalClips.get(Moveset.of(player).guard());
            if (guard != null) {
                PlayerAnimators.play(player, Animator.Priority.MIDDLE, guard, 1, false);
            }
        } else {
            PlayerAnimators.stop(player, Animator.Priority.MIDDLE);
        }
        PacketDistributor.sendToServer(new CombatPayloads.Input(up ? CombatPayloads.Action.GUARD_UP
                : CombatPayloads.Action.GUARD_DOWN, 0));
    }

    /** Starts the move an input asks for at once if the rules allow it, and tells the server. */
    private static void act(LocalPlayer player, CombatState.Input input, int direction) {
        long now = player.level().getGameTime();
        start(player, CombatMoves.decide(LOCAL, input, direction, player, now, ClientCultivation.get().qi()), now);
        PacketDistributor.sendToServer(new CombatPayloads.Input(switch (input) {
            case ATTACK -> CombatPayloads.Action.ATTACK;
            case DODGE -> CombatPayloads.Action.DODGE;
        }, direction));
    }

    private static void start(LocalPlayer player, CombatMoves.Choice choice, long now) {
        if (choice == null) {
            return;
        }
        if (LOCAL.guarding) {
            guard(player, false);
        }
        CombatMoves.begin(LOCAL, choice, player);
        aim(player, choice);
        started = now;
        player.setYBodyRot(player.getYRot());
        Animation drawn = SkeletalClips.get(choice.clip());
        if (drawn != null) {
            PlayerAnimators.play(player, Animator.Priority.HIGH, drawn, 1, false);
        }
    }

    /** How far a strike may carry the body toward its target (blocks), and how close it stops (between edges). */
    private static final double CHASE_MOST = 1.5, CHASE_STOP = 0.45, CHASE_LOOK = 4;
    private static LivingEntity chased;
    private static ResourceLocation chaseClip;
    private static int chaseTicks;
    private static double chasedSoFar;

    /**
     * A strike picks its target (the locked foe, the one under the crosshair, or the foe nearest ahead) and turns to
     * it; until the blow's window opens the body closes the gap (Epic Fight's destination provider), at most
     * {@link #CHASE_MOST} blocks.
     */
    private static void aim(LocalPlayer player, CombatMoves.Choice choice) {
        chased = null;
        if (choice.kind() == CombatState.Kind.DODGE || choice.animation().attack() == null
                || choice.animation().attack().phases().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LivingEntity target = LockOn.target();
        if (target == null && mc.crosshairPickEntity instanceof LivingEntity living && living.distanceTo(player) < CHASE_LOOK) {
            target = living;
        }
        if (target == null) {
            var look = player.getViewVector(1).multiply(1, 0, 1).normalize();
            target = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(CHASE_LOOK),
                    other -> other != player && other.isAlive() && (other instanceof net.minecraft.world.entity.monster.Enemy
                            || other == lastStruck) && look.dot(other.position().subtract(player.position())
                            .multiply(1, 0, 1).normalize()) > 0.7).stream()
                    .min(java.util.Comparator.comparingDouble(other -> other.distanceToSqr(player))).orElse(null);
        }
        if (target == null) {
            return;
        }
        chased = target;
        chaseClip = choice.clip();
        chasedSoFar = 0;
        chaseTicks = Math.max(1, Math.round(choice.animation().attack().phases().get(0).activeFrom() / 0.05f));
        face(player, target);
    }

    private static void face(LocalPlayer player, LivingEntity target) {
        float yaw = (float) Math.toDegrees(Math.atan2(target.getZ() - player.getZ(), target.getX() - player.getX())) - 90;
        player.setYRot(yaw);
        player.setYBodyRot(yaw);
    }

    private static void chase(LocalPlayer player) {
        if (chased == null || !chased.isAlive() || !chaseClip.equals(LOCAL.clip) || chaseTicks <= 0 || LOCAL.hitStop > 0) {
            chased = null;
            return;
        }
        double gap = player.distanceTo(chased) - (player.getBbWidth() + chased.getBbWidth()) / 2 - CHASE_STOP;
        double step = Math.min(Math.min(gap, CHASE_MOST - chasedSoFar) / chaseTicks, 0.45);
        chaseTicks--;
        if (step > 0.01) {
            var toward = chased.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(step);
            player.move(net.minecraft.world.entity.MoverType.SELF, toward);
            chasedSoFar += step;
        }
        face(player, chased);
    }

    /** The predicted fight goes on; an input kept in the buffer starts when it may. */
    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) {
            return;
        }
        long now = player.level().getGameTime();
        if (player.onGround()) {
            LOCAL.airStrike = false;
        }
        Animation playing = LOCAL.animation;
        float before = LOCAL.time;
        boolean frozen = LOCAL.hitStop > 0;
        LOCAL.tick(now);
        if (!frozen && playing != null && playing.rootMotion() != null) {
            rootMotion(player, playing, before, LOCAL.animation == playing ? LOCAL.time : playing.clip().duration());
        }
        chase(player);
        if (LOCAL.buffered != null && LOCAL.free()) {
            CombatState.Input input = LOCAL.takeBuffered(now);
            if (input != null) {
                start(player, CombatMoves.decide(LOCAL, input, LOCAL.bufferedDirection, player, now,
                        ClientCultivation.get().qi()), now);
            }
        }
    }

    /**
     * Moves the local player by its move's root travel (a dodge, a dash, a palm's step), whether or not Shenfa draws
     * the players: the fight moves the body, the drawing only shows it.
     */
    private static void rootMotion(LocalPlayer player, Animation animation, float from, float to) {
        float[] a = animation.clip().value(0, com.murimblock.shenfa.Clip.Path.TRANSLATION, from);
        float[] b = animation.clip().value(0, com.murimblock.shenfa.Clip.Path.TRANSLATION, to);
        if (a == null || b == null) {
            return;
        }
        org.joml.Vector3f move = new org.joml.Vector3f(b[0] - a[0], animation.rootMotion().vertical() ? b[1] - a[1] : 0,
                b[2] - a[2]).mul(com.murimblock.combat.Colliders.pixel(player))
                .rotateY((float) Math.toRadians(-player.yBodyRot));
        player.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(move.x, move.y, move.z));
    }

    /** In combat mode the attack and use keys do nothing of vanilla's: no mining, no swing, no item used. */
    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        if (LOCAL.mode && (event.isAttack() || event.isUseItem())) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    /** No walking while a strike cannot be broken, a dodge carries the body or a blow staggers it. */
    @SubscribeEvent
    public static void movement(MovementInputUpdateEvent event) {
        if (LOCAL.movementLocked()) {
            var input = event.getInput();
            input.forwardImpulse = 0;
            input.leftImpulse = 0;
            input.up = input.down = input.left = input.right = false;
            input.jumping = false;
            event.getEntity().setSprinting(false);
        }
    }

    // Messages from the server.

    public static void anim(CombatPayloads.Anim payload) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level == null ? null : mc.level.getEntity(payload.entity());
        Animator.Priority layer = Animator.Priority.values()[Math.floorMod(payload.layer(),
                Animator.Priority.values().length)];
        ResourceLocation id = payload.clip().isEmpty() ? null : ResourceLocation.tryParse(payload.clip());
        if (entity instanceof AbstractClientPlayer player) {
            if (id == null) {
                PlayerAnimators.stop(player, layer);
                if (player == mc.player && layer == Animator.Priority.HIGH) {
                    LOCAL.end();
                }
            } else {
                Animation animation = SkeletalClips.get(id);
                if (animation != null) {
                    PlayerAnimators.play(player, layer, animation, payload.speed(), payload.mirrored());
                }
            }
        } else if (entity instanceof net.minecraft.world.entity.Mob mob) {
            com.murimblock.shenfa.client.SkeletalMobs.play(mob, layer, id, payload.speed());
        }
    }

    public static void mode(CombatPayloads.Mode payload) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level == null ? null : mc.level.getEntity(payload.entity());
        if (entity instanceof AbstractClientPlayer player) {
            PlayerAnimators.mode(player, payload.on());
            if (player == mc.player) {
                LOCAL.mode = payload.on();
            }
        }
    }

    /** The server's view of the local player's fight: a stagger, or the move it really plays. */
    public static void self(CombatPayloads.Self payload) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (payload.stunTicks() > 0) {
            LOCAL.end();
            LOCAL.buffered = null;
            LOCAL.stunTicks = Math.max(LOCAL.stunTicks, payload.stunTicks());
            if (LOCAL.guarding) {
                guard(player, false);
            }
            return;
        }
        ResourceLocation id = payload.clip().isEmpty() ? null : ResourceLocation.tryParse(payload.clip());
        if (id == null) {
            // The server refused a move this client predicted: it stops.
            if (LOCAL.playing() && LOCAL.kind != CombatState.Kind.REACTION && now - started < 10) {
                LOCAL.end();
                PlayerAnimators.stop(player, Animator.Priority.HIGH);
            }
            return;
        }
        if (!id.equals(LOCAL.clip)) {
            Animation animation = ShenfaLibrary.get(id);
            if (animation == null) {
                return;
            }
            CombatState.Kind kind = Moveset.DODGES.contains(id) ? CombatState.Kind.DODGE : CombatState.Kind.ATTACK;
            LOCAL.start(id, animation, kind);
            LOCAL.time = LOCAL.previousTime = payload.time();
            if (kind == CombatState.Kind.ATTACK) {
                // Its place in the combo too, or the next strike predicted would follow the move the client guessed.
                LOCAL.combo = Moveset.of(player).combo().indexOf(id);
            }
            Animation drawn = SkeletalClips.get(id);
            if (drawn != null) {
                PlayerAnimators.play(player, Animator.Priority.HIGH, drawn, 1, false);
            }
        }
    }

    public static void hit(CombatPayloads.Hit payload) {
        CombatEffects.hit(payload, LOCAL);
    }
}
