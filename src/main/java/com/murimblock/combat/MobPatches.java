package com.murimblock.combat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * How each vanilla mob fights on Shenfa (Epic Fight's mob patches): when vanilla decides the mob strikes, the mob
 * plays one of its attacks instead (picked by weight among those whose distance fits), and the blow lands when the
 * clip's hit window sweeps through the target, with vanilla's own damage and effects. Its stun armor says how hard a
 * blow must be to stagger it.
 *
 * @param biped      whether it is on the biped skeleton (its colliders follow the clip's joints)
 * @param stunArmor  how much impact a blow loses before it staggers (0: every blow staggers)
 */
public record MobPatches(boolean biped, List<Attack> attacks, List<Attack> armed, float stunArmor) {
    /**
     * One attack: its clip, its weight in the draw, and the distance to the target (blocks) it is used at.
     */
    public record Attack(ResourceLocation clip, float weight, float near, float far) {
    }

    private static final Map<EntityType<?>, MobPatches> PATCHES = new HashMap<>();

    private static Attack attack(String clip, float weight, float near, float far) {
        return new Attack(Moveset.clip(clip), weight, near, far);
    }

    static {
        // Humanoids on the biped: bare-handed claws and lunges; with a blade, the blade's cuts.
        List<Attack> claws = List.of(attack("humanoid/claw", 3, 0, 3), attack("humanoid/lunge", 1, 1.5f, 4));
        List<Attack> punches = List.of(attack("humanoid/punch", 3, 0, 3), attack("humanoid/claw", 1, 0, 3));
        List<Attack> cuts = List.of(attack("blade/slash_1", 3, 0, 3.5f), attack("blade/slash_2", 2, 0, 3.5f),
                attack("humanoid/overhead", 2, 0, 3.5f));
        for (EntityType<?> type : List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED,
                EntityType.ZOMBIE_VILLAGER)) {
            PATCHES.put(type, new MobPatches(true, claws, cuts, 0));
        }
        for (EntityType<?> type : List.of(EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON,
                EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN)) {
            PATCHES.put(type, new MobPatches(true, punches, cuts, type == EntityType.WITHER_SKELETON ? 1 : 0));
        }
        PATCHES.put(EntityType.PIGLIN_BRUTE, new MobPatches(true, punches, cuts, 2));
        PATCHES.put(EntityType.VINDICATOR, new MobPatches(true, punches, cuts, 1));
        PATCHES.put(EntityType.ENDERMAN, new MobPatches(true, List.of(attack("humanoid/punch", 2, 0, 4),
                attack("humanoid/overhead", 1, 0, 4)), cuts, 1));
        // Skeletons of their own: one or two attacks keyed on their parts.
        List<Attack> bite = List.of(attack("spider/bite", 3, 0, 3), attack("spider/pounce", 1, 1.5f, 4));
        PATCHES.put(EntityType.SPIDER, new MobPatches(false, bite, bite, 0.5f));
        PATCHES.put(EntityType.CAVE_SPIDER, new MobPatches(false, bite, bite, 0));
        List<Attack> smash = List.of(attack("iron_golem/smash", 3, 0, 4), attack("iron_golem/swipe", 2, 0, 4));
        PATCHES.put(EntityType.IRON_GOLEM, new MobPatches(false, smash, smash, 6));
        List<Attack> ram = List.of(attack("ravager/bite", 3, 0, 4), attack("ravager/ram", 1, 1.5f, 5));
        PATCHES.put(EntityType.RAVAGER, new MobPatches(false, ram, ram, 6));
        List<Attack> toss = List.of(attack("hoglin/toss", 3, 0, 3.5f));
        PATCHES.put(EntityType.HOGLIN, new MobPatches(false, toss, toss, 3));
        PATCHES.put(EntityType.ZOGLIN, new MobPatches(false, toss, toss, 3));
        List<Attack> wolf = List.of(attack("wolf/bite", 3, 0, 3), attack("wolf/leap", 1, 1.5f, 4));
        PATCHES.put(EntityType.WOLF, new MobPatches(false, wolf, wolf, 0));
        List<Attack> bear = List.of(attack("polar_bear/swipe", 3, 0, 3.5f), attack("polar_bear/maul", 1, 0, 3.5f));
        PATCHES.put(EntityType.POLAR_BEAR, new MobPatches(false, bear, bear, 3));
        List<Attack> warden = List.of(attack("warden/smash", 3, 0, 4), attack("warden/swipe", 2, 0, 4));
        PATCHES.put(EntityType.WARDEN, new MobPatches(false, warden, warden, 12));
        List<Attack> vex = List.of(attack("vex/stab", 1, 0, 3));
        PATCHES.put(EntityType.VEX, new MobPatches(false, vex, vex, 0));
        List<Attack> silverfish = List.of(attack("silverfish/bite", 1, 0, 2));
        PATCHES.put(EntityType.SILVERFISH, new MobPatches(false, silverfish, silverfish, 0));
        PATCHES.put(EntityType.ENDERMITE, new MobPatches(false, List.of(attack("endermite/bite", 1, 0, 2)),
                List.of(attack("endermite/bite", 1, 0, 2)), 0));
    }

    /** The patch of {@code type}, or null when the mob fights as vanilla made it. */
    public static MobPatches of(EntityType<?> type) {
        return PATCHES.get(type);
    }

    /** The attacks {@code mob} picks from: the armed ones while it holds a blade. */
    public List<Attack> attacksOf(LivingEntity mob) {
        ItemStack held = mob.getMainHandItem();
        return Moveset.of(held) == Moveset.BLADE ? armed : attacks;
    }

    /** The stun armor of any body: a patched mob's, a player's from its armor (Epic Fight: armor weighs). */
    public static float stunArmor(LivingEntity body) {
        if (body instanceof Player) {
            return body.getArmorValue() / 8f;
        }
        MobPatches patch = of(body.getType());
        return patch != null ? patch.stunArmor() : Math.max(0, body.getBbHeight() * body.getBbWidth() - 1);
    }
}
