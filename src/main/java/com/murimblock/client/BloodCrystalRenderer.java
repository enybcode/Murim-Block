package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.murimblock.Murimblock;
import com.murimblock.martial.BloodCrystalEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Uses the supplied fan unchanged, showing precisely the earned bone at each socket. */
@EventBusSubscriber(modid=Murimblock.MOD_ID,value=Dist.CLIENT)
public final class BloodCrystalRenderer extends GeoEntityRenderer<BloodCrystalEntity> {
    private boolean inHand;
    private static int firstPersonCharges;
    private static ResourceLocation asset(String path) {return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,path);}
    public BloodCrystalRenderer(EntityRendererProvider.Context context) {super(context,new Model());shadowRadius=0;}
    @Override public boolean shouldRender(BloodCrystalEntity entity,Frustum frustum,double x,double y,double z) {
        var mc=Minecraft.getInstance();
        if(BloodCrystalView.hiddenInFirstPerson(entity.dorsal(),entity.visualOwner()==mc.player,mc.getCameraEntity()==mc.player,
                mc.options.getCameraType().isFirstPerson()))return false;
        if(entity.state()==BloodCrystalEntity.CHARGING&&entity.visualOwner()==mc.player&&mc.getCameraEntity()==mc.player
                &&mc.options.getCameraType().isFirstPerson()&&yesman.epicfight.config.ClientConfig.enableAnimatedFirstPersonModel
                &&com.murimblock.integration.epicfight.EpicFightBridge.patch(mc.player).isEpicFightMode())return false;
        return super.shouldRender(entity,frustum,x,y,z);
    }
    public void renderInHand(BloodCrystalEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        boolean before=inHand;inHand=true;
        try {render(entity,0,partial,pose,buffers,light);firstPersonCharges++;}
        finally {inHand=before;}
    }
    public static int firstPersonCharges(){return firstPersonCharges;}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MurimEntities.BLOOD_CRYSTAL.get(),BloodCrystalRenderer::new);
    }
    @Override public RenderType getRenderType(BloodCrystalEntity entity,ResourceLocation texture,MultiBufferSource buffers,float partial) {
        return RenderType.entityCutoutNoCull(texture);
    }
    @Override protected void applyRotations(BloodCrystalEntity entity,PoseStack pose,float age,float yaw,float partial,float nativeScale) {
        if(entity.dorsal()) {
            pose.mulPose(Axis.YP.rotationDegrees(180-entity.getYRot()));
            double[] socket=BloodCrystalEntity.socket(entity.slot());
            // GeckoLib flips raw X; recenter that bone while preserving its supplied orientation and hover.
            pose.translate(socket[0]/16,-socket[1]/16,-socket[2]/16);
        } else {
            if(!inHand&&entity.state()==BloodCrystalEntity.FLYING) {
                pose.mulPose(Axis.YP.rotationDegrees(entity.getYRot()));
                pose.mulPose(Axis.XP.rotationDegrees(90-entity.getXRot()));
            } else if(!inHand) pose.mulPose(Axis.YP.rotationDegrees(180-entity.getYRot()));
            float scale=entity.crystalScale();pose.scale(scale,scale,scale);
        }
    }
    private static final class Model extends GeoModel<BloodCrystalEntity> {
        @Override public ResourceLocation getModelResource(BloodCrystalEntity entity) {
            return asset("geo/blood_crystal_"+(entity.dorsal()?"floating":"projectile")+".geo.json");
        }
        @Override public ResourceLocation getTextureResource(BloodCrystalEntity entity) {return asset("textures/entity/blood_crystal.png");}
        @Override public ResourceLocation getAnimationResource(BloodCrystalEntity entity) {
            return asset("animations/blood_crystal_"+(entity.dorsal()?"floating":"projectile")+".animation.json");
        }
        @Override public void setCustomAnimations(BloodCrystalEntity entity,long id,AnimationState<BloodCrystalEntity> state) {
            // Models are cached/shared: reset every bone on every render, including when the count drops.
            if(entity.dorsal())for(int i=0;i<10;i++) {
                final boolean hidden=i!=entity.slot();getBone("float_crystal_"+(i+1)).ifPresent(b -> {b.setHidden(hidden);b.setChildrenHidden(hidden);});
            }
        }
    }
}
