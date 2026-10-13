package com.murimblock.martial;

import com.murimblock.Murimblock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * The martial arts and their techniques (docs/combat/STYLES.md, docs/combat/styles). Defined here for the first art;
 * meant to move to datapacks once several arts exist.
 */
public final class MartialArts {
    /** A technique of an art: what it costs and how long before it can be used again. */
    public record Technique(ResourceLocation id, ResourceLocation art, double qiCost, int cooldownTicks) {
        public String translationKey() {
            return "technique." + id.getNamespace() + "." + id.getPath();
        }
    }

    /** A martial art: its techniques, in the order they are unlocked (the first comes with the manual). */
    public record Art(ResourceLocation id, List<Technique> techniques) {
        public String translationKey() {
            return "art." + id.getNamespace() + "." + id.getPath();
        }

        public Technique first() {
            return techniques.getFirst();
        }
    }

    public static final ResourceLocation AZURE_CLOUD_SWORD = id("azure_cloud_sword");
    public static final Technique CLOUD_STEP_SLASH = new Technique(id("cloud_step_slash"), AZURE_CLOUD_SWORD, 12, 120);
    public static final Technique THOUSAND_DRIZZLE = new Technique(id("thousand_drizzle"), AZURE_CLOUD_SWORD, 18, 180);
    public static final Technique HEAVEN_SPLITTING_CRESCENT = new Technique(id("heaven_splitting_crescent"), AZURE_CLOUD_SWORD, 25, 280);

    private static final Map<ResourceLocation, Art> ARTS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Technique> TECHNIQUES = new LinkedHashMap<>();

    static {
        add(new Art(AZURE_CLOUD_SWORD, List.of(CLOUD_STEP_SLASH, THOUSAND_DRIZZLE, HEAVEN_SPLITTING_CRESCENT)));
    }

    private MartialArts() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Murimblock.MOD_ID, path);
    }

    private static void add(Art art) {
        ARTS.put(art.id(), art);
        for (Technique t : art.techniques()) TECHNIQUES.put(t.id(), t);
    }

    @Nullable public static Art art(ResourceLocation id) {
        return ARTS.get(id);
    }

    @Nullable public static Technique technique(ResourceLocation id) {
        return TECHNIQUES.get(id);
    }

    public static Iterable<Art> arts() {
        return ARTS.values();
    }
}
