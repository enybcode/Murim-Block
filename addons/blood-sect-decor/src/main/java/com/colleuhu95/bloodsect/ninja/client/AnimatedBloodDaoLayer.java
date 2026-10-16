package com.colleuhu95.bloodsect.ninja.client;

import com.colleuhu95.bloodsect.BloodSectDecor;
import com.colleuhu95.bloodsect.ninja.BloodNinja;
import com.colleuhu95.bloodsect.ninja.DaoAttackMotion;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;
import org.joml.Quaternionf;

/** Draws the original dao with articulated tassels and a ribbon sampled from the actual hand path. */
public final class AnimatedBloodDaoLayer extends ItemInHandLayer<BloodNinja, ReferenceNinjaModel> {
    private static final ResourceLocation TEXTURE = id("textures/item/blood_dao_3d.png");
    private static final ResourceLocation FX = id("textures/item/blood_dao_fx.png");
    private final List<Box> weapon, veins;

    public AnimatedBloodDaoLayer(RenderLayerParent<BloodNinja, ReferenceNinjaModel> parent, EntityRendererProvider.Context context) {
        super(parent, context.getItemInHandRenderer());
        try (var reader = new InputStreamReader(context.getResourceManager()
                .getResourceOrThrow(id("models/entity/blood_dao_animated.json")).open(), StandardCharsets.UTF_8)) {
            var mesh = JsonParser.parseReader(reader).getAsJsonObject();
            this.weapon = boxes(mesh.getAsJsonArray("weapon"));
            this.veins = boxes(mesh.getAsJsonArray("pulse"));
        } catch (IOException e) { throw new IllegalStateException("Cannot load animated Blood Dao", e); }
    }

    @Override
    public void render(PoseStack stack, MultiBufferSource buffers, int light, BloodNinja ninja,
                       float walk, float amount, float partial, float age, float yaw, float pitch) {
        if (!ninja.getMainHandItem().is(BloodSectDecor.BLOOD_DAO.get())) {
            super.render(stack,buffers,light,ninja,walk,amount,partial,age,yaw,pitch);
            return;
        }
        float time = ninja.getDaoAttackAge(partial);
        int variant = ninja.getDaoAttackVariant();
        float tassel = DaoAttackMotion.sample(time,variant).tassel();
        stack.pushPose();
        getParentModel().translateToHand(HumanoidArm.RIGHT,stack);
        applyItemTransform(stack);
        applyWrist(stack,time,variant);
        var solid = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (var box : weapon) {
            if (box.tassel) {
                stack.pushPose();
                stack.translate(0,-4.9F/16,0);
                stack.mulPose(Axis.ZP.rotationDegrees(tassel));
                stack.mulPose(Axis.XP.rotationDegrees(tassel*.28F));
                stack.translate(0,4.9F/16,0);
                box.render(stack,solid,light,255);
                stack.popPose();
            } else box.render(stack,solid,light,255);
        }
        float pulse = DaoAttackMotion.pulse(time,variant);
        if (pulse > 0 && !BloodStepEffects.isDrawingGhosts()) {
            var glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(FX));
            for (var vein : veins) vein.render(stack,glow,LightTexture.FULL_BRIGHT,Math.round(pulse*210));
        }
        stack.popPose();
        if (time < DaoAttackMotion.duration(variant) && !BloodStepEffects.isDrawingGhosts()) renderTrails(stack,buffers,time,variant);
        super.renderArmWithItem(ninja,ninja.getOffhandItem(),ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
            HumanoidArm.LEFT,stack,buffers,light);
    }

    /** Same matrices as the approved vanilla item, after converting its source to grip-relative coordinates. */
    static void applyItemTransform(PoseStack stack) {
        stack.mulPose(Axis.XP.rotationDegrees(-90));
        stack.mulPose(Axis.YP.rotationDegrees(180));
        stack.translate(1F/16,2F/16,-10F/16);
        stack.translate(0,4F/16,.5F/16);
        stack.mulPose(Axis.YP.rotationDegrees(-90));
        stack.mulPose(Axis.ZP.rotationDegrees(55));
        stack.scale(.85F,.85F,.85F);
        stack.mulPose(Axis.ZP.rotationDegrees(-45));
        stack.translate(0,-6.6F/16,0);
    }

    private Vector3f bladePoint(float time, int variant, float x, float y) {
        var local = new PoseStack();
        getParentModel().translateToDaoHandAt(local,time,variant);
        applyItemTransform(local);
        applyWrist(local,time,variant);
        return local.last().pose().transformPosition(x/16,y/16,0,new Vector3f());
    }

    static void applyWrist(PoseStack stack,float time,int variant) {
        var p=DaoAttackMotion.sample(time,variant);
        float d=(float)(Math.PI/180);
        stack.mulPose(new Quaternionf().rotationZYX(p.wristZ()*d,p.wristY()*d,p.wristX()*d));
    }

    private void renderTrails(PoseStack stack, MultiBufferSource buffers, float time, int variant) {
        var buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(FX));
        for (int i=0;i<DaoAttackMotion.trailCount(variant);i++) {
            var trail = DaoAttackMotion.trail(variant,i);
            if (time <= trail.start() || time >= trail.end()+trail.life()) continue;
            float end = Math.min(time,trail.end());
            float start = Math.max(trail.start(),time-trail.life());
            // Sub-tick sampling keeps the ribbon continuous at high FPS without mutable per-entity caches.
            for (float a=start;a<end;a+=.3F) {
                float b=Math.min(a+.3F,end);
                float fade=Math.max(0,1-(time-(a+b)*.5F)/trail.life());
                int alpha=Math.round(205*fade*fade);
                var p0=bladePoint(a,variant,-.65F,trail.heel());
                var p1=bladePoint(a,variant,-1.2F,23.6F);
                var p2=bladePoint(b,variant,-1.2F,23.6F);
                var p3=bladePoint(b,variant,-.65F,trail.heel());
                var normal=new Vector3f(p1).sub(p0).cross(new Vector3f(p3).sub(p0));
                if (normal.lengthSquared()<1e-10F) continue;
                normal.normalize();
                float v0=(a-trail.start())/(trail.end()-trail.start());
                float v1=(b-trail.start())/(trail.end()-trail.start());
                vertex(buffer,stack.last(),p0,0,v0,normal,LightTexture.FULL_BRIGHT,alpha);
                vertex(buffer,stack.last(),p1,1,v0,normal,LightTexture.FULL_BRIGHT,alpha);
                vertex(buffer,stack.last(),p2,1,v1,normal,LightTexture.FULL_BRIGHT,alpha);
                vertex(buffer,stack.last(),p3,0,v1,normal,LightTexture.FULL_BRIGHT,alpha);
            }
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vector3f p, float u, float v,
                               Vector3f normal, int light, int alpha) {
        buffer.addVertex(pose,p).setColor(255,255,255,alpha).setUv(u,v)
            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,normal.x,normal.y,normal.z);
    }

    private record Face(Vector3f[] vertices, float[] uv, Vector3f normal) {}
    private record Box(boolean tassel, List<Face> faces) {
        void render(PoseStack stack, VertexConsumer buffer, int light, int alpha) {
            for (var f:faces) {
                vertex(buffer,stack.last(),f.vertices[0],f.uv[0],f.uv[1],f.normal,light,alpha);
                vertex(buffer,stack.last(),f.vertices[1],f.uv[0],f.uv[3],f.normal,light,alpha);
                vertex(buffer,stack.last(),f.vertices[2],f.uv[2],f.uv[3],f.normal,light,alpha);
                vertex(buffer,stack.last(),f.vertices[3],f.uv[2],f.uv[1],f.normal,light,alpha);
            }
        }
    }
    private static List<Box> boxes(JsonArray elements) {
        var result=new ArrayList<Box>();
        for (var e:elements) {
            var box=e.getAsJsonObject();
            float[] a=array(box.getAsJsonArray("from"),16), b=array(box.getAsJsonArray("to"),16);
            float x=a[0],y=a[1],z=a[2],X=b[0],Y=b[1],Z=b[2];
            var faces=new ArrayList<Face>();
            JsonObject faceData=box.getAsJsonObject("faces");
            for (var name:faceData.keySet()) {
                Vector3f[] points=switch(name) {
                    case "north" -> points(X,Y,z, X,y,z, x,y,z, x,Y,z);
                    case "south" -> points(x,Y,Z, x,y,Z, X,y,Z, X,Y,Z);
                    case "east" -> points(X,Y,Z, X,y,Z, X,y,z, X,Y,z);
                    case "west" -> points(x,Y,z, x,y,z, x,y,Z, x,Y,Z);
                    case "up" -> points(x,Y,z, x,Y,Z, X,Y,Z, X,Y,z);
                    case "down" -> points(x,y,Z, x,y,z, X,y,z, X,y,Z);
                    default -> throw new IllegalArgumentException("Unknown dao face "+name);
                };
                var normal=new Vector3f(points[1]).sub(points[0]).cross(new Vector3f(points[2]).sub(points[0])).normalize();
                faces.add(new Face(points,array(faceData.getAsJsonObject(name).getAsJsonArray("uv"),32),normal));
            }
            result.add(new Box(box.get("name").getAsString().startsWith("tassel_"),List.copyOf(faces)));
        }
        return List.copyOf(result);
    }
    private static Vector3f[] points(float... c) {
        return new Vector3f[]{new Vector3f(c[0],c[1],c[2]),new Vector3f(c[3],c[4],c[5]),
            new Vector3f(c[6],c[7],c[8]),new Vector3f(c[9],c[10],c[11])};
    }
    private static float[] array(JsonArray a,float divisor) {
        var result=new float[a.size()];
        for(int i=0;i<result.length;i++) result[i]=a.get(i).getAsFloat()/divisor;
        return result;
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(BloodSectDecor.ID,path); }
}
