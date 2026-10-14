package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.martial.SanguineArt;
import com.murimblock.martial.BloodCrystalEntity;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.model.Meshes.MeshAccessor;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.patched.entity.PPlayerRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

/** Transparent skin overlay only; no equipment replacement, armor item, or player mesh mutation. */
@EventBusSubscriber(modid=Murimblock.MOD_ID,value=Dist.CLIENT)
public final class BloodCrystalOverlay {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,"textures/entity/blood_crystal.png");
    private static final MeshAccessor<SkinnedMesh> BODY=MeshAccessor.create(Murimblock.MOD_ID,"overlay/blood_crystal_body",loader -> loader.loadSkinnedMesh(SkinnedMesh::new));
    private BloodCrystalOverlay() { }
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event) {
        for(PlayerSkin.Model skin:event.getSkins()) {
            PlayerRenderer renderer=event.getSkin(skin);if(renderer!=null)renderer.addLayer(new Anchor(renderer));
        }
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        EpicFightClientEventHooks.Registry.MODIFY_PATCHED_ENTITY.registerEvent(e -> {
            if(e.get(EntityType.PLAYER) instanceof PPlayerRenderer third)
                third.addPatchedLayer(Anchor.class,new Layer<AbstractClientPlayer,AbstractClientPlayerPatch<AbstractClientPlayer>>(null));
            var first=RenderEngine.getInstance().getFirstPersonRenderer();
            if(first!=null)first.addPatchedLayer(Anchor.class,new Layer<LocalPlayer,LocalPlayerPatch>(patch -> first.getMeshProvider(patch).get()));
        },"murimblock:blood_crystal_overlay",0);
    }
    /** FirstPersonRenderer iterates vanilla layer anchors; a custom layer alone is ignored there. */
    public static final class Anchor extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        Anchor(PlayerRenderer renderer){super(renderer);}
        @Override public void render(PoseStack stack,MultiBufferSource buffers,int light,AbstractClientPlayer player,
                float limb,float amount,float partial,float age,float headYaw,float headPitch) { }
    }
    private static final class Layer<E extends AbstractClientPlayer,P extends AbstractClientPlayerPatch<E>>
            extends PatchedLayer<E,P,PlayerModel<E>,RenderLayer<E,PlayerModel<E>>> {
        final Function<P,SkinnedMesh> firstMesh;
        Layer(Function<P,SkinnedMesh> firstMesh){this.firstMesh=firstMesh;}
        @Override protected void renderLayer(P patch,E player,RenderLayer<E,PlayerModel<E>> original,PoseStack stack,
                MultiBufferSource buffers,int light,OpenMatrix4f[] poses,float age,float headYaw,float headPitch,float partial) {
            if(player.isInvisible()||player.isSpectator())return;
            if(firstMesh!=null) {
                // First-person arms use their own pose. Bind the charge to that wrist rather than its world projection.
                var mc=Minecraft.getInstance();
                for(var entity:mc.level.entitiesForRendering()) {
                    if(!(entity instanceof BloodCrystalEntity crystal)||crystal.state()!=BloodCrystalEntity.CHARGING||crystal.visualOwner()!=player)continue;
                    var joint=patch.getArmature().searchJointByName("Tool_R");stack.pushPose();
                    try {
                        yesman.epicfight.api.utils.math.MathUtils.mulStack(stack,poses[joint.getId()]);
                        stack.translate(0,.08,-.08);
                        if(mc.getEntityRenderDispatcher().getRenderer(crystal) instanceof BloodCrystalRenderer renderer)
                            renderer.renderInHand(crystal,partial,stack,buffers,light);
                    }finally {stack.popPose();}
                    break;
                }
            }
            // The armour is for others and the third-person view: seen from the eyes, its shards filled the screen.
            if(!SanguineArt.state(player).active()||firstMesh!=null)return;
            SkinnedMesh mesh=BODY.get();
            OpenMatrix4f[] adjusted=poses;
            if(player.getSkin().model()==PlayerSkin.Model.SLIM) {
                adjusted=poses.clone();
                for(String name:List.of("Arm_R","Hand_R","Arm_L","Hand_L")) {
                    var joint=patch.getArmature().searchJointByName(name);float inner=name.endsWith("_R")?.25F:-.25F;
                    var narrow=OpenMatrix4f.createTranslation(inner,0,0).scale(.75F,1,1).translate(-inner,0,0);
                    var inverseBind=joint.getToOrigin();var bind=new OpenMatrix4f(inverseBind).invert();
                    adjusted[joint.getId()]=OpenMatrix4f.mulMatrices(poses[joint.getId()],inverseBind,narrow,bind);
                }
            }
            mesh.draw(stack,buffers,RenderType.entityCutoutNoCull(TEXTURE),light,1,1,1,1,OverlayTexture.NO_OVERLAY,patch.getArmature(),adjusted);
        }
    }
}
