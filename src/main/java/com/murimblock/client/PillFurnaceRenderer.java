package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.alchemy.AlchemyItems;
import com.murimblock.alchemy.PillFurnaceBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Draws the pill furnace (PillFurnaceBlockEntity) with GeckoLib from geo/block/plant/alchemy_cauldron_stage0: its fire,
 * its runes and its column of light glow (glowmask); idle, refining or finishing as its block entity says. Also opens
 * its fire screen (PillFurnaceScreen).
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class PillFurnaceRenderer extends GeoBlockRenderer<PillFurnaceBlockEntity> {
    public PillFurnaceRenderer() {
        super(new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override public AABB getRenderBoundingBox(PillFurnaceBlockEntity furnace) {
        return furnace.renderBox();
    }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AlchemyItems.FURNACE_ENTITY.get(), context -> new PillFurnaceRenderer());
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(AlchemyItems.FURNACE_MENU.get(), PillFurnaceScreen::new);
    }

    private static final class Model extends GeoModel<PillFurnaceBlockEntity> {
        private static ResourceLocation asset(String path) {
            return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
        }

        @Override public ResourceLocation getModelResource(PillFurnaceBlockEntity furnace) { return asset("geo/block/plant/alchemy_cauldron_stage0.geo.json"); }
        @Override public ResourceLocation getTextureResource(PillFurnaceBlockEntity furnace) { return asset("textures/block/plant/alchemy_cauldron_bronze.png"); }
        @Override public ResourceLocation getAnimationResource(PillFurnaceBlockEntity furnace) { return asset("animations/block/plant/alchemy_cauldron.animation.json"); }
    }
}
