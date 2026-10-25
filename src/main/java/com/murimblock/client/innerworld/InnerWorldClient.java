package com.murimblock.client.innerworld;

import com.murimblock.Murimblock;
import com.murimblock.innerworld.InnerWorld;
import com.murimblock.innerworld.InnerWorldEvents;
import com.murimblock.innerworld.OldMaster;
import com.murimblock.registry.MurimEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client side of the inner world: the body's renderer, the travel request, and no pick-block copies inside. */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class InnerWorldClient {
    private InnerWorldClient() {
    }

    public static boolean inside() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && InnerWorld.isInnerWorld(mc.level);
    }

    public static void travel() {
        PacketDistributor.sendToServer(new InnerWorldEvents.Travel());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.MEDITATING_BODY.get(), MeditatingBodyRenderer::new);
        event.registerEntityRenderer(MurimEntities.OLD_MASTER.get(), OldMasterRenderer::new);
        event.registerEntityRenderer(MurimEntities.TRIAL_ZOMBIE.get(), TrialZombieRenderer::new);
    }

    /** Right-clicking the Old Master opens the talk; nothing is sent to the server. */
    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() && event.getTarget() instanceof OldMaster master) {
            if (event.getHand() == InteractionHand.MAIN_HAND) {
                Minecraft.getInstance().setScreen(new OldMasterScreen(master));
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /**
     * The inner world dimension makes vanilla call every world "experimental": a warning when creating one and a
     * backup question on its first opening. Both are answered for the player (no backup), as if they had clicked.
     */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        String button = event.getScreen() instanceof BackupConfirmScreen && isTitled(event.getScreen(), "selectWorld.backupQuestion.experimental")
                ? "selectWorld.backupJoinSkipButton"
                : event.getScreen() instanceof ConfirmScreen && isTitled(event.getScreen(), "selectWorld.warning.experimental.title")
                ? "gui.yes" : null;
        if (button == null) {
            return;
        }
        for (GuiEventListener child : event.getScreen().children()) {
            if (child instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents t
                    && t.getKey().equals(button)) {
                Murimblock.LOGGER.info("Skipped the experimental world warning ({})", button);
                // Not during init: pressing replaces the screen being set up.
                Minecraft.getInstance().tell(b::onPress);
                return;
            }
        }
    }

    private static boolean isTitled(Screen screen, String key) {
        return screen.getTitle().getContents() instanceof TranslatableContents t && t.getKey().equals(key);
    }

    /** Creative pick-block would hand out a copy of the island's blocks. */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isPickBlock() && inside()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }
}
