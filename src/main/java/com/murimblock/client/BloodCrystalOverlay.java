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
    /** The blood sword's grip in the hand's frame: the forearm runs along -Y; the blade leaves the fist forward. */
    static final net.minecraft.world.phys.Vec3 GRIP=new net.minecraft.world.phys.Vec3(0,.25,-1).normalize();
    /** From this time into the summoning (s) the grown sword turns from the forearm's line into the grip. */
    static final float GRIP_FROM=.55F;
    /** How far into an animation the player is (s), or -1. */
    static float clockOf(yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch<?> patch,
                         yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<?> animation) {
        var clock=patch.getAnimator().getPlayerFor(animation);
        return clock!=null&&!clock.isEmpty()&&clock.getRealAnimation()==animation?clock.getElapsedTime():-1;
    }
    private static final class Layer<E extends AbstractClientPlayer,P extends AbstractClientPlayerPatch<E>>
            extends PatchedLayer<E,P,PlayerModel<E>,RenderLayer<E,PlayerModel<E>>> {
        final Function<P,SkinnedMesh> firstMesh;
        Layer(Function<P,SkinnedMesh> firstMesh){this.firstMesh=firstMesh;}
        @Override protected void renderLayer(P patch,E player,RenderLayer<E,PlayerModel<E>> original,PoseStack stack,
                MultiBufferSource buffers,int light,OpenMatrix4f[] poses,float age,float headYaw,float headPitch,float partial) {
            if(player.isInvisible()||player.isSpectator())return;
            // Scarlet Tear's blood sword, in both views: drawn in the fist, at the right hand's joint.
            var level=Minecraft.getInstance().level;
            for(var entity:level.entitiesForRendering()) {
                if(!(entity instanceof BloodCrystalEntity sword)||!sword.sword()||sword.state()!=BloodCrystalEntity.CHARGING||sword.visualOwner()!=player)continue;
                var joint=patch.getArmature().searchJointByName("Tool_R");stack.pushPose();
                try {
                    var hand=poses[joint.getId()];
                    yesman.epicfight.api.utils.math.MathUtils.mulStack(stack,hand);
                    // The blade carries on the forearm's line: from the elbow joint through the hand, in the hand's frame.
                    var armature=patch.getArmature();var inverse=new OpenMatrix4f(hand).invert();
                    var zero=net.minecraft.world.phys.Vec3.ZERO;
                    var elbow=OpenMatrix4f.transform(poses[armature.searchJointByName("Hand_R").getId()],zero);
                    var forearm=OpenMatrix4f.transform(inverse,elbow).scale(-1).normalize();
                    // Held as a sword: out of the fist, forward of it (the hand's -Z), a little up.
                    var blade=GRIP;
                    // Summoned: the sword grows along the raised forearm, toward the sky, then turns into the grip.
                    float summon=clockOf(patch,com.murimblock.integration.epicfight.MurimAnimations.SANGUINE_SWORD_SUMMON);
                    if(summon>=0&&summon<GRIP_FROM+.12F) {
                        double f=Math.clamp((summon-GRIP_FROM)/.12,0,1);
                        blade=forearm.scale(1-f).add(GRIP.scale(f)).normalize();
                    }
                    // Thrown: carried like a javelin, its point toward the front of the body, a little up, whatever
                    // the arm does, until it leaves the hand.
                    float t=clockOf(patch,com.murimblock.integration.epicfight.MurimAnimations.SANGUINE_SWORD_THROW);
                    if(t>=0) {
                        var right=OpenMatrix4f.transform(poses[armature.searchJointByName("Arm_R").getId()],zero)
                                .subtract(OpenMatrix4f.transform(poses[armature.searchJointByName("Arm_L").getId()],zero));
                        // Armature space: Y up, the right arm root at +X; the body faces up × right.
                        var up=new net.minecraft.world.phys.Vec3(0,1,0);
                        var forward=up.cross(right).normalize().scale(Math.cos(Math.toRadians(10))).add(up.scale(Math.sin(Math.toRadians(10))));
                        var origin=OpenMatrix4f.transform(hand,zero);
                        var javelin=OpenMatrix4f.transform(inverse,origin.add(forward)).subtract(OpenMatrix4f.transform(inverse,origin)).normalize();
                        double f=Math.min(1,t/.06);
                        blade=blade.scale(1-f).add(javelin.scale(f)).normalize();
                    }
                    BloodCrystalRenderer.swordAlong((float)blade.x,(float)blade.y,(float)blade.z,firstMesh!=null);
                    if(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(sword) instanceof BloodCrystalRenderer renderer)
                        renderer.renderInHand(sword,partial,stack,buffers,light);
                }finally {stack.popPose();}
                break;
            }
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
