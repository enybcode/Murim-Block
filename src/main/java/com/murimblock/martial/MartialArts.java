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
    /**
     * A technique of an art: what it costs and how long before it can be used again. A secret technique is a variant
     * found by a timing, never placed on the bar; it stays hidden until discovered.
     */
    public record Technique(ResourceLocation id, ResourceLocation art, double qiCost, int cooldownTicks, boolean secret) {
        public Technique(ResourceLocation id, ResourceLocation art, double qiCost, int cooldownTicks) {
            this(id, art, qiCost, cooldownTicks, false);
        }

        public String translationKey() {
            return "technique." + id.getNamespace() + "." + id.getPath();
        }

        /** Whether it can be placed on the combat bar (a secret is cast through another technique). */
        public boolean castable() {
            return !secret;
        }
    }

    /**
     * A strike of a passive's M1 chain. A launcher lifts the target, a finisher knocks it down: the fourth M1 slot takes
     * one or the other, so every chain ends on one of them.
     */
    public record Strike(ResourceLocation id, Kind kind) {
        public enum Kind { OPENER, LAUNCHER, FINISHER }

        public String translationKey() {
            return "strike." + id.getNamespace() + "." + id.getPath();
        }

        public boolean endsAChain() {
            return kind != Kind.OPENER;
        }
    }

    /**
     * A passive: the M1 chain and the parry of the art, active for whoever has this art as their main style. Its four
     * M1 slots are chosen among its strikes; {@code defaults} is the chain a new student starts with.
     */
    public record Passive(ResourceLocation id, List<Strike> strikes, List<ResourceLocation> defaults) {
        public static final int M1_SLOTS = 4;

        public String translationKey() {
            return "passive." + id.getNamespace() + "." + id.getPath();
        }

        @Nullable public Strike strike(ResourceLocation strike) {
            for (Strike s : strikes) if (s.id().equals(strike)) return s;
            return null;
        }

        /** Whether a strike may go in a slot: one of this passive's, and the last slot ends the chain. */
        public boolean fits(int slot, ResourceLocation strike) {
            Strike s = strike(strike);
            return s != null && slot >= 0 && slot < M1_SLOTS && (slot < M1_SLOTS - 1 || s.endsAChain());
        }
    }

    /** One step of a quest: a counter (a key in {@link MartialData#progress}) to bring to a goal. */
    public record QuestStep(String key, int goal) {
    }

    /** A quest of an art: its steps, done in order, unlock a technique. */
    public record Quest(String id, ResourceLocation unlocks, List<QuestStep> steps) {
        public String translationKey() {
            return "quest.murimblock." + id;
        }
    }

    /** At this style mastery, a technique evolves into a stronger version (same id, a new name and effect). */
    public record Evolution(ResourceLocation technique, int styleMastery, String nameKey) {
    }

    /** A martial art: its techniques, in the order they are unlocked (the first comes with the manual). */
    public record Art(ResourceLocation id, List<Technique> techniques, Passive passive, List<Quest> quests, List<Evolution> evolutions) {
        public String translationKey() {
            return "art." + id.getNamespace() + "." + id.getPath();
        }

        public Technique first() {
            return techniques.getFirst();
        }

        /** The quest that unlocks a technique, or null (the manual's technique, a secret). */
        @Nullable public Quest questFor(ResourceLocation technique) {
            for (Quest q : quests) if (q.unlocks().equals(technique)) return q;
            return null;
        }

        @Nullable public Evolution evolutionOf(ResourceLocation technique) {
            for (Evolution e : evolutions) if (e.technique().equals(technique)) return e;
            return null;
        }
    }

    public static final ResourceLocation AZURE_CLOUD_SWORD = id("azure_cloud_sword");
    public static final Technique CLOUD_STEP_SLASH = new Technique(id("cloud_step_slash"), AZURE_CLOUD_SWORD, 12, 120);
    /** Cloud Step Slash is held to aim (its draw stance) and goes when let go, or by itself after this long (3 s). */
    public static final int CLOUD_STEP_AIM_MAX_TICKS = 60;
    public static final Technique THOUSAND_DRIZZLE = new Technique(id("thousand_drizzle"), AZURE_CLOUD_SWORD, 18, 180);
    public static final Technique HEAVEN_SPLITTING_CRESCENT = new Technique(id("heaven_splitting_crescent"), AZURE_CLOUD_SWORD, 25, 280);
    /** The secret: Cloud Step Slash cast right after a perfect parry (costs what that Cloud Step Slash costs). */
    public static final Technique SWALLOW_RETURNS = new Technique(id("swallow_returns"), AZURE_CLOUD_SWORD, 0, 0, true);

    public static final Strike DRIFTING_CUT = new Strike(id("drifting_cut"), Strike.Kind.OPENER);
    public static final Strike RETURNING_CUT = new Strike(id("returning_cut"), Strike.Kind.OPENER);
    public static final Strike PIERCING_MIST = new Strike(id("piercing_mist"), Strike.Kind.OPENER);
    public static final Strike RISING_CLOUD = new Strike(id("rising_cloud"), Strike.Kind.LAUNCHER);
    public static final Strike FALLING_RAIN = new Strike(id("falling_rain"), Strike.Kind.FINISHER);
    public static final Strike WIND_STEP = new Strike(id("wind_step"), Strike.Kind.OPENER);
    public static final Passive FLOWING_CLOUD_STANCE = new Passive(id("flowing_cloud_stance"),
            List.of(DRIFTING_CUT, RETURNING_CUT, PIERCING_MIST, RISING_CLOUD, FALLING_RAIN, WIND_STEP),
            List.of(DRIFTING_CUT.id(), RETURNING_CUT.id(), PIERCING_MIST.id(), FALLING_RAIN.id()));

    // The quests' counters, kept in MartialData#progress.
    public static final String PATIENT_BLADE_PARRIES = "patient_blade.parries";
    public static final String PATIENT_BLADE_DASH_HITS = "patient_blade.dash_hits";
    public static final String SEA_OF_CLOUDS_BELL = "sea_of_clouds.bell";
    public static final String SEA_OF_CLOUDS_COMBOS = "sea_of_clouds.combos";
    /** Not a quest: the strikes landed since the passive's mastery last went up. */
    public static final String STANCE_HITS = "flowing_cloud_stance.hits";

    public static final Quest THE_PATIENT_BLADE = new Quest("the_patient_blade", THOUSAND_DRIZZLE.id(),
            List.of(new QuestStep(PATIENT_BLADE_PARRIES, 10), new QuestStep(PATIENT_BLADE_DASH_HITS, 5)));
    public static final Quest ABOVE_THE_SEA_OF_CLOUDS = new Quest("above_the_sea_of_clouds", HEAVEN_SPLITTING_CRESCENT.id(),
            List.of(new QuestStep(SEA_OF_CLOUDS_BELL, 1), new QuestStep(SEA_OF_CLOUDS_COMBOS, 3)));

    public static final Evolution THOUSAND_LI_CLOUD_STEP = new Evolution(CLOUD_STEP_SLASH.id(), 30, "evolution.murimblock.thousand_li_cloud_step");
    public static final Evolution STORM_OF_NEEDLES = new Evolution(THOUSAND_DRIZZLE.id(), 60, "evolution.murimblock.storm_of_needles");
    public static final Evolution AZURE_HEAVEN_CRESCENT = new Evolution(HEAVEN_SPLITTING_CRESCENT.id(), 90, "evolution.murimblock.azure_heaven_crescent");

    // ---------------------------------------------------------------- the Long Fist (docs/combat/styles/long-fist.md)

    /** The first art of the fist: bare hands, Epic Fight's own bare-hand M1. */
    public static final ResourceLocation LONG_FIST = id("long_fist");
    public static final Technique TIGER_STEP_PUNCH = new Technique(id("tiger_step_punch"), LONG_FIST, 10, 100);
    public static final Technique RAPID_FISTS = new Technique(id("rapid_fists"), LONG_FIST, 15, 160);
    public static final Technique WHIRLWIND_KICK = new Technique(id("whirlwind_kick"), LONG_FIST, 20, 240);
    /** The secret: Tiger Step Punch already at the target's chest, right after a perfect parry (costs what it costs). */
    public static final Technique INCH_FORCE = new Technique(id("inch_force"), LONG_FIST, 0, 0, true);
    /** Its passive keeps Epic Fight's bare-hand strikes (no strikes to choose); it only adds Qi on the hits. */
    public static final Passive STEADY_BREATH = new Passive(id("steady_breath"), List.of(), List.of());

    public static final String FIRST_STEPS_PUNCHES = "first_steps.punches";
    public static final String FIRST_STEPS_TIGER_HITS = "first_steps.tiger_hits";
    public static final String UNBROKEN_FLURRY_FULL = "unbroken_flurry.full";
    public static final Quest FIRST_STEPS = new Quest("first_steps", RAPID_FISTS.id(),
            List.of(new QuestStep(FIRST_STEPS_PUNCHES, 30), new QuestStep(FIRST_STEPS_TIGER_HITS, 5)));
    public static final Quest UNBROKEN_FLURRY = new Quest("unbroken_flurry", WHIRLWIND_KICK.id(),
            List.of(new QuestStep(UNBROKEN_FLURRY_FULL, 10)));

    public static final Evolution TIGER_LEAP = new Evolution(TIGER_STEP_PUNCH.id(), 30, "evolution.murimblock.tiger_leap");
    public static final Evolution HUNDRED_FISTS = new Evolution(RAPID_FISTS.id(), 60, "evolution.murimblock.hundred_fists");
    public static final Evolution TORNADO_KICK = new Evolution(WHIRLWIND_KICK.id(), 90, "evolution.murimblock.tornado_kick");

    private static final Map<ResourceLocation, Art> ARTS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Technique> TECHNIQUES = new LinkedHashMap<>();

    static {
        add(new Art(AZURE_CLOUD_SWORD, List.of(CLOUD_STEP_SLASH, THOUSAND_DRIZZLE, HEAVEN_SPLITTING_CRESCENT, SWALLOW_RETURNS),
                FLOWING_CLOUD_STANCE, List.of(THE_PATIENT_BLADE, ABOVE_THE_SEA_OF_CLOUDS),
                List.of(THOUSAND_LI_CLOUD_STEP, STORM_OF_NEEDLES, AZURE_HEAVEN_CRESCENT)));
        add(new Art(LONG_FIST, List.of(TIGER_STEP_PUNCH, RAPID_FISTS, WHIRLWIND_KICK, INCH_FORCE),
                STEADY_BREATH, List.of(FIRST_STEPS, UNBROKEN_FLURRY),
                List.of(TIGER_LEAP, HUNDRED_FISTS, TORNADO_KICK)));
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

    /** The art whose passive this is, or null. */
    @Nullable public static Art artOfPassive(ResourceLocation passive) {
        for (Art a : ARTS.values()) if (a.passive().id().equals(passive)) return a;
        return null;
    }
}
