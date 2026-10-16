package com.colleuhu95.bloodsect.ninja.client;

import com.colleuhu95.bloodsect.BloodSectDecor;
import com.colleuhu95.bloodsect.ninja.BloodNinja;
import com.colleuhu95.bloodsect.ninja.DaoAttackMotion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Vector3f;

/** Dark afterimages inspired by Murimblock's CloudStepEffects, scoped to Blood Ninja guards. */
@EventBusSubscriber(modid=BloodSectDecor.ID,value=Dist.CLIENT)
public final class BloodStepEffects {
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath(BloodSectDecor.ID,"textures/entity/blood_step_silhouette.png");
    private static final ResourceLocation BLOOD=ResourceLocation.fromNamespaceAndPath(BloodSectDecor.ID,"textures/item/blood_dao_fx.png");
    private static final DustParticleOptions RED=new DustParticleOptions(new Vector3f(.34F,.012F,.035F),.75F);
    private record Ghost(Vec3 position,long tick) {}
    private static final class Track {
        Vec3 last;
        final ArrayDeque<Ghost> ghosts=new ArrayDeque<>();
    }
    private static final Map<UUID,Track> TRACKS=new HashMap<>();
    private static ClientLevel currentLevel;
    private static boolean drawingGhosts;
    public static boolean isDrawingGhosts() {return drawingGhosts;}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level!=currentLevel) {TRACKS.clear();currentLevel=level;}
        if(level==null || mc.isPaused()) return;
        long now=level.getGameTime();
        var seen=new HashSet<UUID>();
        for(var entity:level.entitiesForRendering()) {
            if(!(entity instanceof BloodNinja ninja) || !ninja.isAlive()) continue;
            float age=ninja.getDaoAttackAge(0);
            Track track=TRACKS.get(ninja.getUUID());
            boolean dashing=ninja.getDaoAttackVariant()==DaoAttackMotion.CLOUD_STEP && age>=DaoAttackMotion.WINDUP-1 && age<=DaoAttackMotion.DASH_END+5;
            if(!dashing && track==null) {drainParticles(level,ninja);continue;}
            if(track==null) {track=new Track();TRACKS.put(ninja.getUUID(),track);}
            seen.add(ninja.getUUID());
            Vec3 here=ninja.position();
            if(dashing && track.last!=null && here.distanceToSqr(track.last)>.025 && here.distanceToSqr(track.last)<9) {
                track.ghosts.addLast(new Ghost(track.last,now));
                while(track.ghosts.size()>7) track.ghosts.removeFirst();
                for(int i=0;i<3;i++) {
                    double x=here.x+(level.random.nextDouble()-.5)*.7,z=here.z+(level.random.nextDouble()-.5)*.7;
                    level.addParticle(ParticleTypes.SMOKE,x,here.y+.3,z,0,.015,0);
                    level.addParticle(RED,x,here.y+.4+level.random.nextDouble(),z,0,0,0);
                }
            }
            track.last=here;
            while(!track.ghosts.isEmpty() && now-track.ghosts.peekFirst().tick>=8) track.ghosts.removeFirst();
            if(!dashing && track.ghosts.isEmpty()) TRACKS.remove(ninja.getUUID());
            drainParticles(level,ninja);
        }
        TRACKS.keySet().removeIf(id->!seen.contains(id));
    }
    private static void drainParticles(ClientLevel level,BloodNinja ninja) {
        float age=ninja.getDrainAge(0);
        if(age<0 || age>=10) return;
        double radius=.8*(1-age/10);
        for(int i=0;i<3;i++) {
            double angle=age*.7+i*Math.PI*2/3;
            level.addParticle(RED,ninja.getX()+Math.cos(angle)*radius,ninja.getY()+.7+age*.045,
                ninja.getZ()+Math.sin(angle)*radius,0,0,0);
        }
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES || TRACKS.isEmpty()) return;
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level==null || currentLevel!=level) return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now=level.getGameTime()+partial;
        Vec3 camera=event.getCamera().getPosition();
        var stack=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        var shadow=RenderType.entityTranslucent(WHITE,false);
        var streak=RenderType.entityTranslucentEmissive(BLOOD,false);
        drawingGhosts=true;
        try {
            for(var entity:level.entitiesForRendering()) {
                if(!(entity instanceof BloodNinja ninja)) continue;
                var track=TRACKS.get(ninja.getUUID());
                if(track==null) continue;
                var renderer=mc.getEntityRenderDispatcher().getRenderer(ninja);
                Vec3 here=ninja.getPosition(partial);
                for(var ghost:track.ghosts) {
                    float fade=(float)(1-(now-ghost.tick)/8);
                    if(fade<=0 || ghost.position.distanceToSqr(here)<.08) continue;
                    int alpha=Math.round(150*fade);
                    MultiBufferSource tint=type->new ShadowVertex(buffers.getBuffer(shadow),alpha);
                    stack.pushPose();
                    stack.translate(ghost.position.x-camera.x,ghost.position.y-camera.y,ghost.position.z-camera.z);
                    renderer.render(ninja,ninja.getDaoYaw(),partial,stack,tint,LightTexture.FULL_BRIGHT);
                    stack.popPose();
                    // A low dark-red streak anchors the black silhouette to the traversed path.
                    drawStreak(stack,buffers.getBuffer(streak),ghost.position.add(0,.85,0).subtract(camera),
                        here.add(0,.85,0).subtract(camera),Math.round(100*fade));
                }
            }
        } finally {drawingGhosts=false;}
        buffers.endBatch(shadow);buffers.endBatch(streak);
    }
    private static void drawStreak(PoseStack stack,VertexConsumer out,Vec3 a,Vec3 b,int alpha) {
        Vec3 side=b.subtract(a).cross(a.add(b).scale(.5)).normalize().scale(.04);
        ribbonVertex(stack,out,a.add(side),0,0,alpha);
        ribbonVertex(stack,out,a.subtract(side),1,0,alpha);
        ribbonVertex(stack,out,b.subtract(side),1,1,0);
        ribbonVertex(stack,out,b.add(side),0,1,0);
    }
    private static void ribbonVertex(PoseStack stack,VertexConsumer out,Vec3 v,float u,float w,int alpha) {
        out.addVertex(stack.last(),(float)v.x,(float)v.y,(float)v.z).setColor(255,255,255,alpha).setUv(u,w)
            .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(stack.last(),0,1,0);
    }
    @SubscribeEvent public static void nameTag(RenderNameTagEvent event) {
        if(drawingGhosts) event.setCanRender(TriState.FALSE);
    }
    private record ShadowVertex(VertexConsumer out,int alpha) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x,float y,float z) {out.addVertex(x,y,z);return this;}
        @Override public VertexConsumer setColor(int r,int g,int b,int a) {out.setColor(20,4,8,alpha*a/255);return this;}
        @Override public VertexConsumer setUv(float u,float v) {out.setUv(.5F,.5F);return this;}
        @Override public VertexConsumer setUv1(int u,int v) {out.setUv1(u,v);return this;}
        @Override public VertexConsumer setUv2(int u,int v) {out.setUv2(240,240);return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z) {out.setNormal(x,y,z);return this;}
    }
    private BloodStepEffects() {}
}
