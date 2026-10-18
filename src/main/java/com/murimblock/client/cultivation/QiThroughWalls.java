package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Qi vision through walls, from Core Formation (docs/cultivation/refonte-cultivation.md, 18/10/2026): the living within
 * {@link CultivationRules#THROUGH_WALLS_REACH} blocks hidden behind something show through it as Minecraft's glowing
 * outline, drawn over everything, in their aura's colour (QiThroughWallsGlowMixin, QiThroughWallsColourMixin). Who is
 * hidden is worked out once a tick.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class QiThroughWalls {
    /** The hidden ones seen, by entity id: their colour (0xRRGGBB). */
    private static final Int2IntOpenHashMap SEEN = new Int2IntOpenHashMap();

    private QiThroughWalls() {
    }

    /** Whether this entity is seen through walls now. */
    public static boolean seen(Entity e) {
        return SEEN.containsKey(e.getId());
    }

    /** Its outline's colour, or -1. */
    public static int colour(Entity e) {
        return SEEN.getOrDefault(e.getId(), -1);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        SEEN.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !QiSenseClient.open()) return;
        var realm = CultivationService.getRealm(mc.player);
        if (realm.ordinal() < CultivationRules.THROUGH_WALLS_REALM.ordinal()) return;
        double reach = CultivationRules.THROUGH_WALLS_REACH;
        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(reach))) {
            if (e == mc.player || !e.isAlive() || e.isInvisible() || e.isSpectator()) continue;
            if (!CultivationRules.readsThroughWalls(realm, e.distanceToSqr(mc.player)) || mc.player.hasLineOfSight(e)) continue;
            float[] c = QiVisionClient.shown(e);
            SEEN.put(e.getId(), (int) (c[0] * 255) << 16 | (int) (c[1] * 255) << 8 | (int) (c[2] * 255));
        }
    }
}
