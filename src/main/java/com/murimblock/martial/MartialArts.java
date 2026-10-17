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

    // ---------------------------------------------------------------- the Celestial Peak Spear (docs/combat/styles/celestial-peak-spear.md)

    /** The Orthodox Alliance's spear: a spear in hand, Epic Fight's own spear M1; an art of the start of the game. */
    public static final ResourceLocation CELESTIAL_PEAK_SPEAR = id("celestial_peak_spear");
    public static final Technique PIERCING_DRAGON = new Technique(id("piercing_dragon"), CELESTIAL_PEAK_SPEAR, 12, 120);
    public static final Technique PLUM_BLOSSOM_THRUSTS = new Technique(id("plum_blossom_thrusts"), CELESTIAL_PEAK_SPEAR, 18, 180);
    public static final Technique THOUSAND_SOLDIER_SWEEP = new Technique(id("thousand_soldier_sweep"), CELESTIAL_PEAK_SPEAR, 22, 240);
    /** Its passive keeps Epic Fight's spear strikes; a hit at the point (3 blocks or more) strikes harder and pushes. */
    public static final Passive LONG_REACH = new Passive(id("long_reach"), List.of(), List.of());
    public static final String FIRST_LINE_DRAGON_HITS = "first_line.dragon_hits";
    public static final String HOLD_THE_DISTANCE_HITS = "hold_the_distance.reach_hits";
    public static final Quest FIRST_LINE = new Quest("first_line", PLUM_BLOSSOM_THRUSTS.id(), List.of(new QuestStep(FIRST_LINE_DRAGON_HITS, 20)));
    public static final Quest HOLD_THE_DISTANCE = new Quest("hold_the_distance", THOUSAND_SOLDIER_SWEEP.id(), List.of(new QuestStep(HOLD_THE_DISTANCE_HITS, 30)));
    public static final Evolution SOARING_DRAGON = new Evolution(PIERCING_DRAGON.id(), 30, "evolution.murimblock.soaring_dragon");
    public static final Evolution PLUM_RAIN = new Evolution(PLUM_BLOSSOM_THRUSTS.id(), 60, "evolution.murimblock.plum_rain");
    public static final Evolution TEN_THOUSAND_SWEEP = new Evolution(THOUSAND_SOLDIER_SWEEP.id(), 90, "evolution.murimblock.sweep_of_ten_thousand");

    // ---------------------------------------------------------------- the Traveller's Staff Art (docs/combat/styles/traveller-staff.md)

    /** The staff held in both hands (Enzo's design sheet, 16/10/2026): pressure at close quarters and a guard in front. */
    public static final ResourceLocation TRAVELLER_STAFF = id("traveller_staff");
    public static final Technique VERTICAL_STRIKE = new Technique(id("vertical_strike"), TRAVELLER_STAFF, 12, 120);
    public static final Technique POINT_FLURRY = new Technique(id("point_flurry"), TRAVELLER_STAFF, 18, 180);
    /** Held: 10 Qi to start, then 5 a second, 3 s at most; its 12 s cooldown runs from its end. */
    public static final Technique SPINNING_RAMPART = new Technique(id("spinning_rampart"), TRAVELLER_STAFF, 10, 240);
    public static final double RAMPART_QI_PER_SECOND = 5;
    public static final int RAMPART_MAX_TICKS = 60;
    /** Its passive keeps Epic Fight's strikes; the third of a chain strikes twice as hard. */
    public static final Passive THIRD_RESONANCE = new Passive(id("third_resonance"), List.of(), List.of());

    // ---------------------------------------------------------------- the Mountain-Shattering Fist (docs/combat/styles/mountain-shattering-fist.md)

    /**
     * The Bandit Warlord's art (Enzo, 16/10/2026: "a lot of area damage and destruction of the elements"): bare hands,
     * slow and heavy, striking all round and breaking the ground (it comes back: combat.ShatteredGround). Learnt from Qi
     * Condensation.
     */
    public static final ResourceLocation MOUNTAIN_SHATTERING_FIST = id("mountain_shattering_fist");
    public static final com.murimblock.cultivation.CultivationRealm MOUNTAIN_FIST_REALM = com.murimblock.cultivation.CultivationRealm.QI_CONDENSATION;
    public static final Technique MOUNTAIN_CRUSHING_PUNCH = new Technique(id("mountain_crushing_punch"), MOUNTAIN_SHATTERING_FIST, 40, 160);
    public static final Technique EARTH_BREAKER = new Technique(id("earth_breaker"), MOUNTAIN_SHATTERING_FIST, 60, 280);
    public static final Technique BULL_CHARGE = new Technique(id("bull_charge"), MOUNTAIN_SHATTERING_FIST, 50, 240);
    /** The ultimate: up into the sky and down like a meteor, a crater where it lands; five minutes to recover. */
    public static final Technique HEAVEN_FALLING_MOUNTAIN = new Technique(id("heaven_falling_mountain"), MOUNTAIN_SHATTERING_FIST, 200, 6000);
    /** The secret: Mountain-Crushing Punch cast crouching, an uppercut that throws its target to the sky (costs what it costs). */
    public static final Technique SKY_BREAKER = new Technique(id("sky_breaker"), MOUNTAIN_SHATTERING_FIST, 0, 0, true);
    /** Its passive keeps Epic Fight's bare-hand strikes; each that lands shakes the ground round its target. */
    public static final Passive EARTHSHAKER = new Passive(id("earthshaker"), List.of(), List.of());
    public static final String SHATTER_THE_STONE_BLOCKS = "shatter_the_stone.blocks";
    public static final String SHAKE_THE_EARTH_GROUPS = "shake_the_earth.groups";
    public static final String HEAVENS_WEIGHT_BLOCKS = "heavens_weight.blocks";
    public static final Quest SHATTER_THE_STONE = new Quest("shatter_the_stone", EARTH_BREAKER.id(),
            List.of(new QuestStep(SHATTER_THE_STONE_BLOCKS, 200)));
    public static final Quest SHAKE_THE_EARTH = new Quest("shake_the_earth", BULL_CHARGE.id(),
            List.of(new QuestStep(SHAKE_THE_EARTH_GROUPS, 10)));
    public static final Quest HEAVENS_WEIGHT = new Quest("heavens_weight", HEAVEN_FALLING_MOUNTAIN.id(),
            List.of(new QuestStep(HEAVENS_WEIGHT_BLOCKS, 1000)));
    public static final Evolution MOUNTAIN_SPLITTING_PUNCH = new Evolution(MOUNTAIN_CRUSHING_PUNCH.id(), 30, "evolution.murimblock.mountain_splitting_punch");
    public static final Evolution EARTHQUAKE = new Evolution(EARTH_BREAKER.id(), 60, "evolution.murimblock.earthquake");
    public static final Evolution STAMPEDE = new Evolution(BULL_CHARGE.id(), 90, "evolution.murimblock.stampede");

    // ---------------------------------------------------------------- the Seven Stars Sword (docs/combat/styles/seven-stars-sword.md)

    /**
     * A free art of the middle of the game (Enzo, 17/10/2026), the florett only, learnt from Qi Condensation: each blow
     * puts a star on its target, seven burst. Five techniques (an intermediate art's), a secret, an ultimate.
     */
    public static final ResourceLocation SEVEN_STARS_SWORD = id("seven_stars_sword");
    public static final com.murimblock.cultivation.CultivationRealm SEVEN_STARS_REALM = com.murimblock.cultivation.CultivationRealm.QI_CONDENSATION;
    public static final Technique SHOOTING_STAR = new Technique(id("shooting_star"), SEVEN_STARS_SWORD, 30, 140);
    public static final Technique DIPPER_STEPS = new Technique(id("dipper_steps"), SEVEN_STARS_SWORD, 40, 200);
    public static final Technique ORBITING_STARS = new Technique(id("orbiting_stars"), SEVEN_STARS_SWORD, 45, 360);
    public static final Technique CONSTELLATION_LINK = new Technique(id("constellation_link"), SEVEN_STARS_SWORD, 55, 280);
    public static final Technique FALLING_STAR_RAIN = new Technique(id("falling_star_rain"), SEVEN_STARS_SWORD, 70, 400);
    /** The ultimate: seven swords of light round a target held still; three minutes to recover. */
    public static final Technique SEVEN_STARS_ARRAY = new Technique(id("seven_stars_array"), SEVEN_STARS_SWORD, 150, 3600);
    /** The secret: a florett strike right after a perfect parry, on the attacker, completes its constellation. */
    public static final Technique POLARIS_RIPOSTE = new Technique(id("polaris_riposte"), SEVEN_STARS_SWORD, 0, 0, true);
    /** Its passive keeps Dawn Day's florett strikes (a little quicker); each that lands puts a star on its target. */
    public static final Passive STAR_MARKS = new Passive(id("star_marks"), List.of(), List.of());
    /** Constellations burst (the first quest, Seven Points). */
    public static final String SEVEN_POINTS_BURSTS = "seven_points.bursts";
    public static final String WALK_THE_DIPPER_FINISHES = "walk_the_dipper.finishes";
    public static final String UNBROKEN_ORBIT_BLOCKS = "unbroken_orbit.blocks";
    public static final String BIND_THE_SKY_GROUPS = "bind_the_sky.groups";
    public static final String SEVEN_SWORDS_BURSTS = "seven_swords.bursts";
    /** Not a quest: perfect parries in the stance until Polaris Riposte is found. */
    public static final String POLARIS_PARRIES = "polaris.parries";
    public static final Quest SEVEN_POINTS = new Quest("seven_points", DIPPER_STEPS.id(), List.of(new QuestStep(SEVEN_POINTS_BURSTS, 10)));
    public static final Quest WALK_THE_DIPPER = new Quest("walk_the_dipper", ORBITING_STARS.id(), List.of(new QuestStep(WALK_THE_DIPPER_FINISHES, 15)));
    public static final Quest UNBROKEN_ORBIT = new Quest("unbroken_orbit", CONSTELLATION_LINK.id(), List.of(new QuestStep(UNBROKEN_ORBIT_BLOCKS, 30)));
    public static final Quest BIND_THE_SKY = new Quest("bind_the_sky", FALLING_STAR_RAIN.id(), List.of(new QuestStep(BIND_THE_SKY_GROUPS, 10)));
    public static final Quest SEVEN_SWORDS = new Quest("seven_swords", SEVEN_STARS_ARRAY.id(), List.of(new QuestStep(SEVEN_SWORDS_BURSTS, 50)));
    /** At 30: Shooting Star goes 12 blocks and may go again once, free, within 2 s. */
    public static final Evolution TWIN_COMETS = new Evolution(SHOOTING_STAR.id(), 30, "evolution.murimblock.twin_comets");
    /** At 60: Dipper Steps bounds five times. At 90: Falling Star Rain calls nine stars. */
    public static final Evolution FULL_DIPPER = new Evolution(DIPPER_STEPS.id(), 60, "evolution.murimblock.full_dipper");
    public static final Evolution METEOR_SHOWER = new Evolution(FALLING_STAR_RAIN.id(), 90, "evolution.murimblock.meteor_shower");

    // ---------------------------------------------------------------- the Golden Bell (docs/combat/styles/golden-bell.md)

    /**
     * A starter art of bare hands, all defence (Enzo, 17/10/2026): no passive; a golden form one keeps (the body gold), in
     * which the two other techniques change. Its manual is sold by the Golden Bell monk.
     */
    public static final ResourceLocation GOLDEN_BELL = id("golden_bell");
    /** The form: 15 Qi to go in, then 4 a second (GoldenBell); its 12 s cooldown runs from its end. */
    public static final Technique GOLDEN_BELL_FORM = new Technique(id("golden_bell"), GOLDEN_BELL, 15, 240);
    public static final Technique IRON_REBOUND = new Technique(id("iron_rebound"), GOLDEN_BELL, 12, 160);
    /** Its 16 s cooldown runs from its end. */
    public static final Technique ROOTED_MOUNTAIN = new Technique(id("rooted_mountain"), GOLDEN_BELL, 18, 320);
    /** The secret: a perfect parry in the form rings the bell at once and knocks the attacker down. */
    public static final Technique BELL_TOLL = new Technique(id("bell_toll"), GOLDEN_BELL, 0, 0, true);
    /** No passive: Epic Fight's own bare-hand strikes (the palms), nothing more. */
    public static final Passive BELL_PALMS = new Passive(id("bell_palms"), List.of(), List.of());
    public static final String TAKE_THE_BLOW_BLOWS = "take_the_blow.blows";
    public static final String RETURN_IT_BLOWS = "return_it.blows";
    /** Not a quest: blows taken in the form until Bell Toll is found. */
    public static final String BELL_TOLL_BLOWS = "bell_toll.blows";
    public static final Quest TAKE_THE_BLOW = new Quest("take_the_blow", IRON_REBOUND.id(), List.of(new QuestStep(TAKE_THE_BLOW_BLOWS, 50)));
    public static final Quest RETURN_IT = new Quest("return_it", ROOTED_MOUNTAIN.id(), List.of(new QuestStep(RETURN_IT_BLOWS, 10)));
    public static final Evolution STEADFAST_BELL = new Evolution(GOLDEN_BELL_FORM.id(), 30, "evolution.murimblock.steadfast_bell");
    public static final Evolution MIRROR_IRON = new Evolution(IRON_REBOUND.id(), 60, "evolution.murimblock.mirror_iron");
    public static final Evolution ANCIENT_MOUNTAIN = new Evolution(ROOTED_MOUNTAIN.id(), 90, "evolution.murimblock.ancient_mountain");

    public static final ResourceLocation SANGUINE_ART = id("sanguine_art");
    /** Keep the serialized ID of the replaced first ability, preserving existing bars and learned data. */
    /**
     * The Sanguine Art is a high art: its scroll teaches only from Qi Crystallization (Enzo, 15/10/2026), so its techniques
     * cost Qi for a reserve of 3 000 to 5 000, and strike accordingly (TechniquePower.BLOOD_TRAINING).
     */
    public static final com.murimblock.cultivation.CultivationRealm SANGUINE_REALM = com.murimblock.cultivation.CultivationRealm.QI_CRYSTALLIZATION;
    public static final Technique BLOOD_CRYSTAL = new Technique(id("bloodbane_drain"), SANGUINE_ART, 150, 160);
    public static final int BLOOD_CRYSTAL_MIN_CHARGE = BloodCrystalCharge.MIN_TICKS, BLOOD_CRYSTAL_MAX_CHARGE = BloodCrystalCharge.AUTO_RELEASE_TICKS;
    public static final Technique SCARLET_TEAR = new Technique(id("scarlet_tear"), SANGUINE_ART, 225, 200);
    /** The previous third ability now activates the final crystal form. */
    public static final Technique BLOOD_CRYSTAL_FORM = new Technique(id("devourer_of_worlds"), SANGUINE_ART, 300, 80);
    public static final Technique CRYSTAL_BARRAGE = new Technique(id("crystal_barrage"), SANGUINE_ART, 180, 120);
    /** A mountain of blood crystal under the player for 60 s (BloodIceberg); 9 minutes to recover. */
    public static final Technique BLOOD_ICEBERG = new Technique(id("blood_iceberg"), SANGUINE_ART, 750, 10800);
    /** Fixed bare-hand chain from the approved Sanguine combo, with no configurable strike slots. */
    public static final Passive CRIMSON_STANCE = new Passive(id("crimson_stance"), List.of(), List.of());
    public static final String CRIMSON_CONTACT_HITS = "crimson_contact.hits";
    public static final String SCARLET_RELEASE_HITS = "scarlet_release.hits";
    public static final Quest CRIMSON_CONTACT = new Quest("crimson_contact", SCARLET_TEAR.id(),
            List.of(new QuestStep(CRIMSON_CONTACT_HITS, 20)));
    public static final Quest SCARLET_RELEASE = new Quest("scarlet_release", BLOOD_CRYSTAL_FORM.id(),
            List.of(new QuestStep(SCARLET_RELEASE_HITS, 15)));
    public static final String CRYSTAL_MASTERY_HITS = "crystal_mastery.hits";
    public static final Quest CRYSTAL_MASTERY = new Quest("crystal_mastery", CRYSTAL_BARRAGE.id(),
            List.of(new QuestStep(CRYSTAL_MASTERY_HITS, 10)));
    /** Blood Iceberg: three Blood Crystal Forms held to their end. */
    public static final String CRIMSON_PEAK_FORMS = "crimson_peak.forms";
    public static final Quest CRIMSON_PEAK = new Quest("crimson_peak", BLOOD_ICEBERG.id(),
            List.of(new QuestStep(CRIMSON_PEAK_FORMS, 3)));

    /** The realm an art's manual asks of its reader, if any. */
    @Nullable public static com.murimblock.cultivation.CultivationRealm requiredRealm(ResourceLocation art) {
        return art.equals(SANGUINE_ART) ? SANGUINE_REALM : art.equals(MOUNTAIN_SHATTERING_FIST) ? MOUNTAIN_FIST_REALM
                : art.equals(SEVEN_STARS_SWORD) ? SEVEN_STARS_REALM : null;
    }

    private static final Map<ResourceLocation, Art> ARTS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Technique> TECHNIQUES = new LinkedHashMap<>();

    static {
        add(new Art(AZURE_CLOUD_SWORD, List.of(CLOUD_STEP_SLASH, THOUSAND_DRIZZLE, HEAVEN_SPLITTING_CRESCENT, SWALLOW_RETURNS),
                FLOWING_CLOUD_STANCE, List.of(THE_PATIENT_BLADE, ABOVE_THE_SEA_OF_CLOUDS),
                List.of(THOUSAND_LI_CLOUD_STEP, STORM_OF_NEEDLES, AZURE_HEAVEN_CRESCENT)));
        add(new Art(LONG_FIST, List.of(TIGER_STEP_PUNCH, RAPID_FISTS, WHIRLWIND_KICK, INCH_FORCE),
                STEADY_BREATH, List.of(FIRST_STEPS, UNBROKEN_FLURRY),
                List.of(TIGER_LEAP, HUNDRED_FISTS, TORNADO_KICK)));
        add(new Art(CELESTIAL_PEAK_SPEAR, List.of(PIERCING_DRAGON, PLUM_BLOSSOM_THRUSTS, THOUSAND_SOLDIER_SWEEP),
                LONG_REACH, List.of(FIRST_LINE, HOLD_THE_DISTANCE), List.of(SOARING_DRAGON, PLUM_RAIN, TEN_THOUSAND_SWEEP)));
        // No quests, secret nor evolutions defined yet: the scroll teaches all three techniques.
        add(new Art(TRAVELLER_STAFF, List.of(VERTICAL_STRIKE, POINT_FLURRY, SPINNING_RAMPART), THIRD_RESONANCE, List.of(), List.of()));
        add(new Art(MOUNTAIN_SHATTERING_FIST, List.of(MOUNTAIN_CRUSHING_PUNCH, EARTH_BREAKER, BULL_CHARGE, HEAVEN_FALLING_MOUNTAIN, SKY_BREAKER),
                EARTHSHAKER, List.of(SHATTER_THE_STONE, SHAKE_THE_EARTH, HEAVENS_WEIGHT), List.of(MOUNTAIN_SPLITTING_PUNCH, EARTHQUAKE, STAMPEDE)));
        add(new Art(SEVEN_STARS_SWORD, List.of(SHOOTING_STAR, DIPPER_STEPS, ORBITING_STARS, CONSTELLATION_LINK, FALLING_STAR_RAIN, SEVEN_STARS_ARRAY, POLARIS_RIPOSTE),
                STAR_MARKS, List.of(SEVEN_POINTS, WALK_THE_DIPPER, UNBROKEN_ORBIT, BIND_THE_SKY, SEVEN_SWORDS), List.of(TWIN_COMETS, FULL_DIPPER, METEOR_SHOWER)));
        add(new Art(GOLDEN_BELL, List.of(GOLDEN_BELL_FORM, IRON_REBOUND, ROOTED_MOUNTAIN, BELL_TOLL), BELL_PALMS,
                List.of(TAKE_THE_BLOW, RETURN_IT), List.of(STEADFAST_BELL, MIRROR_IRON, ANCIENT_MOUNTAIN)));
        add(new Art(SANGUINE_ART, List.of(BLOOD_CRYSTAL, SCARLET_TEAR, BLOOD_CRYSTAL_FORM, CRYSTAL_BARRAGE, BLOOD_ICEBERG),
                CRIMSON_STANCE, List.of(CRIMSON_CONTACT, SCARLET_RELEASE, CRYSTAL_MASTERY, CRIMSON_PEAK), List.of()));
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
