package com.murimblock.mob;

import com.murimblock.Murimblock;
import com.murimblock.integration.epicfight.EpicFightMobSupport;
import com.murimblock.martial.QiBladeEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import yesman.epicfight.registry.entries.EpicFightAttributes;

/** Murim entities have stable IDs; the training fixture has no natural spawn, the crabs, the crab man and the bandit do. */
public final class MurimEntities {
    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Murimblock.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<com.murimblock.martial.BloodCrystalEntity>> BLOOD_CRYSTAL =
            TYPES.register("blood_crystal", () -> EntityType.Builder.<com.murimblock.martial.BloodCrystalEntity>of(com.murimblock.martial.BloodCrystalEntity::new,MobCategory.MISC)
                    .sized(.75F,.75F).clientTrackingRange(8).updateInterval(1).build("murimblock:blood_crystal"));
    /** A piece of Scarlet Bloom's show (a crystal, a beam, a stone): shown only, never saved. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.murimblock.martial.ScarletPieceEntity>> SCARLET_PIECE =
            TYPES.register("scarlet_piece", () -> EntityType.Builder.<com.murimblock.martial.ScarletPieceEntity>of(com.murimblock.martial.ScarletPieceEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F).noSave().fireImmune().clientTrackingRange(8).updateInterval(1).build("murimblock:scarlet_piece"));
    public static final DeferredHolder<EntityType<?>, EntityType<TrainingOpponentEntity>> TRAINING_OPPONENT =
            TYPES.register("training_opponent", () -> EntityType.Builder.of(TrainingOpponentEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build("murimblock:training_opponent"));
    /** Slightly smaller than a vanilla spider (1.4 x 0.9). */
    public static final DeferredHolder<EntityType<?>, EntityType<ArmoredCrabEntity>> ARMORED_CRAB =
            TYPES.register("armored_crab", () -> EntityType.Builder.of(ArmoredCrabEntity::new, MobCategory.MONSTER)
                    .sized(1.1F, 0.6F).clientTrackingRange(8).build("murimblock:armored_crab"));
    /** Infected crab man, about a zombie's size; rare natural spawn near water, structures later. */
    public static final DeferredHolder<EntityType<?>, EntityType<CrabManEntity>> CRAB_MAN =
            TYPES.register("crab_man", () -> EntityType.Builder.of(CrabManEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 1.95F).clientTrackingRange(8).build("murimblock:crab_man"));
    /** The Mountain Bandit: a man's size, as frequent as the zombie in the Murim biomes, in bands of two to four. */
    public static final DeferredHolder<EntityType<?>, EntityType<MountainBanditEntity>> MOUNTAIN_BANDIT =
            TYPES.register("mountain_bandit", () -> EntityType.Builder.of(MountainBanditEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(8).build("murimblock:mountain_bandit"));
    /** The Bandit Chief, the mini-boss: drawn 1.3 times a man's size (his box too). Not spawned in the world yet. */
    public static final DeferredHolder<EntityType<?>, EntityType<BanditChiefEntity>> BANDIT_CHIEF =
            TYPES.register("bandit_chief", () -> EntityType.Builder.of(BanditChiefEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.5F).eyeHeight(2.1F).clientTrackingRange(10).build("murimblock:bandit_chief"));
    /** The Crab King, the crab art's boss: drawn 1.5 times a man's size (his box too). Never spawned naturally. */
    public static final DeferredHolder<EntityType<?>, EntityType<CrabKingEntity>> CRAB_KING =
            TYPES.register("crab_king", () -> EntityType.Builder.of(CrabKingEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.9F).eyeHeight(2.45F).clientTrackingRange(10).build("murimblock:crab_king"));
    /** Sensei Yun, master of the Azure Cloud Sect: a man's size, placed in the Misty Bamboo Temple, never spawned naturally. */
    public static final DeferredHolder<EntityType<?>, EntityType<AzureSenseiEntity>> AZURE_SENSEI =
            TYPES.register("azure_sensei", () -> EntityType.Builder.of(AzureSenseiEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).build("murimblock:azure_sensei"));
    /** The Alliance Merchant, who trades in silver taels: a man's size, never spawned naturally (an egg, /summon). */
    public static final DeferredHolder<EntityType<?>, EntityType<AllianceMerchantEntity>> ALLIANCE_MERCHANT =
            TYPES.register("alliance_merchant", () -> EntityType.Builder.of(AllianceMerchantEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).build("murimblock:alliance_merchant"));
    /** The Captain of the Peak Guard, the Alliance's spear boss: a man's size, never spawned naturally (an egg, /summon, his arena). */
    public static final DeferredHolder<EntityType<?>, EntityType<PeakCaptainEntity>> PEAK_CAPTAIN =
            TYPES.register("peak_captain", () -> EntityType.Builder.of(PeakCaptainEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).build("murimblock:peak_captain"));
    /** The Orthodox Alliance's soldiers, one type for each kind (AllianceSoldierEntity.Kind): a man's size. */
    public static final DeferredHolder<EntityType<?>, EntityType<AllianceSoldierEntity>> ALLIANCE_SPEAR_GUARD = soldier(AllianceSoldierEntity.Kind.SPEAR_GUARD);
    public static final DeferredHolder<EntityType<?>, EntityType<AllianceSoldierEntity>> ALLIANCE_SWORD_DISCIPLE = soldier(AllianceSoldierEntity.Kind.SWORD_DISCIPLE);
    public static final DeferredHolder<EntityType<?>, EntityType<AllianceSoldierEntity>> ALLIANCE_ARCHER = soldier(AllianceSoldierEntity.Kind.ARCHER);
    public static final DeferredHolder<EntityType<?>, EntityType<AllianceSoldierEntity>> ALLIANCE_WARRIOR_MONK = soldier(AllianceSoldierEntity.Kind.WARRIOR_MONK);
    public static final DeferredHolder<EntityType<?>, EntityType<AllianceSoldierEntity>> ALLIANCE_SERGEANT = soldier(AllianceSoldierEntity.Kind.SERGEANT);
    /** The soldiers in the order of their kinds. */
    public static final java.util.List<DeferredHolder<EntityType<?>, EntityType<AllianceSoldierEntity>>> ALLIANCE_SOLDIERS =
            java.util.List.of(ALLIANCE_SPEAR_GUARD, ALLIANCE_SWORD_DISCIPLE, ALLIANCE_ARCHER, ALLIANCE_WARRIOR_MONK, ALLIANCE_SERGEANT);
    /** The crab man's water ball; its hitbox follows the ball's scale (big 0.6, small ones smaller). */
    public static final DeferredHolder<EntityType<?>, EntityType<WaterBallEntity>> WATER_BALL =
            TYPES.register("water_ball", () -> EntityType.Builder.<WaterBallEntity>of(WaterBallEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F).clientTrackingRange(8).updateInterval(2).build("murimblock:water_ball"));
    /** A blade of Qi sent by the Azure Cloud Sword Art (Thousand Drizzle's wave, its needle, the azure crescent). */
    public static final DeferredHolder<EntityType<?>, EntityType<QiBladeEntity>> QI_BLADE =
            TYPES.register("qi_blade", () -> EntityType.Builder.<QiBladeEntity>of(QiBladeEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(6).updateInterval(1).build("murimblock:qi_blade"));
    /** A needle thrown by the flying needles (FlyingNeedleEntity). */
    public static final DeferredHolder<EntityType<?>, EntityType<FlyingNeedleEntity>> FLYING_NEEDLE =
            TYPES.register("flying_needle", () -> EntityType.Builder.<FlyingNeedleEntity>of(FlyingNeedleEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(6).updateInterval(1).build("murimblock:flying_needle"));
    /** The inner world (docs/cultivation/qi-cultivation.md): a cultivator's own darkness, a dark copy of them. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.murimblock.cultivation.inner.HeartDemonEntity>> HEART_DEMON =
            TYPES.register("heart_demon", () -> EntityType.Builder.of(com.murimblock.cultivation.inner.HeartDemonEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).build("murimblock:heart_demon"));
    /** The body left seated in the lotus while its owner is in their inner world. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.murimblock.cultivation.inner.MeditatingBodyEntity>> MEDITATING_BODY =
            TYPES.register("meditating_body", () -> EntityType.Builder.<com.murimblock.cultivation.inner.MeditatingBodyEntity>of(
                            com.murimblock.cultivation.inner.MeditatingBodyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.0F).clientTrackingRange(10).build("murimblock:meditating_body"));
    /** The remnant soul of an old master, in each inner world: it gives the day's missions. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.murimblock.cultivation.inner.RemnantSoulEntity>> REMNANT_SOUL =
            TYPES.register("remnant_soul", () -> EntityType.Builder.of(com.murimblock.cultivation.inner.RemnantSoulEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).clientTrackingRange(10).build("murimblock:remnant_soul"));
    /** A mote of scattered Qi (a mission of the inner world). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.murimblock.cultivation.inner.QiMoteEntity>> QI_MOTE =
            TYPES.register("qi_mote", () -> EntityType.Builder.<com.murimblock.cultivation.inner.QiMoteEntity>of(
                            com.murimblock.cultivation.inner.QiMoteEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F).clientTrackingRange(10).build("murimblock:qi_mote"));
    private static DeferredHolder<EntityType<?>, EntityType<AllianceSoldierEntity>> soldier(AllianceSoldierEntity.Kind kind) {
        return TYPES.register(kind.id, () -> EntityType.Builder.<AllianceSoldierEntity>of((type, level) -> new AllianceSoldierEntity(type, level, kind), MobCategory.CREATURE)
                .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).build("murimblock:" + kind.id));
    }

    private MurimEntities() { }
    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener(MurimEntities::attributes);
        bus.addListener(MurimEntities::spawnPlacements);
    }
    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(TRAINING_OPPONENT.get(), TrainingOpponentEntity.createTrainingAttributes().build());
        event.put(ARMORED_CRAB.get(), EpicFightMobSupport.creatureAttributes(ArmoredCrabEntity.createCrabAttributes()).build());
        event.put(CRAB_MAN.get(), EpicFightMobSupport.creatureAttributes(CrabManEntity.createCrabManAttributes())
                .add(EpicFightAttributes.IMPACT, 1.5).build());
        event.put(MOUNTAIN_BANDIT.get(), MountainBanditEntity.createBanditAttributes().build());
        event.put(BANDIT_CHIEF.get(), BanditChiefEntity.createChiefAttributes().build());
        event.put(CRAB_KING.get(), CrabKingEntity.createCrabKingAttributes().build());
        event.put(HEART_DEMON.get(), com.murimblock.cultivation.inner.HeartDemonEntity.createAttributes().build());
        event.put(REMNANT_SOUL.get(), com.murimblock.cultivation.inner.RemnantSoulEntity.createAttributes().build());
        event.put(AZURE_SENSEI.get(), AzureSenseiEntity.createSenseiAttributes().build());
        event.put(ALLIANCE_MERCHANT.get(), AllianceMerchantEntity.createMerchantAttributes().build());
        event.put(PEAK_CAPTAIN.get(), PeakCaptainEntity.createCaptainAttributes().build());
        for (AllianceSoldierEntity.Kind kind : AllianceSoldierEntity.Kind.values())
            event.put(ALLIANCE_SOLDIERS.get(kind.ordinal()).get(), kind.attributes().build());
    }
    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Biomes, weight and group size live in data/murimblock/neoforge/biome_modifier/armored_crab_spawns.json.
        event.register(ARMORED_CRAB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ArmoredCrabEntity::checkCrabSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Same biomes, about seven times rarer: data/murimblock/neoforge/biome_modifier/crab_man_spawns.json.
        event.register(CRAB_MAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                CrabManEntity::checkCrabManSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // As the zombie: on the ground, in the dark (data/murimblock/neoforge/biome_modifier/mountain_bandit_spawns.json).
        event.register(MOUNTAIN_BANDIT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
