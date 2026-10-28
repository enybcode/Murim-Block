package com.murimblock.combat;

import com.murimblock.Murimblock;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * A fighting style's moves (Epic Fight's weapon movesets): a combo of strikes played one after the other while the
 * attack key is pressed in time, a dash strike out of a sprint, a strike in the air, a guard, and a dodge each way.
 * Clips are the biped's ({@code murimblock:<set>/<clip>}). Only the fists for now: weapons and techniques come later,
 * to Enzo's design.
 */
public record Moveset(String id, List<ResourceLocation> combo, ResourceLocation dash, ResourceLocation air,
                      ResourceLocation guard) {
    /** The dodges, by direction: forward, back, left, right. */
    public static final List<ResourceLocation> DODGES = List.of(clip("combat/dodge_forward"),
            clip("combat/dodge_back"), clip("combat/dodge_left"), clip("combat/dodge_right"));

    /** Bare hands: boxing, legs first (jab, cross, lead hook, rear uppercut). */
    public static final Moveset FISTS = new Moveset("fists", List.of(clip("fists/jab"), clip("fists/cross"),
            clip("fists/hook"), clip("fists/uppercut")), clip("fists/step_in"), clip("fists/hammer"),
            clip("fists/guard"));

    public static ResourceLocation clip(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    /** The moveset of what {@code entity} holds in its main hand. */
    public static Moveset of(LivingEntity entity) {
        return of(entity.getMainHandItem());
    }

    public static Moveset of(ItemStack held) {
        return FISTS;
    }
}
