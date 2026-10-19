package com.murimblock.alchemy;

import java.util.List;
import net.minecraft.world.item.Rarity;

/**
 * The furnace's elixirs (docs/cultivation/alchemy.md): pills and decoctions that give temporary passives, Minecraft's
 * effects ("minecraft:speed") or the alchemy's own (AlchemyEffects, "murimblock:iron_skin"), each with its level
 * (0 for I) and its time at full purity; their toxicity; whether they come in a flask (decoctions, drunk) or as a pill
 * (eaten). Their icons and names: scripts/plants/alchemy-data.cjs.
 */
public final class AlchemyElixirs {
    private AlchemyElixirs() {
    }

    public record Effect(String id, int amplifier, int seconds) {
    }

    public record Elixir(String id, boolean flask, int toxicity, Rarity rarity, List<Effect> effects) {
    }

    private static Effect e(String id, int amplifier, int seconds) {
        return new Effect(id.contains(":") ? id : "murimblock:" + id, amplifier, seconds);
    }

    private static Elixir pill(String id, int toxicity, Rarity rarity, Effect... effects) {
        return new Elixir(id, false, toxicity, rarity, List.of(effects));
    }

    private static Elixir flask(String id, int toxicity, Rarity rarity, Effect... effects) {
        return new Elixir(id, true, toxicity, rarity, List.of(effects));
    }

    private static final Rarity C = Rarity.COMMON, U = Rarity.UNCOMMON, R = Rarity.RARE, E = Rarity.EPIC;

    public static final List<Elixir> ALL = List.of(
        // The Alchemy Manual's.
        pill("swift_wind_pill", 6, C, e("minecraft:speed", 0, 180)),
        pill("night_eye_pill", 5, C, e("minecraft:night_vision", 0, 300)),
        pill("fish_breath_pill", 6, C, e("minecraft:water_breathing", 0, 240)),
        pill("fire_ward_pill", 8, C, e("minecraft:fire_resistance", 0, 240)),
        pill("feather_fall_pill", 5, C, e("minecraft:slow_falling", 0, 180)),
        pill("iron_skin_pill", 10, U, e("iron_skin", 0, 180)),
        pill("qi_flow_pill", 10, U, e("qi_flow", 0, 180)),
        flask("clear_mind_decoction", 0, U, e("clear_mind", 0, 300)),
        pill("leaping_toad_pill", 6, C, e("minecraft:jump_boost", 1, 180)),
        pill("stoneheart_pill", 12, U, e("minecraft:resistance", 0, 120)),
        // The Treatise on Elixirs'.
        pill("tiger_might_pill", 14, R, e("tiger_might", 0, 180)),
        pill("thorn_body_pill", 14, R, e("thorn_body", 0, 180)),
        pill("blood_vigor_pill", 15, R, e("blood_vigor", 0, 180)),
        pill("keen_senses_pill", 10, R, e("keen_senses", 0, 240)),
        pill("golden_body_pill", 18, R, e("minecraft:absorption", 1, 180), e("minecraft:health_boost", 0, 180)),
        flask("deep_sea_elixir", 14, R, e("minecraft:dolphins_grace", 0, 240), e("minecraft:conduit_power", 0, 240)),
        pill("miners_vigor_pill", 10, U, e("minecraft:haste", 1, 240)),
        pill("great_qi_flow_pill", 18, R, e("qi_flow", 1, 240)),
        pill("fortune_pill", 12, R, e("minecraft:luck", 1, 600), e("minecraft:hero_of_the_village", 0, 600)),
        pill("shadow_step_pill", 16, R, e("minecraft:invisibility", 0, 120), e("minecraft:speed", 0, 120)),
        // The rare scrolls'.
        pill("diamond_body_pill", 28, E, e("iron_skin", 2, 90), e("minecraft:resistance", 0, 90)),
        pill("phoenix_rebirth_pill", 30, E, e("minecraft:regeneration", 2, 20), e("minecraft:fire_resistance", 0, 120), e("minecraft:absorption", 3, 120)),
        pill("heavenly_tiger_pill", 30, E, e("tiger_might", 2, 120), e("minecraft:strength", 0, 120)),
        flask("dragon_breath_elixir", 25, E, e("qi_flow", 2, 300), e("clear_mind", 1, 300))
    );

    public static Elixir byId(String id) {
        for (Elixir x : ALL) if (x.id().equals(id)) return x;
        return null;
    }
}
