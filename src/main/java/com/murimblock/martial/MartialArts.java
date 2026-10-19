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

    // ---------------------------------------------------------------- the Purple Heaven Thunder Saber (docs/combat/styles/purple-heaven-thunder-saber.md)

    /**
     * A free art of the middle of the game (Enzo, 17/10/2026: "une grosse arme à deux mains"), a greatsword only, learnt
     * from Qi Condensation: heavy blows beat thunder drums, a technique beats one to call the purple lightning. Five
     * techniques, a secret, an ultimate.
     */
    public static final ResourceLocation THUNDER_SABER = id("thunder_saber");
    public static final com.murimblock.cultivation.CultivationRealm THUNDER_SABER_REALM = com.murimblock.cultivation.CultivationRealm.QI_CONDENSATION;
    public static final Technique THUNDER_DESCENDS = new Technique(id("thunder_descends"), THUNDER_SABER, 30, 160);
    public static final Technique ROLLING_THUNDER = new Technique(id("rolling_thunder"), THUNDER_SABER, 40, 240);
    /** Held: the saber planted, every blow from the front stopped, until the key is let go (3 s at most). */
    public static final Technique HEAVENS_DRUM = new Technique(id("heavens_drum"), THUNDER_SABER, 45, 320);
    public static final int HEAVENS_DRUM_MAX_TICKS = 60;
    public static final double HEAVENS_DRUM_QI_PER_SECOND = 5;
    /** Held to charge (1.5 s at most), let go to cut. */
    public static final Technique SEVERING_FLASH = new Technique(id("severing_flash"), THUNDER_SABER, 55, 280);
    public static final int SEVERING_FLASH_MAX_CHARGE = 30;
    public static final Technique NINE_HEAVENS_WRATH = new Technique(id("nine_heavens_wrath"), THUNDER_SABER, 70, 440);
    /** The ultimate: struck by the purple lightning, eight seconds of a thunder god; three minutes to recover. */
    public static final Technique THUNDER_SOVEREIGN = new Technique(id("thunder_sovereign"), THUNDER_SABER, 150, 3600);
    /** The secret: a saber strike right after a perfect parry, on the attacker: the sky answers with a bolt. */
    public static final Technique HEAVEN_ANSWERS = new Technique(id("heaven_answers"), THUNDER_SABER, 0, 0, true);
    /** Its passive keeps Epic Fight's greatsword strikes; each that lands beats a thunder drum. */
    public static final Passive ROLLING_DRUMS = new Passive(id("rolling_drums"), List.of(), List.of());
    public static final String FIRST_DRUM_FULL = "first_drum.full";
    public static final String UNMOVED_HITS = "unmoved.hits";
    public static final String DRUM_ANSWERS_BLOCKS = "drum_answers.blocks";
    public static final String CALL_DOWN_SKY_FLASHES = "call_down_sky.flashes";
    public static final String SOVEREIGN_BOLTS = "sovereign.bolts";
    /** Not a quest: perfect parries in the stance until Heaven Answers is found. */
    public static final String HEAVEN_ANSWERS_PARRIES = "heaven_answers.parries";
    public static final Quest FIRST_DRUM = new Quest("first_drum", ROLLING_THUNDER.id(), List.of(new QuestStep(FIRST_DRUM_FULL, 20)));
    public static final Quest UNMOVED = new Quest("unmoved", HEAVENS_DRUM.id(), List.of(new QuestStep(UNMOVED_HITS, 50)));
    public static final Quest DRUM_ANSWERS = new Quest("drum_answers", SEVERING_FLASH.id(), List.of(new QuestStep(DRUM_ANSWERS_BLOCKS, 30)));
    public static final Quest CALL_DOWN_SKY = new Quest("call_down_sky", NINE_HEAVENS_WRATH.id(), List.of(new QuestStep(CALL_DOWN_SKY_FLASHES, 10)));
    public static final Quest SOVEREIGN_PATH = new Quest("sovereign_path", THUNDER_SOVEREIGN.id(), List.of(new QuestStep(SOVEREIGN_BOLTS, 100)));
    /** At 30: Thunder Descends may leap again once, free, within 2 s, if it struck. */
    public static final Evolution SECOND_THUNDER = new Evolution(THUNDER_DESCENDS.id(), 30, "evolution.murimblock.second_thunder");
    /** At 60: Rolling Thunder turns three times. At 90: Nine Heavens' Wrath leaves a storm over its circle. */
    public static final Evolution THIRD_TURN = new Evolution(ROLLING_THUNDER.id(), 60, "evolution.murimblock.third_turn");
    public static final Evolution LINGERING_STORM = new Evolution(NINE_HEAVENS_WRATH.id(), 90, "evolution.murimblock.lingering_storm");
    // ---------------------------------------------------------------- the Majin Fist (docs/combat/styles/majin-fist.md)

    /**
     * The Son of the Demon's art, the Pride of the Sun (Enzo, 17/10/2026: "make me a martial art with these techniques; he
     * drops it when killed"): bare hands and the sun's fire, learnt from Qi Liquidation, a high art. Five techniques, an
     * ultimate (The One), a secret (Grand Noon), its passive Pride of the Sun.
     */
    public static final ResourceLocation MAJIN_FIST = id("majin_fist");
    public static final com.murimblock.cultivation.CultivationRealm MAJIN_REALM = com.murimblock.cultivation.CultivationRealm.QI_LIQUIDATION;
    public static final Technique SUNFIRE_FIST = new Technique(id("sunfire_fist"), MAJIN_FIST, 50, 140);
    public static final Technique PRIDE_FLARE = new Technique(id("pride_flare"), MAJIN_FIST, 70, 240);
    public static final Technique CRUEL_SUN = new Technique(id("cruel_sun"), MAJIN_FIST, 90, 360);
    public static final Technique NOON_JUDGMENT = new Technique(id("noon_judgment"), MAJIN_FIST, 100, 400);
    public static final Technique MIDDAY_METEOR = new Technique(id("midday_meteor"), MAJIN_FIST, 90, 320);
    /** The ultimate: the sun born over the body, 20 s of its strength, then brought down; five minutes to recover. */
    public static final Technique THE_ONE = new Technique(id("the_one"), MAJIN_FIST, 250, 6000);
    /** The secret: Sunfire Fist cast crouching under the noon sun, a pillar of sunlight on its target (costs what it costs). */
    public static final Technique GRAND_NOON = new Technique(id("grand_noon"), MAJIN_FIST, 0, 0, true);
    /** Its passive keeps Epic Fight's bare-hand strikes; each that lands sets its target alight, harder by day under the sky. */
    public static final Passive PRIDE_OF_THE_SUN = new Passive(id("pride_of_the_sun"), List.of(), List.of());
    public static final String KINDLING_BURNS = "kindling.burns";
    public static final String SPREADING_FLAME_HITS = "spreading_flame.hits";
    public static final String SUNS_WRATH_HITS = "suns_wrath.hits";
    public static final String HIGH_NOON_HITS = "high_noon.hits";
    public static final String PRIDE_INCARNATE_SLAIN = "pride_incarnate.slain";
    public static final Quest KINDLING = new Quest("kindling", PRIDE_FLARE.id(), List.of(new QuestStep(KINDLING_BURNS, 30)));
    public static final Quest SPREADING_FLAME = new Quest("spreading_flame", CRUEL_SUN.id(), List.of(new QuestStep(SPREADING_FLAME_HITS, 25)));
    public static final Quest SUNS_WRATH = new Quest("suns_wrath", NOON_JUDGMENT.id(), List.of(new QuestStep(SUNS_WRATH_HITS, 15)));
    public static final Quest HIGH_NOON = new Quest("high_noon", MIDDAY_METEOR.id(), List.of(new QuestStep(HIGH_NOON_HITS, 15)));
    public static final Quest PRIDE_INCARNATE = new Quest("pride_incarnate", THE_ONE.id(), List.of(new QuestStep(PRIDE_INCARNATE_SLAIN, 50)));
    /** At 30: Sunfire Fist bursts twice as wide. At 60: Pride Flare runs farther and wider. At 90: Cruel Sun is two suns. */
    public static final Evolution SOLAR_FIST = new Evolution(SUNFIRE_FIST.id(), 30, "evolution.murimblock.solar_fist");
    public static final Evolution INFERNO_FLARE = new Evolution(PRIDE_FLARE.id(), 60, "evolution.murimblock.inferno_flare");
    public static final Evolution TWIN_SUNS = new Evolution(CRUEL_SUN.id(), 90, "evolution.murimblock.twin_suns");

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
    // ---------------------------------------------------------------- the Overlord Spear (docs/combat/styles/overlord-spear.md)

    /**
     * The Overlord Spear (Enzo, 18/10/2026: an intermediate spear art "with a certain destructive power"): one does not
     * pierce, one pulverises. A spear in hand, learnt from Qi Condensation; its manual comes to whoever brings the
     * Celestial Peak Spear to mastery 50. No quests: the manual teaches its five techniques and its ultimate.
     */
    public static final ResourceLocation OVERLORD_SPEAR = id("overlord_spear");
    public static final com.murimblock.cultivation.CultivationRealm OVERLORD_REALM = com.murimblock.cultivation.CultivationRealm.QI_CONDENSATION;
    /** The Celestial Peak Spear's mastery that brings the manual. */
    public static final int OVERLORD_FROM_MASTERY = 50;
    /** Held to charge (0.5 to 1.5 s), let go: a beam of Qi through all in its line. */
    public static final Technique TYRANTS_THRUST = new Technique(id("tyrants_thrust"), OVERLORD_SPEAR, 35, 160);
    public static final int TYRANTS_THRUST_MAX_CHARGE = 30;
    public static final Technique EARTH_SPLITTING_SWEEP = new Technique(id("earth_splitting_sweep"), OVERLORD_SPEAR, 50, 240);
    public static final Technique FALLING_MOUNTAIN = new Technique(id("falling_mountain"), OVERLORD_SPEAR, 60, 300);
    public static final Technique OVERLORDS_CHARGE = new Technique(id("overlords_charge"), OVERLORD_SPEAR, 55, 280);
    public static final Technique HUNDRED_BATTLES_RAMPAGE = new Technique(id("hundred_battles_rampage"), OVERLORD_SPEAR, 75, 440);
    /** The ultimate: a giant spear of Qi falls from the sky; four minutes to recover. */
    public static final Technique CONQUEROR_DESCENDS = new Technique(id("conqueror_descends"), OVERLORD_SPEAR, 180, 4800);
    /** The secret: Tyrant's Thrust under a fifth of one's health, its beam three blocks wide and twice as strong. */
    public static final Technique BREAKING_THE_CAULDRONS = new Technique(id("breaking_the_cauldrons"), OVERLORD_SPEAR, 0, 0, true);
    /** Its passive keeps Epic Fight's spear strikes; each that lands adds a charge of Domination, five make a battering ram. */
    public static final Passive CONQUERORS_WEIGHT = new Passive(id("conquerors_weight"), List.of(), List.of());

    // ---------------------------------------------------------------- the Crescent Moon Halberd (docs/combat/styles/crescent-moon-halberd.md)

    /**
     * The Crescent Moon Halberd (月牙戟, Enzo 19/10/2026): an orthodox art of the halberd, of the moon and water, elegant,
     * all in circles, at middle reach. A spear in hand (any Epic Fight spear; the Crescent Moon Halberd is its own),
     * learnt from Qi Condensation. No quests: its scroll teaches its four techniques and its ultimate.
     */
    public static final ResourceLocation CRESCENT_MOON_HALBERD = id("crescent_moon_halberd");
    public static final com.murimblock.cultivation.CultivationRealm CRESCENT_MOON_REALM = com.murimblock.cultivation.CultivationRealm.QI_CONDENSATION;
    /** A whole turn low, then a ring of moon-water on the ground that slows whoever stands in it. */
    public static final Technique MOONLIT_CIRCLE = new Technique(id("moonlit_circle"), CRESCENT_MOON_HALBERD, 45, 240);
    /** The hook caught on a foe ahead: pulled in to the halberd, or, too heavy, tripped where it stands. */
    public static final Technique TIDE_HOOK = new Technique(id("tide_hook"), CRESCENT_MOON_HALBERD, 35, 180);
    /** A great slash down and across: three crescents of light fly off in a fan. */
    public static final Technique FALLING_CRESCENT = new Technique(id("falling_crescent"), CRESCENT_MOON_HALBERD, 55, 260);
    /** Held: the halberd upright before the body, a mirror of still water; what strikes it from the front is sent back as a crescent. */
    public static final Technique MIRROR_LAKE_STANCE = new Technique(id("mirror_lake_stance"), CRESCENT_MOON_HALBERD, 30, 300);
    public static final int MIRROR_MAX_TICKS = 60;
    public static final double MIRROR_QI_PER_SECOND = 5;
    /** The ultimate: night falls round the cultivator, a full moon rises and its beams strike the enemies; four minutes to recover. */
    public static final Technique NIGHT_OF_THE_FULL_MOON = new Technique(id("night_of_the_full_moon"), CRESCENT_MOON_HALBERD, 180, 4800);
    /** Its passive keeps Epic Fight's spear strikes; each that lands fills the moon, and a full moon sends a crescent off the next. */
    public static final Passive WAXING_MOON = new Passive(id("waxing_moon"), List.of(), List.of());

    // ---------------------------------------------------------------- the Devouring Demon Art (docs/combat/styles/devouring-demon-art.md)

    /**
     * A high art of the kagune (Enzo, 18/10/2026), bare hands: four tentacles under 100 Fury, the demon form on all fours
     * at 100 (DevouringDemon). Eight techniques, none costing Fury, each changing in the demon form on the same key
     * (DEMON_VERSIONS); built one pair at a time. Proposed realm: Foundation Establishment.
     */
    public static final ResourceLocation DEVOURING_DEMON = id("devouring_demon");
    public static final com.murimblock.cultivation.CultivationRealm DEVOURING_DEMON_REALM = com.murimblock.cultivation.CultivationRealm.FOUNDATION_ESTABLISHMENT;
    /** The four tentacles whip in a fan in front of him. In the demon form: Centipede Bite. */
    public static final Technique LASH = new Technique(id("lash"), DEVOURING_DEMON, 160, 100);
    /** A centipede shoots at the foe ahead, up to 7 blocks, and bites: the key of Lash, in the demon form. */
    public static final Technique CENTIPEDE_BITE = new Technique(id("centipede_bite"), DEVOURING_DEMON, 160, 100);
    /** The tentacles plunge into the ground and burst out under the foe looked at, up to 12 blocks: impaled, lifted, held. */
    public static final Technique IMPALING_THORNS = new Technique(id("impaling_thorns"), DEVOURING_DEMON, 220, 160);
    /** In the demon form: the centipedes dive into the ground and burst out under the two nearest foes ahead. */
    public static final Technique BURROW = new Technique(id("burrow"), DEVOURING_DEMON, 220, 160);
    /** A tentacle shoots at what he looks at (14 blocks): a foe is pulled in to him; else it hooks the block and pulls him there. */
    public static final Technique GRAPPLE = new Technique(id("grapple"), DEVOURING_DEMON, 140, 120);
    /**
     * Trades the four tentacles for the two centipedes, standing, or back (Enzo, 19/10/2026): free, a second to recover; the
     * keys then cast the centipedes' versions. Not at full Fury: the demon form has both at once.
     */
    public static final Technique KAGUNE_SHIFT = new Technique(id("kagune_shift"), DEVOURING_DEMON, 0, 20);
    /** In the demon form: a centipede coils round the foe looked at (8 blocks), holds it 2 s and crushes it four times. */
    public static final Technique CONSTRICT = new Technique(id("constrict"), DEVOURING_DEMON, 140, 120);
    /** Epic Fight's bare-hand strikes for now; the tentacles' own M1 come next. Each blow feeds the Fury. */
    public static final Passive DEVOURING_HUNGER = new Passive(id("devouring_hunger"), List.of(), List.of());
    /** Each technique's version in the demon form, cast by its key there (its cost and cooldown are the key's). */
    public static final Map<Technique, Technique> DEMON_VERSIONS = Map.of(LASH, CENTIPEDE_BITE, IMPALING_THORNS, BURROW, GRAPPLE, CONSTRICT);

    @Nullable public static com.murimblock.cultivation.CultivationRealm requiredRealm(ResourceLocation art) {
        return art.equals(SANGUINE_ART) ? SANGUINE_REALM : art.equals(MOUNTAIN_SHATTERING_FIST) ? MOUNTAIN_FIST_REALM
                : art.equals(SEVEN_STARS_SWORD) ? SEVEN_STARS_REALM : art.equals(THUNDER_SABER) ? THUNDER_SABER_REALM
                : art.equals(MAJIN_FIST) ? MAJIN_REALM : art.equals(OVERLORD_SPEAR) ? OVERLORD_REALM
                : art.equals(DEVOURING_DEMON) ? DEVOURING_DEMON_REALM : art.equals(CRESCENT_MOON_HALBERD) ? CRESCENT_MOON_REALM : null;
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
        add(new Art(THUNDER_SABER, List.of(THUNDER_DESCENDS, ROLLING_THUNDER, HEAVENS_DRUM, SEVERING_FLASH, NINE_HEAVENS_WRATH, THUNDER_SOVEREIGN, HEAVEN_ANSWERS),
                ROLLING_DRUMS, List.of(FIRST_DRUM, UNMOVED, DRUM_ANSWERS, CALL_DOWN_SKY, SOVEREIGN_PATH), List.of(SECOND_THUNDER, THIRD_TURN, LINGERING_STORM)));
        add(new Art(MAJIN_FIST, List.of(SUNFIRE_FIST, PRIDE_FLARE, CRUEL_SUN, NOON_JUDGMENT, MIDDAY_METEOR, THE_ONE, GRAND_NOON),
                PRIDE_OF_THE_SUN, List.of(KINDLING, SPREADING_FLAME, SUNS_WRATH, HIGH_NOON, PRIDE_INCARNATE), List.of(SOLAR_FIST, INFERNO_FLARE, TWIN_SUNS)));

        // No quests (Enzo's design): the manual teaches the five techniques and the ultimate; the secret is found.
        add(new Art(OVERLORD_SPEAR, List.of(TYRANTS_THRUST, EARTH_SPLITTING_SWEEP, FALLING_MOUNTAIN, OVERLORDS_CHARGE, HUNDRED_BATTLES_RAMPAGE,
                CONQUEROR_DESCENDS, BREAKING_THE_CAULDRONS), CONQUERORS_WEIGHT, List.of(), List.of()));

        // No quests (Enzo's choice of design): the scroll teaches the four techniques and the ultimate.
        add(new Art(CRESCENT_MOON_HALBERD, List.of(MOONLIT_CIRCLE, TIDE_HOOK, FALLING_CRESCENT, MIRROR_LAKE_STANCE, NIGHT_OF_THE_FULL_MOON),
                WAXING_MOON, List.of(), List.of()));

        add(new Art(GOLDEN_BELL, List.of(GOLDEN_BELL_FORM, IRON_REBOUND, ROOTED_MOUNTAIN, BELL_TOLL), BELL_PALMS,
                List.of(TAKE_THE_BLOW, RETURN_IT), List.of(STEADFAST_BELL, MIRROR_IRON, ANCIENT_MOUNTAIN)));
        add(new Art(SANGUINE_ART, List.of(BLOOD_CRYSTAL, SCARLET_TEAR, BLOOD_CRYSTAL_FORM, CRYSTAL_BARRAGE, BLOOD_ICEBERG),
                CRIMSON_STANCE, List.of(CRIMSON_CONTACT, SCARLET_RELEASE, CRYSTAL_MASTERY, CRIMSON_PEAK), List.of()));
        // No quests yet: the scroll teaches every technique built so far.
        add(new Art(DEVOURING_DEMON, List.of(LASH, IMPALING_THORNS, GRAPPLE, KAGUNE_SHIFT), DEVOURING_HUNGER, List.of(), List.of()));
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
