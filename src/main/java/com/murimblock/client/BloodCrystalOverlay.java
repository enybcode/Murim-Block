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
    /** The summoning is a draw (s): the blade lies along the scabbard at the left hip until DRAW, carries on the arm's
     *  line through the arc and the flick, and turns into the grip from GRIP_FROM. */
    static final float DRAW=.38F, GRIP_FROM=.84F;
    /** How far into an animation the player is (s), or -1. */
    static float clockOf(yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch<?> patch,
                         yesman.epicfight.api.animation.AnimationManager.AnimationAccessor<?> animation) {
        var clock=patch.getAnimator().getPlayerFor(animation);
        return clock!=null&&!clock.isEmpty()&&clock.getRealAnimation()==animation?clock.getElapsedTime():-1;
    }
    /** A direction fixed to the body (forward * along + up * rise, armature space: Y up, the right arm root at +X, the
     *  body facing up × right), in the hand's frame. */
    static net.minecraft.world.phys.Vec3 bodyDirection(OpenMatrix4f[] poses,yesman.epicfight.api.model.Armature armature,
            OpenMatrix4f hand,OpenMatrix4f inverse,double along,double rise) {
        return bodyDirection(poses,armature,hand,inverse,along,rise,0);
    }
    /** As above, plus side * the body's right. */
    static net.minecraft.world.phys.Vec3 bodyDirection(OpenMatrix4f[] poses,yesman.epicfight.api.model.Armature armature,
            OpenMatrix4f hand,OpenMatrix4f inverse,double along,double rise,double side) {
        var zero=net.minecraft.world.phys.Vec3.ZERO;
        var right=OpenMatrix4f.transform(poses[armature.searchJointByName("Arm_R").getId()],zero)
                .subtract(OpenMatrix4f.transform(poses[armature.searchJointByName("Arm_L").getId()],zero));
        var up=new net.minecraft.world.phys.Vec3(0,1,0);
        var direction=up.cross(right).normalize().scale(along).add(up.scale(rise)).add(right.normalize().scale(side));
        var origin=OpenMatrix4f.transform(hand,zero);
        return OpenMatrix4f.transform(inverse,origin.add(direction)).subtract(OpenMatrix4f.transform(inverse,origin)).normalize();
    }
    /** When each player's form was last seen ending (client game time), and who was in it, for the crystals' shedding. */
    private static final java.util.Map<java.util.UUID,Long> SHED_AT=new java.util.HashMap<>();
    private static final java.util.Set<java.util.UUID> FORMED=new java.util.HashSet<>();
    /** The form's end: the body's crystals shrink away over these ticks as they come off and fall. */
    static final float SHED_TICKS=5;
    /** 1 in the form, falling to 0 as its crystals come away at its end; 0 out of it. */
    private static float shedding(AbstractClientPlayer player,float partial) {
        var id=player.getUUID();long now=player.level().getGameTime();
        if(SanguineArt.state(player).active()){FORMED.add(id);SHED_AT.remove(id);return 1;}
        if(FORMED.remove(id))SHED_AT.put(id,now);
        Long at=SHED_AT.get(id);if(at==null)return 0;
        float left=1-(now-at+partial)/SHED_TICKS;
        if(left<=0){SHED_AT.remove(id);return 0;}
        return left;
    }
    /** How long after the chest the crystals reach each part of the body (s), as the blood spreads. */
    static float delayOf(String joint) {
        return switch(joint) {
            case "Chest" -> 0; case "Torso" -> .06F; case "Shoulder_R","Shoulder_L","Head" -> .1F;
            case "Arm_R","Arm_L" -> .14F; case "Hand_R","Hand_L","Tool_R","Tool_L" -> .22F;
            case "Thigh_R","Thigh_L","Root" -> .26F; default -> .34F;
        };
    }
    /**
     * How far grown the crystals of a part of the body are (0 to 1, a little over at the burst), t seconds into the
     * transformation (sanguine_crystal_form, 2.1 s): nothing for 0.15 s, then they creep out with the pain, trembling,
     * to 0.42; at 1.05 s they burst out (1.08 at the peak) and settle by 1.45 s. Smooth, never by steps.
     */
    static float growth(float t,float delay) {
        float c=t-delay;
        float creep=c<=.15F?0:.42F*smooth(Math.min(1,(c-.15F)/.9F));
        if(creep>0&&creep<.42F)creep=Math.max(0,creep+.03F*(float)Math.sin(c*40));
        float b=t-1.05F-delay*.25F;
        if(b<=0)return creep;
        if(b<.18F){float f=b/.18F;f=1-(1-f)*(1-f);return creep+(1.08F-creep)*f;}
        if(b<.4F)return 1.08F-.08F*smooth((b-.18F)/.22F);
        return 1;
    }
    private static float smooth(float f){f=Math.clamp(f,0,1);return f*f*(3-2*f);}
    /** How far the first-person sword is raised into view (0 in the grip, 1 raised), eased frame to frame. */
    private static float firstPersonRaise;
    /** Whether one of the blood sword's cuts (Epic Fight's sword combo) is playing. */
    private static boolean cutting(yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch<?> patch) {
        for(var cut:java.util.List.of(yesman.epicfight.gameasset.Animations.SWORD_AUTO1,yesman.epicfight.gameasset.Animations.SWORD_AUTO2,
                yesman.epicfight.gameasset.Animations.SWORD_AUTO3,yesman.epicfight.gameasset.Animations.SWORD_DASH,
                yesman.epicfight.gameasset.Animations.SWORD_AIR_SLASH))
            if(clockOf(patch,cut)>=0)return true;
        return false;
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
            for(var entity:com.murimblock.client.ClientEntityIndex.of(BloodCrystalEntity.class)) {
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
                    // Summoned, a draw: in the sheath at the left hip the blade points back and a little down (it forms
                    // there, as if pulled out of an unseen scabbard); drawn, it carries on the arm's line through the
                    // arc and the flick; then it turns into the grip.
                    float summon=clockOf(patch,com.murimblock.integration.epicfight.MurimAnimations.SANGUINE_SWORD_SUMMON);
                    if(summon>=0&&summon<GRIP_FROM+.12F) {
                        var sheath=bodyDirection(poses,armature,hand,inverse,-1,-.35);
                        double drawn=Math.clamp((summon-DRAW)/.1,0,1),held=Math.clamp((summon-GRIP_FROM)/.12,0,1);
                        var out=sheath.scale(1-drawn).add(forearm.scale(drawn)).normalize();
                        blade=out.scale(1-held).add(GRIP.scale(held)).normalize();
                    }
                    // Thrown: carried like a javelin, its point toward the front of the body, a little up, whatever
                    // the arm does, until it leaves the hand.
                    float t=clockOf(patch,com.murimblock.integration.epicfight.MurimAnimations.SANGUINE_SWORD_THROW);
                    if(t>=0) {
                        var javelin=bodyDirection(poses,armature,hand,inverse,1,Math.sin(Math.toRadians(10)));
                        double f=Math.min(1,t/.06);
                        blade=blade.scale(1-f).add(javelin.scale(f)).normalize();
                    }
                    // Seen from the eyes, in guard, Epic Fight holds the empty hand low, off the screen: the sword is
                    // raised before the eyes, up and a little inward, so it can be seen. It eases back into the grip
                    // for the cuts, the draw and the throw.
                    if(firstMesh!=null) {
                        boolean busy=summon>=0||t>=0||cutting(patch);
                        firstPersonRaise+=((busy?0:1)-firstPersonRaise)*(busy?.5F:.15F);
                        if(firstPersonRaise>.01F) {
                            var raised=bodyDirection(poses,armature,hand,inverse,.55,.95,-.3);
                            blade=blade.scale(1-firstPersonRaise).add(raised.scale(firstPersonRaise)).normalize();
                        }
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
                for(var entity:com.murimblock.client.ClientEntityIndex.of(BloodCrystalEntity.class)) {
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
            if(firstMesh!=null)return;
            float shedding=shedding(player,partial);
            if(shedding<=0)return;
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
            // The transformation: the crystals push out of the body joint by joint (the chest first, the legs last), creeping
            // with the pain, then bursting; at the form's end they shrink back as they come away and fall.
            float formClock=clockOf(patch,com.murimblock.integration.epicfight.MurimAnimations.SANGUINE_CRYSTAL_FORM);
            if(formClock>=0||shedding<1) {
                if(adjusted==poses)adjusted=poses.clone();
                for(int index=0;index<adjusted.length;index++) {
                    var joint=patch.getArmature().searchJointById(index);if(joint==null)continue;
                    float g=(formClock>=0?growth(formClock,delayOf(joint.getName())):1)*shedding;
                    if(g>=.999F&&g<=1.001F)continue;
                    g=Math.max(.001F,g);
                    var inverseBind=joint.getToOrigin();var bind=new OpenMatrix4f(inverseBind).invert();
                    adjusted[joint.getId()]=OpenMatrix4f.mulMatrices(adjusted[joint.getId()],inverseBind,OpenMatrix4f.createScale(g,g,g),bind);
                }
            }
            mesh.draw(stack,buffers,RenderType.entityCutoutNoCull(TEXTURE),light,1,1,1,1,OverlayTexture.NO_OVERLAY,patch.getArmature(),adjusted);
        }
    }
}
