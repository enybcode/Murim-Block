package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.QiTraces;
import com.murimblock.network.CultivationPayloads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Qi vision's hidden, from Nascent Soul (docs/cultivation/refonte-cultivation.md, 18/10/2026):
 * <ul>
 *   <li>the trails of the powerful (QiTraces, sent by the server): a mote on the ground every 4 to 8 blocks where they
 *   walked, in the walker's aura colour, smaller as it ages;</li>
 *   <li>the caches (QiTraces.caches, sent by the server, which alone knows what is inside): a chest or barrel near, hidden
 *   from sight (buried, walled in), with something in it, breathes a thin thread of amber Qi up out of the surface above
 *   it, where to dig.</li>
 * </ul>
 * Illusory walls come when there are some.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiHiddenClient {
    /** A cache's Qi: amber, apart from the foresight's gold. */
    static final float[] AMBER = {1.0F, 0.55F, 0.05F};
    /** A creature's trail (a boss): red. */
    static final float[] BEAST = {1.0F, 0.15F, 0.1F};

    private static List<CultivationPayloads.Trace> traces = List.of();
    private static long received;
    /** Where each hidden cache's thread rises from (the surface above it). */
    private static final List<Vec3> CACHES = new ArrayList<>();

    private QiHiddenClient() {
    }

    public static void traces(CultivationPayloads.Traces payload) {
        Minecraft mc = Minecraft.getInstance();
        traces = payload.traces();
        CACHES.clear();
        for (BlockPos p : payload.caches()) CACHES.add(new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5));
        received = mc.level == null ? 0 : mc.level.getGameTime();
    }

    /** For the tours: points of trail and caches shown now. */
    public static int footprints() {
        return traces.size();
    }

    public static int caches() {
        return CACHES.size();
    }

    static boolean on(Minecraft mc) {
        return mc.player != null && mc.level != null && QiSenseClient.open()
                && CultivationService.getRealm(mc.player).ordinal() >= QiTraces.REALM.ordinal();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!on(mc)) {
            traces = List.of();
            CACHES.clear();
            return;
        }
        if (mc.isPaused()) return;
        long time = mc.level.getGameTime();
        RandomSource random = mc.level.random;
        // Once a second, each mote living a little longer than that: few motes, always some shown.
        if (time % 20 == 0) trails(mc, random, (int) (time - received));
        for (Vec3 c : CACHES) {
            if (time % 3 == 0) {
                AuraParticle p = AuraClient.spark(mc.level, c.x + (random.nextDouble() - 0.5) * 0.5, c.y, c.z + (random.nextDouble() - 0.5) * 0.5,
                        0, 0.04 + random.nextDouble() * 0.03, 0, AMBER, false);
                if (p != null) p.big(0.1F + random.nextFloat() * 0.06F, 40);
            }
            if (time % 8 == 0) {
                AuraParticle core = AuraClient.spark(mc.level, c.x, c.y + 0.05, c.z, 0, 0, 0, AMBER, false);
                if (core != null) core.big(0.3F, 9);
            }
        }
    }

    /** The trails: a mote just above the ground at each point (one every 4 to 8 blocks walked); the older, the smaller. */
    static void trails(Minecraft mc, RandomSource random, int since) {
        for (CultivationPayloads.Trace t : traces) {
            float fresh = 1 - Math.min(1, (t.age() + since) / (float) QiTraces.LIFE_TICKS);
            if (fresh <= 0) continue;
            float[] c = t.art().isEmpty() ? BEAST : t.art().equals("none") ? new float[]{0.35F, 0.75F, 1.0F} : AuraClient.colour(AuraClient.artOf(t.art()));
            AuraParticle m = AuraClient.spark(mc.level, t.x(), t.y() + 0.12, t.z(), 0, 0, 0, c, true);
            if (m != null) m.big(0.1F + 0.1F * fresh, 28).still();
        }
    }
}
