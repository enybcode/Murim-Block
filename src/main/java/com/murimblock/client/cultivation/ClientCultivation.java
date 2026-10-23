package com.murimblock.client.cultivation;

import com.murimblock.Murimblock;
import com.murimblock.client.gui.SystemScreen;
import com.murimblock.cultivation.Cultivation;
import com.murimblock.cultivation.CultivationRules;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.enums.PlayState;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** The client's copy of its own cultivation, and the lotus pose of meditating players. */
public final class ClientCultivation {
    /** PAL layer playing Murimblock animations on players. */
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "main");
    public static final ResourceLocation LOTUS = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "meditate_lotus");
    private static final int LAYER_PRIORITY = 1000;

    private static Cultivation.Snapshot current = Cultivation.Snapshot.START;
    private static final Set<Integer> MEDITATING = new HashSet<>();

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
        return MEDITATING.contains(player.getId());
    }

    public static void pose(int entityId, boolean meditating) {
        if (meditating) {
            MEDITATING.add(entityId);
        } else {
            MEDITATING.remove(entityId);
        }
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level == null ? null : mc.level.getEntity(entityId);
        if (entity instanceof AbstractClientPlayer player
                && PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER) instanceof PlayerAnimationController controller) {
            if (meditating) {
                controller.triggerAnimation(LOTUS);
            } else {
                controller.stopTriggeredAnimation();
            }
        }
    }

    @EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void setup(FMLClientSetupEvent event) {
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, LAYER_PRIORITY,
                    player -> new PlayerAnimationController(player, (controller, data, setter) -> PlayState.STOP));
        }

        @SubscribeEvent
        public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            current = Cultivation.Snapshot.START;
            MEDITATING.clear();
        }
    }
}
