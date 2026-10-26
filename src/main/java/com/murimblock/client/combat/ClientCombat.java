package com.murimblock.client.combat;

import com.murimblock.Murimblock;
import com.murimblock.combat.CombatMove;
import com.murimblock.combat.CombatMoves;
import com.murimblock.combat.CombatMoveset;
import com.murimblock.combat.CombatMovesets;
import com.murimblock.combat.CombatPayloads;
import com.murimblock.combat.Fighter;
import com.murimblock.combat.FighterState;
import com.murimblock.combat.StrikeReach;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.enums.PlayState;
import java.util.Objects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Bare-handed combat on the client. Our own strike is predicted: the animation, the step and the impact play at once,
 * and the server, which deals the damage, may refuse it. Others' strikes and impacts come from the server.
 */
public final class ClientCombat {
    /** PAL layer for combat, above the lotus and getting-up layer so the two never fight over it. */
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "combat");
    private static final int LAYER_PRIORITY = 1100;
    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "combat_mode");
    /** Ground friction turns a push into a distance: a push of v slides about v / (1 - 0.546) blocks. */
    private static final double STEP_TO_PUSH = 1 - 0.546;
    private static final int SHAKE_TICKS = 5;
    private static final float SHAKE_DEGREES = 1.6f;

    private static final Fighter OWN = new Fighter();
    private static int shake;
    private static boolean combatMode;
    /** Others' strikes seen and blows felt from the server, read by the duel test. */
    private static int strikesSeen;
    private static int blowsFelt;

    private ClientCombat() {
    }

    public static int strikesSeen() {
        return strikesSeen;
    }

    public static int blowsFelt() {
        return blowsFelt;
    }

    /** Our own fighter state, as predicted. */
    public static FighterState ownState() {
        return OWN.clock().state();
    }

    /** Right of the hotbar while combat mode is on: a small crimson emblem. */
    private static void renderHud(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!combatMode || mc.options.hideGui || mc.player == null || mc.player.isSpectator()) {
            return;
        }
        int x = graphics.guiWidth() / 2 + 91 + 8;
        int y = graphics.guiHeight() - 17;
        // A diamond of 13 px: dark rim, crimson heart, a pale glint at its center.
        for (int row = 0; row < 13; row++) {
            int half = 6 - Math.abs(6 - row);
            graphics.fill(x + 6 - half, y + row, x + 7 + half, y + row + 1, 0xFF2A0608);
            if (half > 1) {
                graphics.fill(x + 8 - half, y + row, x + 5 + half, y + row + 1, 0xFFB3141E);
            }
        }
        graphics.fill(x + 5, y + 5, x + 8, y + 8, 0xFFFF6A5A);
    }

    public static boolean inCombatMode() {
        return combatMode;
    }

    /** The server switched combat mode; {@code reason} is a lang key, empty for the player's own toggle. */
    public static void combatMode(boolean on, String reason) {
        Minecraft mc = Minecraft.getInstance();
        boolean changed = combatMode != on;
        combatMode = on;
        if (!on) {
            refused();
        }
        if (mc.player != null && (changed || !reason.isEmpty())) {
            String key = !reason.isEmpty() ? reason : on ? "message.murimblock.combat.on" : "message.murimblock.combat.off";
            mc.player.displayClientMessage(Component.translatable(key), true);
        }
    }

    /** A left click in combat mode: always a strike, never mining. */
    private static void tryStrike(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || !OWN.clock().canStrike(0)) {
            return;
        }
        // The style of what is held picks the move; the chain goes past its first move in a later step.
        ResourceLocation styleId = CombatMovesets.forHeld(player.getMainHandItem());
        CombatMoveset style = CombatMovesets.get(styleId);
        if (style == null) {
            return;
        }
        ResourceLocation id = style.chainMove(0);
        CombatMove move = CombatMoves.get(id);
        if (move == null) {
            return;
        }
        if (!CombatAnimator.canPlay(player, FighterState.ATTACK)) {
            return;
        }
        OWN.style(styleId);
        OWN.strike(id, move);
        OWN.look(player.getYRot(), player.getXRot());
        player.setSprinting(false);
        CombatAnimator.play(player, FighterState.ATTACK, move.animation(), move.length());
        PacketDistributor.sendToServer(new CombatPayloads.StrikeRequest(id));
    }

    /** A change of weapon stops what the old one was throwing, as on the server. */
    private static void checkStyle(LocalPlayer player) {
        ResourceLocation styleId = CombatMovesets.forHeld(player.getMainHandItem());
        if (OWN.move() != null && !Objects.equals(styleId, OWN.style())) {
            refused();
        }
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
                    CombatAnimator.hold(player, move.hitStop());
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
        if (mc.player != null) {
            CombatAnimator.stop(mc.player);
        }
    }

    public static void strikeSeen(int entityId, ResourceLocation id) {
        CombatMove move = CombatMoves.get(id);
        Minecraft mc = Minecraft.getInstance();
        if (move != null && mc.level != null && mc.level.getEntity(entityId) instanceof AbstractClientPlayer player) {
            CombatAnimator.play(player, FighterState.ATTACK, move.animation(), move.length());
            strikesSeen++;
        }
    }

    public static void impactSeen(int attackerId, int targetId, int hitStop) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (mc.level.getEntity(attackerId) instanceof AbstractClientPlayer attacker) {
            CombatAnimator.hold(attacker, hitStop);
        }
        if (mc.level.getEntity(targetId) instanceof Entity target) {
            impact(target);
            if (target == mc.player) {
                shake = SHAKE_TICKS;
                blowsFelt++;
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

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, LAYER_PRIORITY,
                    player -> new PlayerAnimationController(player, (c, data, setter) -> PlayState.STOP));
        }

        /** In combat mode we read the clicks before vanilla does: each one is a strike. */
        @SubscribeEvent
        public static void onClientTickPre(ClientTickEvent.Pre event) {
            Minecraft mc = Minecraft.getInstance();
            if (!combatMode || mc.player == null || mc.screen != null) {
                return;
            }
            while (mc.options.keyAttack.consumeClick()) {
                tryStrike(mc);
            }
        }

        /** And vanilla gets nothing of the left click: no punch, no mining, even with the button held. */
        @SubscribeEvent
        public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
            if (event.isAttack() && combatMode) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
        }

        /** During a strike our own arms replace the vanilla hand. */
        @SubscribeEvent
        public static void onRenderHand(RenderHandEvent event) {
            Minecraft mc = Minecraft.getInstance();
            CombatMove move = OWN.move();
            if (move == null || move.firstPerson() == CombatMove.FirstPerson.NONE || mc.player == null) {
                return;
            }
            event.setCanceled(true);
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                FirstPersonArms.render(event, mc.player, move.firstPerson(),
                        OWN.clock().time((float) event.getPartialTick()));
            }
        }

        @SubscribeEvent
        public static void registerHud(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.HOTBAR, HUD, ClientCombat::renderHud);
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.isPaused()) {
                return;
            }
            checkStyle(mc.player);
            if (OWN.move() != null) {
                tickOwn(mc.player);
            }
            CombatAnimator.tick(mc);
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
            combatMode = false;
            CombatAnimator.clear();
            shake = 0;
        }
    }
}
