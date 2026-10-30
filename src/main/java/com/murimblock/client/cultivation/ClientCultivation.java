package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.CultivationRules;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** The client's copy of its own cultivation, and who meditates (the lotus itself is {@link LotusBody}). */
public final class ClientCultivation {

    private static Cultivation.Snapshot current = Cultivation.Snapshot.START;
    /** Meditating players by entity id, with the Qi each holds. */
    private static final Map<Integer, Integer> MEDITATING = new HashMap<>();

    private ClientCultivation() {
    }

    public static Cultivation.Snapshot get() {
        return current;
    }

    public static void receive(Cultivation.Snapshot snapshot) {
        Cultivation.Snapshot before = current;
        current = snapshot;
        // Buttons are built once per layout: rebuild them when what they show changes, not on every Qi tick.
        if (Minecraft.getInstance().screen instanceof SystemScreen screen && buttonsChanged(before, snapshot)) {
            screen.refresh();
        }
    }

    private static boolean buttonsChanged(Cultivation.Snapshot a, Cultivation.Snapshot b) {
        return a.meditating() != b.meditating() || a.realm() != b.realm() || a.stage() != b.stage()
                || a.meridians() != b.meridians()
                || CultivationRules.gaugeFull(a.realm(), a.stage(), a.stageQi())
                != CultivationRules.gaugeFull(b.realm(), b.stage(), b.stageQi());
    }

    /** Whether this player sits in meditation, as last told by the server. */
    public static boolean isMeditating(Entity player) {
        return MEDITATING.containsKey(player.getId());
    }

    /** Qi held by a meditating player: our own from the full sync, others' as last sent with their pose. */
    public static double qiOf(Entity player) {
        if (player == Minecraft.getInstance().player) {
            return current.qi();
        }
        return MEDITATING.getOrDefault(player.getId(), 0);
    }

    /** The Qi a meditating player holds now, sent every second of meditation. */
    public static void meditatorQi(int entityId, int qi) {
        MEDITATING.computeIfPresent(entityId, (id, old) -> qi);
    }

    public static void pose(int entityId, boolean meditating, int qi) {
        if (meditating) {
            MEDITATING.put(entityId, qi);
        } else {
            MEDITATING.remove(entityId);
        }
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level == null ? null : mc.level.getEntity(entityId);
        if (entity instanceof AbstractClientPlayer player) {
            LotusBody.seat(player, meditating);
        }
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            current = Cultivation.Snapshot.START;
            MEDITATING.clear();
        }
    }
}
