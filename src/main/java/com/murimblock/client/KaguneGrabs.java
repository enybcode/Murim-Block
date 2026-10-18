package com.murimblock.client;

import com.murimblock.Murimblock;
import com.murimblock.martial.DevouringDemon;
import com.murimblock.martial.KaguneState;
import com.murimblock.network.KaguneGrabPayload;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * What a kagune goes for, on every client (martial/DevouringDemon: Grapple, Constrict; KaguneGrabPayload): the foe (or
 * the point of the wall) the strike's chain is aimed at (KaguneStrikes reads it), and Constrict's coils: three turns of
 * the centipede round the foe, tilted each its own way, closing in tight and squeezing on every crush, until it lets go.
 */
@EventBusSubscriber(modid = Murimblock.MOD_ID, value = Dist.CLIENT)
public final class KaguneGrabs {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, "textures/entity/kagune_forms.png");
    private record Grab(int strike, int target, Vec3 point, long since) { }
    private static final Map<Integer, Grab> GRABS = new HashMap<>();

    private KaguneGrabs() { }

    public static void grab(KaguneGrabPayload p) {
        var level = Minecraft.getInstance().level;
        if (level != null) GRABS.put(p.attacker(), new Grab(p.strike(), p.target(), new Vec3(p.x(), p.y(), p.z()), level.getGameTime()));
    }

    /** Where this player's kagune is going now (world): the foe's middle, or the point of the wall; null for none. */
    @Nullable static Vec3 targetOf(Player player, int strike, long strikeSince, float partial) {
        Grab g = GRABS.get(player.getId());
        if (g == null || g.strike() != strike || Math.abs(g.since() - strikeSince) > 3) return null;
        Entity e = g.target() < 0 ? null : player.level().getEntity(g.target());
        if (e != null) return e.getPosition(partial).add(0, e.getBbHeight() * 0.55, 0);
        return g.target() < 0 ? g.point() : null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || GRABS.isEmpty()) return;
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) { GRABS.clear(); return; }
        long now = level.getGameTime();
        GRABS.values().removeIf(g -> now - g.since() > 80);
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        var stack = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        var type = RenderType.entityCutoutNoCull(TEXTURE);
        var out = buffers.getBuffer(type);
        var model = KaguneModel.get();
        int root = model.bone("centipede_right_0"), joints = model.chainLength(root);
        if (root < 0 || joints < 3) return;
        for (Grab g : GRABS.values()) {
            if (g.strike() != KaguneState.STRIKE_CONSTRICT) continue;
            float t = now - g.since() - DevouringDemon.CONSTRICT_HIT_TICK + partial;
            if (t < 0 || t > DevouringDemon.CONSTRICT_HOLD_TICKS) continue;
            Entity e = level.getEntity(g.target());
            if (e == null || !e.isAlive()) continue;
            Vec3 at = e.getPosition(partial);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(at.x, at.y + 1, at.z));
            // Closing in from twice as wide, then squeezed tighter on each crush.
            float tight = e.getBbWidth() * 0.55F + 0.16F;
            float close = ease(Mth.clamp(t / 3F, 0, 1));
            float crush = (t % DevouringDemon.CONSTRICT_CRUSH_EVERY) - DevouringDemon.CONSTRICT_CRUSH_EVERY / 2F;
            float squeeze = crush >= 0 && crush < 3 ? 0.85F + 0.05F * crush : 1;
            float radius = tight * Mth.lerp(close, 2, 1) * squeeze;
            for (int ring = 0; ring < 3; ring++) {
                float height = e.getBbHeight() * (0.22F + 0.26F * ring), tilt = (ring % 2 == 0 ? 1 : -1) * 0.25F;
                double start = ring * 2.1 + t * 0.03;
                // The chain starts on the ring, runs along it, and bends toward its middle: a closed turn.
                Vector3f radial = new Vector3f((float) Math.cos(start), 0, (float) Math.sin(start));
                Vector3f tangent = new Vector3f((float) -Math.sin(start), tilt, (float) Math.cos(start)).normalize();
                float stretch = (float) (2 * Math.PI * radius / (joints * 4.5F / 16));
                float arc = 360F / (joints - 1);
                stack.pushPose();
                stack.translate(at.x + radial.x * radius - camera.x, at.y + height - camera.y, at.z + radial.z * radius - camera.z);
                stack.scale(-1 / 16F, 1 / 16F, 1 / 16F);
                model.drawChain(root, "idle_demon", t / 20F, tangent, new Vector3f(radial).negate(), 0, 0, arc, stretch, stack, out, light);
                stack.popPose();
            }
        }
        buffers.endBatch(type);
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }
}
