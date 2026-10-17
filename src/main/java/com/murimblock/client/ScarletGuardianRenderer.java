package com.murimblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.ScarletGuardianPatch;
import com.murimblock.martial.BloodCrystalEntity;
import com.murimblock.mob.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.model.Meshes.MeshAccessor;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.client.mesh.HumanoidMesh;
import yesman.epicfight.client.renderer.patched.entity.PHumanoidRenderer;
import yesman.epicfight.client.renderer.patched.layer.PatchedLayer;

@EventBusSubscriber(modid=Murimblock.MOD_ID,value=Dist.CLIENT)
public final class ScarletGuardianRenderer extends HumanoidMobRenderer<ScarletGuardianEntity,PlayerModel<ScarletGuardianEntity>> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,"textures/entity/scarlet_guardian.png");
    private static final ResourceLocation CRYSTALS=ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID,"textures/entity/blood_crystal.png");
    private static final MeshAccessor<HumanoidMesh> MESH=MeshAccessor.create(Murimblock.MOD_ID,"entity/scarlet_guardian",l -> l.loadSkinnedMesh(HumanoidMesh::new));
    private static final MeshAccessor<SkinnedMesh> FORM=MeshAccessor.create(Murimblock.MOD_ID,"overlay/blood_crystal_body",l -> l.loadSkinnedMesh(SkinnedMesh::new));
    private static boolean hooked;
    public ScarletGuardianRenderer(EntityRendererProvider.Context c){super(c,new PlayerModel<>(c.bakeLayer(ModelLayers.PLAYER),false),1);addLayer(new Anchor(this));}
    @Override public ResourceLocation getTextureLocation(ScarletGuardianEntity entity){return TEXTURE;}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(MurimEntities.SCARLET_GUARDIAN.get(),ScarletGuardianRenderer::new);
        if(!hooked){hooked=true;EpicFightClientEventHooks.Registry.ADD_PATCHED_ENTITY.registerEvent(a ->
            a.addPatchedEntityRenderer(MurimEntities.SCARLET_GUARDIAN.get(),t -> new Patched(a.getContext(),t).initLayerLast(a.getContext(),t)));}
    }
    static final class Patched extends PHumanoidRenderer<ScarletGuardianEntity,ScarletGuardianPatch,PlayerModel<ScarletGuardianEntity>,ScarletGuardianRenderer,HumanoidMesh> {
        Patched(EntityRendererProvider.Context c,EntityType<?> t){super(MESH,c,t);addPatchedLayer(Anchor.class,new CrystalLayer());}
        @Override public void mulPoseStack(PoseStack pose,Armature a,ScarletGuardianEntity entity,ScarletGuardianPatch p,float partial) {
            super.mulPoseStack(pose,a,entity,p,partial);pose.scale(ScarletGuardianEntity.SCALE,ScarletGuardianEntity.SCALE,ScarletGuardianEntity.SCALE);
        }
    }
    public static final class Anchor extends RenderLayer<ScarletGuardianEntity,PlayerModel<ScarletGuardianEntity>> {
        Anchor(ScarletGuardianRenderer r){super(r);}
        @Override public void render(PoseStack p,MultiBufferSource b,int light,ScarletGuardianEntity e,float limb,float amount,float partial,float age,float yaw,float pitch) { }
    }
    static final class CrystalLayer extends PatchedLayer<ScarletGuardianEntity,ScarletGuardianPatch,PlayerModel<ScarletGuardianEntity>,Anchor> {
        @Override protected void renderLayer(ScarletGuardianPatch patch,ScarletGuardianEntity entity,Anchor original,PoseStack stack,
                MultiBufferSource buffers,int light,OpenMatrix4f[] poses,float age,float yaw,float pitch,float partial) {
            var mc=Minecraft.getInstance();var armature=patch.getArmature();
            for(var e:com.murimblock.client.ClientEntityIndex.of(com.murimblock.martial.BloodCrystalEntity.class)) {
                if(!(e instanceof BloodCrystalEntity sword)||!sword.sword()||sword.state()!=BloodCrystalEntity.CHARGING||sword.visualOwner()!=entity)continue;
                stack.pushPose();try {
                    var hand=poses[armature.searchJointByName("Tool_R").getId()];
                    yesman.epicfight.api.utils.math.MathUtils.mulStack(stack,hand);
                    var blade=BloodCrystalOverlay.GRIP;
                    float throwing=BloodCrystalOverlay.clockOf(patch,com.murimblock.integration.epicfight.MurimAnimations.SANGUINE_SWORD_THROW);
                    if(throwing>=0)blade=BloodCrystalOverlay.bodyDirection(poses,armature,hand,new OpenMatrix4f(hand).invert(),1,.17);
                    BloodCrystalRenderer.swordAlong((float)blade.x,(float)blade.y,(float)blade.z,false);
                    if(mc.getEntityRenderDispatcher().getRenderer(sword) instanceof BloodCrystalRenderer renderer)renderer.renderInHand(sword,partial,stack,buffers,light);
                }finally{stack.popPose();}break;
            }
            if(entity.crystalForm()) {
                var adjusted=poses.clone();
                float t=BloodCrystalOverlay.clockOf(patch,com.murimblock.integration.epicfight.MurimAnimations.SANGUINE_CRYSTAL_FORM);
                if(t>=0)for(int i=0;i<adjusted.length;i++){
                    var joint=armature.searchJointById(i);if(joint==null)continue;
                    float growth=Math.max(.001F,BloodCrystalOverlay.growth(t,BloodCrystalOverlay.delayOf(joint.getName())));
                    var inverse=joint.getToOrigin();
                    adjusted[i]=OpenMatrix4f.mulMatrices(adjusted[i],inverse,OpenMatrix4f.createScale(growth,growth,growth),new OpenMatrix4f(inverse).invert());
                }
                FORM.get().draw(stack,buffers,RenderType.entityCutoutNoCull(CRYSTALS),light,1,1,1,1,OverlayTexture.NO_OVERLAY,armature,adjusted);
            }
        }
    }
}
