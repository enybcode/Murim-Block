package com.murimblock.client;

import com.lowdragmc.photon.client.light.DynamicLight;
import com.lowdragmc.photon.client.light.PhotonLights;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.martial.SanguineArt;
import com.murimblock.martial.SanguineParticles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

/**
 * The Sanguine Art's client side: its bare-hand guard pose and a brief blood-red Photon light at the peak of a throw,
 * a Scarlet Tear, the crystallization and a barrage, on the same Epic Fight clock on every client. No particles: at
 * Enzo's request (14/10/2026) the art shows only its animations, crystals and armour.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class SanguineEffects {
    /** Light flashed once per move, per player: the move's kind and how far its animation has played. */
    private static final Map<UUID, Seen> SEEN = new HashMap<>();
    private static ClientLevel world;
    private record Motion(int kind, float time) { }
    private static final class Seen {
        final int kind;
        float last;
        boolean flashed;
        Seen(int kind, float time) { this.kind = kind; last = time; }
    }
    private SanguineEffects() { }

    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        EpicFightEventHooks.Animation.INIT_ANIMATOR.registerEvent(e -> {
            if (e.getEntityPatch() instanceof PlayerPatch<?>) e.getAnimator().addLivingAnimation(MurimAnimations.Motions.SANGUINE_GUARD, MurimAnimations.SANGUINE_GUARD);
        }, "murimblock:sanguine_guard", 0);
        EpicFightClientEventHooks.Entity.MODIFY_PLAYER_LIVING_MOTION_COMPOSITE.registerEvent(e -> {
            var patch = e.getPlayerPatch();
            if ((e.getMotion() == LivingMotions.BLOCK || patch.getHoldingSkill() instanceof GuardSkill)
                    && SanguineArt.guardsBareHanded(patch.getOriginal())) e.setMotion(MurimAnimations.Motions.SANGUINE_GUARD);
        }, "murimblock:sanguine_guard", -10);
    }

    /** The blows' hit "particle" draws nothing. */
    @SubscribeEvent public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpecial(SanguineParticles.NO_HIT.get(), (type, level, x, y, z, dx, dy, dz) -> null);
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level != world) { SEEN.clear(); world = mc.level; }
        if (world == null || mc.isPaused() || mc.player == null) return;
        Set<UUID> seen = new HashSet<>();
        for (Player player : world.players()) {
            if (!player.isAlive() || player.distanceToSqr(mc.player) > 48 * 48) continue;
            Motion motion = motion(EpicFightBridge.patch(player));
            if (motion == null) continue;
            Seen state = SEEN.get(player.getUUID());
            if (state != null && (state.kind != motion.kind || motion.time + 0.001F < state.last)) state = null;
            if (state == null) { state = new Seen(motion.kind, motion.time); SEEN.put(player.getUUID(), state); }
            seen.add(player.getUUID());
            flash(state, player, motion.time);
            state.last = motion.time;
        }
        SEEN.keySet().retainAll(seen);
    }

    private static Motion motion(PlayerPatch<?> patch) {
        if (patch == null) return null;
        var animations = java.util.List.of(MurimAnimations.SANGUINE_CRYSTAL_THROW, MurimAnimations.SCARLET_TEAR,
                MurimAnimations.SANGUINE_CRYSTAL_FORM, MurimAnimations.SANGUINE_CRYSTAL_BARRAGE);
        int[] kinds = {1, 2, 3, 6};
        for (int i = 0; i < animations.size(); i++) {
            var animation = animations.get(i); var clock = patch.getAnimator().getPlayerFor(animation);
            if (clock != null && !clock.isEmpty() && clock.getRealAnimation() == animation) return new Motion(kinds[i], clock.getElapsedTime());
        }
        return null;
    }

    private static Vec3 point(Player p, double ahead) {
        Vec3 forward = Vec3.directionFromRotation(0, p.getYRot());
        return p.position().add(forward.x * ahead, 1, forward.z * ahead);
    }

    /** The light at the move's peak, once: a throw, a tear, the crystallization (stronger) or a barrage. */
    private static void flash(Seen s, Player p, float t) {
        float peak = s.kind == 1 ? .30F : s.kind == 2 ? .43F : s.kind == 3 ? .55F : .30F;
        if (s.flashed || s.last >= peak || t < peak) return;
        s.flashed = true;
        if (!PhotonLights.isEnabled()) return;
        Vec3 at = point(p, s.kind == 3 ? 2.0 : 1.0);
        PhotonLights.flash(new DynamicLight().point().at(at.x, at.y, at.z).color(.55F, .018F, .025F)
                .intensity(s.kind == 3 ? 2.2F : 1.0F).range(s.kind == 3 ? 3 : 1.8F).sourceRadius(.2F), 4);
    }

    /** Development tour evidence: how many players' moves are followed for their light (bounded by the players near). */
    public static int followed() { return SEEN.size(); }
}
