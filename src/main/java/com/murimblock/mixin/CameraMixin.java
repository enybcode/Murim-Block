package com.murimblock.mixin;

import com.murimblock.client.dev.GifRecorder;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dev only: the engine test's recorded scenes may film from a camera of their own (a side view of a fight). */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("RETURN"))
    private void murimblock$sceneCamera(BlockGetter level, Entity entity, boolean detached, boolean mirrored,
                                        float partialTick, CallbackInfo info) {
        GifRecorder.View view = GifRecorder.view(partialTick);
        if (view == null) {
            return;
        }
        Vec3 look = view.target().subtract(view.eye());
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)));
        setRotation(yaw, pitch);
        setPosition(view.eye());
    }
}
