package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.combat.knockdown.KnockdownService;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimAnimations.Motions;
import com.murimblock.network.KnockdownActionPayload;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.client.event.types.entity.ModifyPlayerLivingMotionEvent;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * Client side of the knock-down: which Murim animation each player shows (downed, crawling, carrying, carried), their
 * pose, the V (carry / put down) and B (execute) keys next to a downed player, and the prompts. Decides nothing: the
 * server validates every action.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class KnockdownClient {
    /**
     * Walk animation speed above which a player counts as moving. Minecraft keeps it for every player, remote ones too;
     * a plain position check fails because Epic Fight picks the motion at the start of a tick, before anyone moved.
     */
    private static final float MOVING = 0.02F;
    /** Half-angle of the cone in front of the camera where a downed player can be picked, in degrees. */
    private static final double PICK_CONE = 35;

    private KnockdownClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        registerHooks();
    }

    /** Epic Fight hooks, registered once at client setup. */
    static void registerHooks() {
        EpicFightEventHooks.Animation.INIT_ANIMATOR.registerEvent(event -> {
            if (event.getEntityPatch() instanceof PlayerPatch<?>) {
                var animator = event.getAnimator();
                animator.addLivingAnimation(Motions.DOWNED, MurimAnimations.DOWNED_IDLE);
                animator.addLivingAnimation(Motions.CRAWL, MurimAnimations.DOWNED_CRAWL);
                animator.addLivingAnimation(Motions.CARRY, MurimAnimations.CARRY_IDLE);
                animator.addLivingAnimation(Motions.CARRY_WALK, MurimAnimations.CARRY_WALK);
                animator.addLivingAnimation(Motions.CARRIED, MurimAnimations.CARRIED);
            }
        }, "murimblock:knockdown_animations", 0);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_BASE.registerEvent(event -> {
            Motions motion = motion(event.getPlayerPatch().getOriginal());
            if (motion != null) event.setMotion(motion);
        }, "murimblock:knockdown_motion", 0);
        // Nothing layered over these (no sword-holding arms while crawling or carrying).
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_COMPOSITE.registerEvent(event -> {
            Motions motion = motion(event.getPlayerPatch().getOriginal());
            if (motion != null) event.setMotion(motion);
        }, "murimblock:knockdown_composite", 0);
    }

    /** The Murim motion a player shows, or null for Epic Fight's own. */
    @Nullable static Motions motion(Player player) {
        boolean moving = player.walkAnimation.speed() > MOVING;
        if (KnockdownService.isDowned(player)) {
            if (player.isNoGravity()) return null; // swept into the sky by Scarlet Bloom
            if (KnockdownService.isCarried(player)) return Motions.CARRIED;
            return moving ? Motions.CRAWL : Motions.DOWNED;
        }
        if (KnockdownService.isCarrying(player)) return moving ? Motions.CARRY_WALK : Motions.CARRY;
        return null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        for (Player player : minecraft.level.players()) {
            dieWhereTheyLie(player);
            // The local player computes its own pose: give it the crawl the server enforces.
            // Not a body swept into the sky by Scarlet Bloom (weightless meanwhile): it shows its own animation.
            if (KnockdownService.isDowned(player) && !player.isNoGravity()) player.setForcedPose(Pose.SWIMMING);
            else if (player.getForcedPose() == Pose.SWIMMING) player.setForcedPose(null);
            // A carried body faces where its carrier faces.
            Player carrier = KnockdownService.carrier(player);
            if (carrier != null) {
                player.yBodyRot = player.yBodyRotO = carrier.yBodyRot;
                player.yHeadRot = player.yHeadRotO = carrier.yBodyRot;
            }
        }
        while (MurimblockKeyMappings.EXECUTE.consumeClick()) {
            Player target = pickDowned(minecraft);
            if (minecraft.screen == null && target != null && !KnockdownService.isCarried(target)) {
                PacketDistributor.sendToServer(new KnockdownActionPayload(KnockdownActionPayload.Action.EXECUTE, target.getId()));
            }
        }
    }

    /** Players whose death animation is, for now, to stay lying (by entity id). */
    private static final java.util.Set<Integer> LYING_DEATH = new java.util.HashSet<>();

    /**
     * A downed player who is executed dies where they lie: while down, their death "animation" is the downed pose,
     * instead of Epic Fight's death that first lifts the body to its knees.
     */
    private static void dieWhereTheyLie(Player player) {
        boolean downed = KnockdownService.isDowned(player);
        if (downed == LYING_DEATH.contains(player.getId())) return;
        PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        if (patch == null || patch.getClientAnimator() == null) return;
        patch.getClientAnimator().addLivingAnimation(LivingMotions.DEATH, downed ? MurimAnimations.DOWNED_IDLE : Animations.BIPED_DEATH);
        if (downed) LYING_DEATH.add(player.getId());
        else LYING_DEATH.remove(player.getId());
    }

    /**
     * V next to a downed player carries them, and V while carrying puts them down. Returns false when V should keep its
     * usual meaning (combat mode).
     */
    public static boolean handleCarryKey(Minecraft minecraft) {
        LocalPlayer self = minecraft.player;
        if (self == null) return false;
        if (KnockdownService.isCarrying(self)) {
            PacketDistributor.sendToServer(new KnockdownActionPayload(KnockdownActionPayload.Action.RELEASE, -1));
            return true;
        }
        Player target = pickDowned(minecraft);
        if (target == null || KnockdownService.isCarried(target)) return false;
        PacketDistributor.sendToServer(new KnockdownActionPayload(KnockdownActionPayload.Action.CARRY, target.getId()));
        return true;
    }

    /** The downed player closest to the crosshair within reach, if the local player is free to act. */
    @Nullable static Player pickDowned(Minecraft minecraft) {
        LocalPlayer self = minecraft.player;
        if (self == null || minecraft.level == null || KnockdownService.isHindered(self) || self.isPassenger()) return null;
        Vec3 eye = self.getEyePosition(), look = self.getViewVector(1.0F);
        Player best = null;
        double bestAngle = PICK_CONE;
        for (Player other : minecraft.level.players()) {
            if (other == self || !KnockdownService.isDowned(other) || self.distanceTo(other) > KnockdownService.ACTION_REACH) continue;
            Vec3 to = other.getBoundingBox().getCenter().subtract(eye).normalize();
            double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, to.dot(look)))));
            if (angle < bestAngle) { bestAngle = angle; best = other; }
        }
        return best;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        if (self == null || minecraft.options.hideGui || minecraft.screen != null || FinishCinematic.active()) return;
        Component prompt = null;
        if (KnockdownService.isDowned(self)) {
            prompt = KnockdownService.isCarried(self) ? Component.translatable("knockdown.murimblock.hud.carried")
                    : Component.translatable("knockdown.murimblock.hud.downed", (KnockdownService.data(self).ticksLeft() + 19) / 20);
        } else if (KnockdownService.isCarrying(self)) {
            prompt = Component.translatable("knockdown.murimblock.hud.release", MurimblockKeyMappings.COMBAT_MODE.getTranslatedKeyMessage());
        } else {
            Player target = pickDowned(minecraft);
            if (target != null && !KnockdownService.isCarried(target)) {
                prompt = Component.translatable("knockdown.murimblock.hud.actions",
                        MurimblockKeyMappings.COMBAT_MODE.getTranslatedKeyMessage(), MurimblockKeyMappings.EXECUTE.getTranslatedKeyMessage());
            }
        }
        if (prompt == null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.font.width(prompt);
        // Above the action bar (its messages sit at guiHeight - 68), so the two never overlap.
        int x = (graphics.guiWidth() - width) / 2, y = graphics.guiHeight() - 92;
        graphics.fill(x - 4, y - 3, x + width + 4, y + 11, 0x88000000);
        graphics.drawString(minecraft.font, prompt, x, y, 0xFFF3EBD4);
    }
}
