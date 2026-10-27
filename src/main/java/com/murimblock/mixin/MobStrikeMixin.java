package com.murimblock.mixin;

import com.murimblock.combat.CombatServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where vanilla makes a mob strike (its goals and brains all end there): a mob patched for Shenfa plays its attack
 * instead, and the blow comes later, when the attack's window reaches the target. The mobs that strike without
 * calling {@code Mob}'s method (golem, hoglin, zoglin) or start their own swing first (ravager, warden) are caught in
 * their own.
 */
@Mixin({Mob.class, IronGolem.class, Hoglin.class, Zoglin.class, Ravager.class, Warden.class})
public abstract class MobStrikeMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void murimblock$strikeOnShenfa(Entity target, CallbackInfoReturnable<Boolean> info) {
        if (CombatServer.intercept((Mob) (Object) this, target)) {
            info.setReturnValue(false);
        }
    }
}
