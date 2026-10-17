package com.murimblock.client.mixin;

import com.murimblock.client.GoldenBellSkins;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Golden Bell's body: a player's skin recoloured gold, iron or stone while their technique holds (GoldenBellSkins).
 * Applied after other mods' skin hooks (priority), and optional: a mod that replaced the method costs the gold, not the game.
 */
@Mixin(value = AbstractClientPlayer.class, priority = 1500)
public abstract class PlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true, require = 0)
    private void murimblock$goldenBellSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin own = cir.getReturnValue();
        PlayerSkin shown = GoldenBellSkins.skinFor((AbstractClientPlayer) (Object) this, own);
        if (shown != own) cir.setReturnValue(shown);
    }
}
