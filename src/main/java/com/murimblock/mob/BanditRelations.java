package com.murimblock.mob;

import com.murimblock.Murimblock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** People are prey, bandits are allies, animals are spared; the tiger is the explicit exception. */
@EventBusSubscriber(modid = Murimblock.MOD_ID)
public final class BanditRelations {
    public static final TagKey<EntityType<?>> BANDITS = tag("bandits");
    public static final TagKey<EntityType<?>> PEOPLE = tag("bandit_human_targets");
    public static final TagKey<EntityType<?>> FACTION_FIGHTERS = tag("bandit_enemies");
    private BanditRelations() { }
    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, name));
    }
    public static boolean bandit(LivingEntity entity) { return entity.getType().is(BANDITS); }
    public static boolean mayFight(LivingEntity target) {
        return !bandit(target) && !(target instanceof Animal) && !(target instanceof WaterAnimal)
                && !(target instanceof AmbientCreature) && !target.isInvulnerable();
    }
    public static boolean prey(LivingEntity target) {
        return mayFight(target) && (target instanceof Player || target instanceof TigerEntity || target.getType().is(PEOPLE));
    }
    @SubscribeEvent public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob)
                || !mob.getType().is(FACTION_FIGHTERS)) return;
        // Entity instances can leave and rejoin a level; do not accumulate target goals on the same instance.
        if (mob.targetSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof HuntBandits)) return;
        mob.targetSelector.addGoal(2, new HuntBandits(mob));
    }
    private static final class HuntBandits extends NearestAttackableTargetGoal<LivingEntity> {
        HuntBandits(Mob mob) { super(mob, LivingEntity.class, 5, true, false, BanditRelations::bandit); }
    }
}
