package com.murimblock.mixin;

import com.murimblock.combat.SealFighting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The slime as the Sceau de jade vivant sounds like stone (a dry knock as it lands, jade ringing when struck) and sheds
 * specks of jade instead of slime where the client draws it so. The magma cube keeps its own.
 */
@Mixin(Slime.class)
public abstract class SlimeMixin {
    private boolean murimblock$seal() {
        return ((Slime) (Object) this).getType() == EntityType.SLIME;
    }

    @Inject(method = "getParticleType", at = @At("HEAD"), cancellable = true)
    private void murimblock$jade(CallbackInfoReturnable<ParticleOptions> info) {
        if (murimblock$seal() && SealFighting.jade.test((Slime) (Object) this)) {
            info.setReturnValue(SealFighting.JADE);
        }
    }

    @Inject(method = "getSquishSound", at = @At("HEAD"), cancellable = true)
    private void murimblock$land(CallbackInfoReturnable<SoundEvent> info) {
        if (murimblock$seal()) {
            info.setReturnValue(SoundEvents.DEEPSLATE_PLACE);
        }
    }

    @Inject(method = "getJumpSound", at = @At("HEAD"), cancellable = true)
    private void murimblock$jump(CallbackInfoReturnable<SoundEvent> info) {
        if (murimblock$seal()) {
            info.setReturnValue(SoundEvents.STONE_STEP);
        }
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void murimblock$hurt(DamageSource source, CallbackInfoReturnable<SoundEvent> info) {
        if (murimblock$seal()) {
            info.setReturnValue(SoundEvents.DEEPSLATE_TILES_HIT);
        }
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void murimblock$death(CallbackInfoReturnable<SoundEvent> info) {
        if (murimblock$seal()) {
            info.setReturnValue(SoundEvents.DEEPSLATE_TILES_BREAK);
        }
    }
}
