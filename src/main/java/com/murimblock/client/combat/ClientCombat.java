package com.murimblock.client.combat;

import com.murimblock.Murimblock;
import com.murimblock.client.cultivation.ClientCultivation;
import com.murimblock.combat.CombatMove;
import com.murimblock.combat.CombatMoves;
import com.murimblock.combat.CombatPayloads;
import com.murimblock.combat.Fighter;
import com.murimblock.combat.FighterState;
import com.murimblock.combat.StrikeReach;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Bare-handed combat on the client. Our own strike is predicted: the animation, the step and the impact play at once,
 * and the server, which deals the damage, may refuse it. Others' strikes and impacts come from the server.
 */
public final class ClientCombat {
    /** PAL layer for combat, above the lotus and getting-up layer so the two never fight over it. */
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "combat");
    private static final int LAYER_PRIORITY = 1100;
    /** Ground friction turns a push into a distance: a push of v slides about v / (1 - 0.546) blocks. */
    private static final double STEP_TO_PUSH = 1 - 0.546;
    private static final int SHAKE_TICKS = 5;
    private static final float SHAKE_DEGREES = 1.6f;

    private static final Fighter OWN = new Fighter();
    /** Players whose animation is held by hit-stop, with the ticks left. */
    private static final Map<Integer, Integer> HELD = new HashMap<>();
    private static int shake;

    private ClientCombat() {
    }

    /** Our own fighter state, as predicted. */
    public static FighterState ownState() {
        return OWN.clock().state();
    }

    /** The left click with bare hands: strike unless a block is aimed at (that one is mined). */
    private static boolean tryStrike(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || !player.getMainHandItem().isEmpty() || player.isSpectator()
                || mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            return false;
        }
        if (ClientCultivation.get().meditating() || !OWN.clock().canStrike(0)) {
            return true;
        }
        CombatMove move = CombatMoves.get(CombatMoves.JAB);
        if (move == null) {
            return false;
        }
        OWN.strike(CombatMoves.JAB, move);
        OWN.look(player.getYRot(), player.getXRot());
        player.setSprinting(false);
        play(player, move.animation());
        PacketDistributor.sendToServer(new CombatPayloads.StrikeRequest(CombatMoves.JAB));
        return true;
    }

    private static void tickOwn(LocalPlayer player) {
        CombatMove move = OWN.move();
        if (OWN.clock().starting() && move.step() > 0 && player.onGround()) {
            Vec3 forward = Vec3.directionFromRotation(0, player.getYRot()).scale(move.step() * STEP_TO_PUSH);
            player.push(forward.x, 0, forward.z);
        }
        if (OWN.clock().active()) {
            for (LivingEntity target : StrikeReach.targets(player, move, OWN.lastYaw(), OWN.lastPitch())) {
                if (OWN.clock().markStruck(target.getId())) {
                    OWN.clock().hitStop(move.hitStop());
                    hold(player.getId(), move.hitStop());
                    impact(target);
                    shake = SHAKE_TICKS;
                }
            }
        }
        OWN.look(player.getYRot(), player.getXRot());
        OWN.clock().advance();
    }

    public static void refused() {
        Minecraft mc = Minecraft.getInstance();
        OWN.reset();
        if (mc.player != null && controller(mc.player) instanceof PlayerAnimationController controller) {
            controller.stopTriggeredAnimation();
        }
    }

    public static void strikeSeen(int entityId, ResourceLocation id) {
        CombatMove move = CombatMoves.get(id);
        Minecraft mc = Minecraft.getInstance();
        if (move != null && mc.level != null && mc.level.getEntity(entityId) instanceof AbstractClientPlayer player) {
            play(player, move.animation());
        }
    }

    public static void impactSeen(int attackerId, int targetId, int hitStop) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        hold(attackerId, hitStop);
        if (mc.level.getEntity(targetId) instanceof Entity target) {
            impact(target);
            if (target == mc.player) {
                shake = SHAKE_TICKS;
            }
        }
    }

    private static void impact(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        RandomSource random = target.getRandom();
        Vec3 at = target.position().add(0, target.getBbHeight() * 0.65, 0);
        mc.level.playLocalSound(at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1,
                0.9f + random.nextFloat() * 0.2f, false);
        for (int i = 0; i < 10; i++) {
            mc.level.addParticle(ParticleTypes.CRIT, at.x, at.y, at.z, random.nextGaussian() * 0.4,
                    random.nextGaussian() * 0.3 + 0.1, random.nextGaussian() * 0.4);
        }
        mc.level.addParticle(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 0, 0, 0);
    }

    private static void play(AbstractClientPlayer player, ResourceLocation animation) {
        if (controller(player) instanceof PlayerAnimationController controller) {
            controller.unpause();
            HELD.remove(player.getId());
            controller.triggerAnimation(animation);
        }
    }

    /** Hit-stop: the striker's animation stands still for a few ticks. */
    private static void hold(int entityId, int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (ticks > 0 && mc.level != null && mc.level.getEntity(entityId) instanceof AbstractClientPlayer player
                && controller(player) instanceof PlayerAnimationController controller) {
            controller.pause();
            HELD.put(entityId, ticks);
        }
    }

    private static Object controller(AbstractClientPlayer player) {
        return PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER);
    }

    private static void releaseHeld(Minecraft mc) {
        for (Iterator<Map.Entry<Integer, Integer>> it = HELD.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Integer> held = it.next();
            if (held.setValue(held.getValue() - 1) > 1) {
                continue;
            }
            it.remove();
            if (mc.level.getEntity(held.getKey()) instanceof AbstractClientPlayer player
                    && controller(player) instanceof PlayerAnimationController controller) {
                controller.unpause();
            }
        }
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, LAYER_PRIORITY, player -> {
                PlayerAnimationController controller =
                        new PlayerAnimationController(player, (c, data, setter) -> PlayState.STOP);
                // In first person the arms of the third person model play the strike.
                controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
                controller.setFirstPersonConfiguration(new FirstPersonConfiguration(true, true, true, true));
                return controller;
            });
        }

        @SubscribeEvent
        public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
            if (event.isAttack() && tryStrike(Minecraft.getInstance())) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.isPaused()) {
                return;
            }
            if (OWN.move() != null) {
                tickOwn(mc.player);
            }
            releaseHeld(mc);
            if (shake > 0) {
                shake--;
            }
        }

        /** Striking slows the feet. */
        @SubscribeEvent
        public static void onMovementInput(MovementInputUpdateEvent event) {
            if (OWN.move() != null) {
                float speed = (float) OWN.move().moveSpeed();
                event.getInput().forwardImpulse *= speed;
                event.getInput().leftImpulse *= speed;
            }
        }

        @SubscribeEvent
        public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
            if (shake <= 0) {
                return;
            }
            float time = (float) (shake - event.getPartialTick());
            float strength = SHAKE_DEGREES * Math.max(0, time) / SHAKE_TICKS;
            event.setPitch(event.getPitch() + Mth.sin(time * 4.1f) * strength);
            event.setRoll(event.getRoll() + Mth.sin(time * 5.7f) * strength);
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            OWN.reset();
            HELD.clear();
            shake = 0;
        }
    }
}
