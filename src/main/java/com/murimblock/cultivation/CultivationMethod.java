package com.murimblock.cultivation;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The cultivation methods (心法, docs/cultivation/refonte-cultivation.md, 18/10/2026): not martial arts, ways of
 * cultivating. One is practised at a time; each makes meditation condense more in its element's places
 * ({@link #factor}) and gives a small effect, stronger at each of its three levels ({@link #level}: Qi Condensation,
 * Foundation Establishment, Nascent Soul). Ordinary Breath is known from the start.
 */
public enum CultivationMethod {
    /** Known from the start: ×1 everywhere, no effect. */
    ORDINARY_BREATH("ordinary_breath", 0xB8B8B0),
    /** Wind: ×1.4 high up (from y 100) under the open sky; more Qi back out of combat. */
    AZURE_SKY_BREATHING("azure_sky_breathing", 0x4CB8FF),
    /** Ice: ×1.5 in the cold (snowy places, Tianshan, Kunlun); a melee blow taken slows the attacker. */
    FROST_LOTUS_HEART("frost_lotus_heart", 0x9FE8FF),
    /** Fire: ×1.5 by day under the sun; from Foundation, some blows set alight. */
    BLAZING_SUN_SCRIPTURE("blazing_sun_scripture", 0xFF8A1E);

    /** From this height the wind method breathes better. */
    public static final int HIGH_UP = 100;

    private final String id;
    /** Its colour in the Cultivation tab (0xRRGGBB). */
    public final int colour;

    CultivationMethod(String id, int colour) {
        this.id = id;
        this.colour = colour;
    }

    public String id() {
        return id;
    }

    public String key() {
        return "cultivation.murimblock.method." + id;
    }

    public static Optional<CultivationMethod> byId(String id) {
        return Arrays.stream(values()).filter(m -> m.id.equals(id)).findFirst();
    }

    /** Its level for a realm: 0 before Qi Condensation, then 1, 2 at Foundation Establishment, 3 at Nascent Soul. */
    public static int level(CultivationRealm realm) {
        if (realm.ordinal() >= CultivationRealm.NASCENT_SOUL.ordinal()) return 3;
        if (realm.ordinal() >= CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal()) return 2;
        return realm.ordinal() >= CultivationRealm.QI_CONDENSATION.ordinal() ? 1 : 0;
    }

    /** How much more meditation condenses where this player stands (its element's places). */
    public double factor(Player player) {
        Level level = player.level();
        BlockPos at = player.blockPosition();
        return switch (this) {
            case ORDINARY_BREATH -> 1;
            case AZURE_SKY_BREATHING -> at.getY() >= HIGH_UP && level.canSeeSky(at.above()) ? 1.4 : 1;
            case FROST_LOTUS_HEART -> cold(level, at) ? 1.5 : 1;
            case BLAZING_SUN_SCRIPTURE -> level.isDay() && level.canSeeSky(at.above()) ? 1.5 : 1;
        };
    }

    /** Snowy (the biome's cold at this height), or the north's mountains. */
    static boolean cold(Level level, BlockPos at) {
        var biome = level.getBiome(at);
        if (biome.value().coldEnoughToSnow(at)) return true;
        return biome.unwrapKey().map(k -> k.location().getPath()).filter(p -> p.contains("tianshan") || p.contains("kunlun")).isPresent();
    }
}
