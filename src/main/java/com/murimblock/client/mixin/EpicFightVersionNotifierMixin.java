package com.murimblock.client.mixin;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.gui.VersionNotifier;

/** Keep the dependency/version in the mod list and docs, not over the Murim HUD. */
@Mixin(value = VersionNotifier.class, remap = false)
public abstract class EpicFightVersionNotifierMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void murimblock$hideVersionOverlay(GuiGraphics graphics, boolean inWorld, CallbackInfo callback) {
        callback.cancel();
    }
}
