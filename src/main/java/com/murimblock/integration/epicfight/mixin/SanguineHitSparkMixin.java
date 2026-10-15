package com.murimblock.integration.epicfight.mixin;

import com.murimblock.martial.SanguineArt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** The blood sword cuts with Epic Fight's own sword animations, without their hit sparks: the Sanguine Art shows none. */
@Mixin(value = AttackAnimation.class, remap = false)
public abstract class SanguineHitSparkMixin {
    @Inject(method = "spawnHitParticle", at = @At("HEAD"), cancellable = true)
    private void murimblock$silenceBloodSword(ServerLevel level, LivingEntityPatch<?> attacker, Entity hit,
            AttackAnimation.Phase phase, CallbackInfo callback) {
        if (SanguineArt.silencesHit(attacker.getOriginal(), ((AttackAnimation) (Object) this).getAccessor())) callback.cancel();
    }
}
