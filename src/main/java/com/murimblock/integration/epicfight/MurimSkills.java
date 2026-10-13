package com.murimblock.integration.epicfight;

import com.murimblock.Murimblock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.Skill;

/** Murim's Epic Fight skills (in the murimblock namespace, so Murim's content policy keeps them). */
public final class MurimSkills {
    private static final DeferredRegister<Skill> SKILLS = DeferredRegister.create(EpicFightRegistries.Keys.SKILL, Murimblock.MOD_ID);

    /** The guard every player has (data/murimblock/skill_parameters/murim_guard.json). */
    public static final DeferredHolder<Skill, MurimGuardSkill> MURIM_GUARD = SKILLS.register("murim_guard", key ->
            MurimGuardSkill.createMurimGuardBuilder().build(key));

    private MurimSkills() {
    }

    public static void register(IEventBus modEventBus) {
        SKILLS.register(modEventBus);
    }
}
