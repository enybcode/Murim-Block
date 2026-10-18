package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Qi vision's foresight, from Qi Crystallization (docs/cultivation/refonte-cultivation.md, 18/10/2026): an enemy's Qi
 * gathers where its blow will land before it lands. While an attack of Epic Fight's winds up (from the phase's start to
 * its contact), motes of the attacker's Qi are drawn in to a point before it, at the height of the blow, more and more
 * and brighter as the moment nears; at the contact, it flashes. Enough to time a perfect parry by.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiForesight {
    /** The realm the foresight comes with. */
    public static final CultivationRealm REALM = CultivationRealm.QI_CRYSTALLIZATION;
    /** The foresight's colour. */
    static final float[] GOLD = {1.0F, 0.82F, 0.08F};
    /** How far the blows are foreseen (blocks). */
    static final double REACH = 24;

    private QiForesight() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.isPaused() || !QiSenseClient.open()) return;
        if (CultivationService.getRealm(mc.player).ordinal() < REALM.ordinal()) return;
        RandomSource random = mc.level.random;
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(REACH))) {
            if (e == mc.player) continue;
            float near = windup(e);
            if (near < 0) continue;
            // Bright gold, whatever the aura's colour: it must stand out from the aura (and stay a colour, so the vision's grey
            // leaves it).
            float[] c = GOLD;
            Vec3 strike = strikePoint(e);
            if (near >= 1) {
                // The contact: the gathered Qi bursts.
                for (int i = 0; i < 18; i++) {
                    double a = random.nextDouble() * Math.PI * 2, b = random.nextDouble() * Math.PI - Math.PI / 2;
                    AuraParticle p = AuraClient.spark(mc.level, strike.x, strike.y, strike.z,
                            Math.cos(a) * Math.cos(b) * 0.25, Math.sin(b) * 0.25, Math.sin(a) * Math.cos(b) * 0.25, c, false);
                    if (p != null) p.big(0.3F, 10);
                }
                continue;
            }
            // Drawn in: more motes and nearer as the blow comes; the point itself swelling.
            int count = 2 + Math.round(near * 6);
            for (int i = 0; i < count; i++) {
                double a = random.nextDouble() * Math.PI * 2, b = (random.nextDouble() - 0.5) * Math.PI, d = 2.0 - near * 1.2;
                Vec3 from = strike.add(Math.cos(a) * Math.cos(b) * d, Math.sin(b) * d, Math.sin(a) * Math.cos(b) * d);
                Vec3 in = strike.subtract(from).scale(0.18);
                AuraParticle p = AuraClient.spark(mc.level, from.x, from.y, from.z, in.x, in.y, in.z, c, false);
                if (p != null) p.big(0.14F + 0.1F * near, 7);
            }
            AuraParticle core = AuraClient.spark(mc.level, strike.x, strike.y, strike.z, 0, 0, 0, c, false);
            if (core != null) core.big(0.2F + 0.6F * near, 2);
        }
    }

    /**
     * How far into the wind-up of an attack the entity is: from 0 at its phase's start to 1 at its contact (and a tick
     * past it, 1); -1 if it is not winding up a blow.
     */
    static float windup(LivingEntity e) {
        LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(e, LivingEntityPatch.class);
        if (patch == null) return -1;
        var player = patch.getAnimator().getPlayerFor(null);
        if (player == null || player.isEmpty() || !(player.getRealAnimation().get() instanceof AttackAnimation attack)) return -1;
        float t = player.getElapsedTime();
        AttackAnimation.Phase phase = attack.getPhaseByTime(t);
        if (phase == null || t > phase.contact + 0.06F) return -1;
        if (t >= phase.contact) return 1;
        return Math.max(0, Math.min(1, (t - phase.start) / Math.max(0.05F, phase.contact - phase.start)));
    }

    /** Where the blow will land: before the attacker, at the height of its chest, as far as it reaches. */
    static Vec3 strikePoint(LivingEntity e) {
        Vec3 look = Vec3.directionFromRotation(0, e.getYRot());
        double reach = e.getBbWidth() * 0.5 + 1.3;
        // Facing the viewer and nearer than its reach: the blow lands on the viewer, just before their body.
        var viewer = Minecraft.getInstance().player;
        if (viewer != null && viewer != e) {
            Vec3 to = viewer.position().subtract(e.position()).multiply(1, 0, 1);
            double d = to.length();
            if (d > 0.1 && d < reach + viewer.getBbWidth() && to.normalize().dot(look) > 0.6) reach = Math.max(0.5, d - viewer.getBbWidth() * 0.6);
        }
        return e.position().add(look.scale(reach)).add(0, e.getBbHeight() * 0.62, 0);
    }
}
