package com.murimblock.mixin;

import com.murimblock.client.combat.LockOn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the lock-on how far the mouse itself turns the view, apart from turns made by code (strikes, the lock). */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double accumulatedDX;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void murimblock$lockOnFlick(double movementTime, CallbackInfo info) {
        if (accumulatedDX == 0 || !minecraft.isWindowActive() || minecraft.screen != null) {
            return;
        }
        // Vanilla's turn: the sensitivity cubed, then 0.15 degree a step.
        double speed = minecraft.options.sensitivity().get() * 0.6 + 0.2;
        LockOn.mouse((float) (accumulatedDX * speed * speed * speed * 8 * 0.15));
    }
}
