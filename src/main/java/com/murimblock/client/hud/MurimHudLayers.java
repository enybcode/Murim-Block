package com.murimblock.client.hud;

import com.murimblock.Murimblock;
import com.murimblock.client.CombatBarClient;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Murimblock's own HUD layers: the combat bar above the hotbar, the Qi bar above the experience bar. Their own layers,
 * not drawn inside the vanilla ones' events, so another mod hiding or replacing a vanilla layer leaves them be.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class MurimHudLayers {
    public static final ResourceLocation COMBAT_BAR = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "combat_bar");
    public static final ResourceLocation QI_BAR = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "qi_bar");
    public static final ResourceLocation CRESCENT_MOON = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "crescent_moon");

    private MurimHudLayers() {
    }

    @SubscribeEvent
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, COMBAT_BAR, (g, delta) -> CombatBarClient.renderLayer(g));
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, QI_BAR, (g, delta) -> CombatQiHud.renderLayer(g));
        // The Crescent Moon Halberd: its moon by the crosshair, the night of its ultimate over the screen.
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, CRESCENT_MOON, (g, delta) -> com.murimblock.client.CrescentMoonClient.renderLayer(g));
    }
}
