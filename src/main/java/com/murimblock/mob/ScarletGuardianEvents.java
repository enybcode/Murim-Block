package com.murimblock.mob;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.MurimGuardSkill;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid=Murimblock.MOD_ID)
public final class ScarletGuardianEvents {
    @SubscribeEvent public static void struck(LivingDamageEvent.Post e) {
        if(e.getSource().getEntity() instanceof ScarletGuardianEntity guardian && e.getSource().getDirectEntity()==guardian
                && e.getNewDamage()>0 && !MurimGuardSkill.guardedThisTick(e.getEntity()) && !MurimGuardSkill.parriedThisTick(e.getEntity())
                && !guardian.isAlliedTo(e.getEntity()))guardian.replenishCrystal();
    }
}
