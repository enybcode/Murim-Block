package com.murimblock.client.mixin;

import com.murimblock.client.GoldenBellSkins;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The Golden Bell's body: a player's skin recoloured gold, iron or stone while their technique holds (GoldenBellSkins). */
@Mixin(AbstractClientPlayer.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void murimblock$goldenBellSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin own = cir.getReturnValue();
        PlayerSkin shown = GoldenBellSkins.skinFor((AbstractClientPlayer) (Object) this, own);
        if (shown != own) cir.setReturnValue(shown);
    }
}
