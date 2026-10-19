package com.murimblock;

import com.murimblock.cultivation.CultivationData;
import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationRegistries;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.cultivation.MeditationService;
import com.murimblock.cultivation.QiSense;
import com.murimblock.cultivation.inner.HeartDemonEntity;
import com.murimblock.cultivation.inner.InnerMissions;
import com.murimblock.cultivation.inner.SoulDialogue;
import com.murimblock.dialogue.DialogueService;
import com.murimblock.cultivation.inner.InnerWorldService;
import com.murimblock.cultivation.inner.MeditatingBodyEntity;
import com.murimblock.cultivation.inner.QiMoteEntity;
import com.murimblock.cultivation.inner.RemnantSoulEntity;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.mob.MurimEntities;
import com.murimblock.network.CultivationPayloads;
import com.murimblock.qi.QiRewardManager;
import com.murimblock.qi.QiService;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * Cultivation on the server (docs/cultivation/qi-cultivation.md): meditation condensing Qi into Qi Max, the small
 * breakthroughs and the gate of a realm, Qi Max from techniques and kills, the realm's bonuses, death's floor, pills and
 * toxicity, the inner world (going in, the body left behind, coming back), the Heart Demon won and lost, the body struck,
 * the missions, and what Qi Sense lets each reader see. Real server players on mock connections, ticked by hand.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CultivationGameTests {
    private CultivationGameTests() { }

    /**
     * A player on the ground of the arena, bare-handed, combat mode off, full Qi. The GameTest server has no datapack
     * dimension: the End, three million blocks out, stands in for the inner world.
     */
    private static ServerPlayer cultivator(GameTestHelper helper) {
        InnerWorldService.useStandInForTests(helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END), 3_000_000);
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        com.murimblock.combat.CombatService.setCombatMode(player, false);
        player.setOnGround(true);
        return player;
    }

    /**
     * The living entities of a kind near the player, among all the level's: one just added (or in a chunk just loaded)
     * is not found by area for a few ticks.
     */
    private static <T extends net.minecraft.world.entity.Entity> List<T> near(ServerPlayer player, Class<T> type) {
        List<T> out = new java.util.ArrayList<>();
        for (var e : player.serverLevel().getAllEntities())
            if (type.isInstance(e) && e.isAlive() && e.distanceTo(player) < 80) out.add(type.cast(e));
        return out;
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int from, int ticks) {
        for (int t = from; t < from + ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            if (InnerWorldService.isInside(p)) InnerWorldService.leave(p, InnerWorldService.Exit.LOGIN);
            MeditationService.forget(p);
            cleanup(p);
        }
        InnerWorldService.clearForTests();
        InnerWorldService.useStandInForTests(null, 0);
        InnerMissions.clearForTests();
        QiSense.clearForTests();
        helper.succeed();
    }

    // ---------------------------------------------------------------- meditation and Qi Max

    @GameTest(template = "foundation_arena", batch = "cultivation_meditation")
    public static void meditationCondensesQiIntoQiMaxOnlyWithAFullReserve(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        helper.assertTrue(MeditationService.start(player), "Could not sit down to meditate");
        helper.assertTrue(MeditationService.aura(player).meditating(), "Other clients are not told of the meditation");
        QiService.setQi(player, 50);
        MeditationService.condense(player, 1200);
        helper.assertTrue(QiService.getQiMax(player) == 100, "Qi condensed with half a reserve");
        QiService.setQi(player, 100);
        MeditationService.condense(player, 1200);
        double gained = QiService.getQiMax(player) - 100;
        // One minute in an ordinary place at Qi Sensing: 1.2 % of Qi Max, at the first realm's pace (×6).
        helper.assertTrue(Math.abs(gained - 7.2) < 0.01, "A minute of condensation should give 7.2 Qi Max, gave " + gained);
        helper.assertTrue(CultivationService.getStage(player) == CultivationStage.MIDDLE, "The first gain is a small breakthrough");
        helper.assertTrue(CultivationService.journal(player).condensedOn(player.level().getGameTime()) == 1200, "The day's condensation not counted");
        helper.assertTrue(MeditationService.stop(player, true), "Could not get up");
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_gate")
    public static void smallBreakthroughsComeByThemselvesAndTheGateOfARealmHolds(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        double gained = CultivationService.gainQiMax(player, 500, CultivationService.Source.COMMAND);
        helper.assertTrue(CultivationService.getCultivation(player).equals(new CultivationData(CultivationRealm.QI_SENSING, CultivationStage.PEAK)),
                "Not at the Peak: " + CultivationService.getCultivation(player));
        helper.assertTrue(QiService.getQiMax(player) == 250 && gained == 150, "Qi Max did not stop at the gate: " + QiService.getQiMax(player));
        helper.assertTrue(CultivationService.gainQiMax(player, 20, CultivationService.Source.COMMAND) == 0, "Qi Max grew past the gate");
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_combat")
    public static void techniquesAndKillsFeedQiMax(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        new ItemStack(MurimItems.LONG_FIST_SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        com.murimblock.combat.CombatService.setCombatMode(player, true);
        MartialService.clearCooldownsForTests(player);
        double before = QiService.getQiMax(player);
        CastResult cast = MartialService.castSlot(player, 0);
        helper.assertTrue(cast == CastResult.CAST, "Tiger Step was refused: " + cast);
        double fromTechnique = QiService.getQiMax(player) - before;
        // 5 % of the Qi spent, at the first realm's pace (×6).
        helper.assertTrue(Math.abs(fromTechnique - 3.0) < 1e-6, "10 Qi spent should give 3 Qi Max, gave " + fromTechnique);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        before = QiService.getQiMax(player);
        QiRewardManager.awardKillReward(player, zombie);
        double fromKill = QiService.getQiMax(player) - before;
        helper.assertTrue(Math.abs(fromKill - 1.2) < 1e-6, "A zombie (4 Qi) should give 1.2 Qi Max, gave " + fromKill);
        zombie.discard();
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_sensei")
    public static void slayingSenseiYunTheFirstTimeGivesThirtyQiMax(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        com.murimblock.mob.AzureSenseiEntity sensei = helper.spawn(com.murimblock.mob.MurimEntities.AZURE_SENSEI.get(), new BlockPos(2, 1, 4));
        double before = QiService.getQiMax(player);
        double share = QiRewardManager.calculateQiReward(player, sensei).finalReward() * com.murimblock.cultivation.CultivationRules.KILL_SHARE
                * com.murimblock.cultivation.CultivationRules.growth(CultivationService.getRealm(player));
        QiRewardManager.awardKillReward(player, sensei);
        double first = QiService.getQiMax(player) - before;
        // +30 for the first victory, and the usual share of the kill's Qi on top.
        helper.assertTrue(Math.abs(first - 30 - share) < 0.01, "The first victory over Sensei Yun should give 30 Qi Max and " + share + ", gave " + first);
        before = QiService.getQiMax(player);
        double again = QiRewardManager.calculateQiReward(player, sensei).finalReward() * com.murimblock.cultivation.CultivationRules.KILL_SHARE
                * com.murimblock.cultivation.CultivationRules.growth(CultivationService.getRealm(player));
        QiRewardManager.awardKillReward(player, sensei);
        helper.assertTrue(QiService.getQiMax(player) - before < again + 0.01, "A second victory gave the first victory's Qi Max again");
        sensei.discard();
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_bonuses")
    public static void eachRealmGivesItsBonusesAndBareHandsHitHarder(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        helper.assertTrue(player.getMaxHealth() == 20, "Qi Sensing has no bonus");
        CultivationService.setCultivation(player, CultivationRealm.QI_GUIDING, CultivationStage.EARLY);
        helper.assertTrue(player.getMaxHealth() == 24, "Qi Guiding should give 2 hearts: " + player.getMaxHealth());
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == 2, "Qi Guiding should give 2 armour: " + player.getAttributeValue(Attributes.ARMOR));
        // Each stage brings more: Late Qi Guiding, a heart more and a point of armour.
        CultivationService.setCultivation(player, CultivationRealm.QI_GUIDING, CultivationStage.LATE);
        helper.assertTrue(player.getMaxHealth() == 26 && player.getAttributeValue(Attributes.ARMOR) == 3,
                "Late Qi Guiding: 26 health and 3 armour, got " + player.getMaxHealth() + " and " + player.getAttributeValue(Attributes.ARMOR));
        CultivationService.setCultivation(player, CultivationRealm.QI_GUIDING, CultivationStage.EARLY);
        double bare = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(Math.abs(bare - (1 + 1) * 1.05) < 1e-6, "Bare hands at Qi Guiding: (1 + 1) x 1.05, got " + bare);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        player.doTick();
        com.murimblock.cultivation.RealmBonuses.updateFist(player);
        double sword = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(Math.abs(sword - 6 * 1.05) < 1e-6, "A sword at Qi Guiding: 6 x 1.05, got " + sword);
        CultivationService.resetCultivation(player);
        helper.assertTrue(player.getMaxHealth() == 20, "The bonuses stayed after the realm was taken away");
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_shield")
    public static void blowsFromWeakerOnesLandLighter(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        var fromZombie = player.damageSources().mobAttack(zombie);
        helper.assertTrue(com.murimblock.cultivation.RealmBonuses.shield(player, fromZombie, 10) == 10, "A beginner shrugged off a zombie's blow");
        // Two realms above a creature with no cultivation: 40 % off. Through the real event too.
        CultivationService.setCultivation(player, CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);
        float shielded = com.murimblock.cultivation.RealmBonuses.shield(player, fromZombie, 10);
        helper.assertTrue(Math.abs(shielded - 6) < 1e-4, "Qi Condensation against a zombie: 6 of 10, got " + shielded);
        var event = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(player,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(fromZombie, 10));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
        helper.assertTrue(Math.abs(event.getAmount() - 6) < 1e-4, "The incoming blow was not lightened: " + event.getAmount());
        // Nothing off a fall, nor from an equal.
        helper.assertTrue(com.murimblock.cultivation.RealmBonuses.shield(player, player.damageSources().fall(), 10) == 10, "A fall was lightened");
        zombie.discard();
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_death")
    public static void deathNeverTakesACultivatorBelowTheirStage(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        CultivationService.setCultivation(player, CultivationRealm.QI_GUIDING, CultivationStage.EARLY);
        QiService.setQiMax(player, 260);
        double lost = CultivationService.deathPenalty(player);
        helper.assertTrue(QiService.getQiMax(player) == 250 && lost == 10, "Death took Qi Max below Qi Guiding's floor: " + QiService.getQiMax(player));
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_pills")
    public static void pillsRaiseQiMaxLessAndLessThenPoison(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        var pill = MurimItems.QI_GATHERING_PILL.get();
        double[] gains = new double[4];
        for (int i = 0; i < 4; i++) {
            double before = QiService.getQiMax(player);
            pill.swallow(player);
            gains[i] = QiService.getQiMax(player) - before;
        }
        helper.assertTrue(Math.abs(gains[0] - 10) < 1e-6 && Math.abs(gains[1] - 7) < 1e-6 && Math.abs(gains[2] - 4) < 1e-6 && Math.abs(gains[3] - 1) < 1e-6,
                "Each pill should give less (10, 7, 4, 1): " + java.util.Arrays.toString(gains));
        pill.swallow(player);
        helper.assertTrue(player.hasEffect(MobEffects.POISON), "Past 100 toxicity a pill should poison");
        MurimItems.REALM_BREAKING_PILL.get().swallow(player);
        end(helper, player);
    }

    // ---------------------------------------------------------------- the inner world

    /** A cultivator seated with a full reserve. */
    private static ServerPlayer seated(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        helper.assertTrue(MeditationService.start(player), "Could not sit down");
        QiService.refillQi(player);
        return player;
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_inner_visit", timeoutTicks = 60)
    public static void theInnerWorldIsEnteredFromMeditationAndLeftBackToTheBody(GameTestHelper helper) {
        ServerPlayer player = seated(helper);
        ServerLevel home = player.serverLevel();
        BlockPos seat = player.blockPosition();

        helper.assertTrue(MeditationService.isMeditating(player) && QiService.getQi(player) == QiService.getQiMax(player), "Not seated with a full reserve");
        helper.assertTrue(InnerWorldService.enter(player), "Could not open the inner world");
        // A mock player never acknowledges its teleport: it would stay invulnerable, as one changing dimension.
        player.hasChangedDimension();
        helper.assertTrue(InnerWorldService.isInside(player), "Not in the inner world");
        List<MeditatingBodyEntity> bodies = home.getEntitiesOfClass(MeditatingBodyEntity.class, new AABB(seat).inflate(2));
        helper.assertTrue(bodies.size() == 1 && player.getUUID().equals(bodies.get(0).owner()), "No body left seated where the player meditated");
        ServerLevel inner = player.serverLevel();
        BlockPos centre = InnerWorldService.layoutOf(player).centre();
        helper.assertTrue(!inner.getBlockState(centre).isAir(), "No island under the player");
        // The island's chunks come into view on the next ticks (the player ticked as a connected one would be).
        tickFor(helper, player, 1, 20);
        helper.runAfterDelay(22, () -> {
            int souls = inner.getEntitiesOfClass(RemnantSoulEntity.class, new AABB(centre).inflate(40)).size();
            helper.assertTrue(souls == 1, "Not one remnant soul on the island: " + souls);
            helper.assertTrue(inner.getEntitiesOfClass(HeartDemonEntity.class, new AABB(centre).inflate(40)).isEmpty(), "A Heart Demon without a bottleneck");
            MeditationService.held(player);
            helper.assertTrue(player.serverLevel() == home && player.blockPosition().closerThan(seat, 1.5), "Not back where the body was");
            helper.assertTrue(MeditationService.isMeditating(player), "Not back seated in meditation");
        });
        helper.runAfterDelay(26, () -> {
            helper.assertTrue(home.getEntitiesOfClass(MeditatingBodyEntity.class, new AABB(seat).inflate(2)).stream().noneMatch(b -> b.isAlive()),
                    "The body stayed after its owner came back");
            end(helper, player);
        });
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_one_soul", timeoutTicks = 200)
    public static void aRebuiltIslandKeepsOneRemnantSoul(GameTestHelper helper) {
        ServerPlayer player = seated(helper);
        helper.assertTrue(InnerWorldService.enter(player), "Could not open the inner world");
        player.hasChangedDimension();
        tickFor(helper, player, 1, 150);
        helper.runAfterDelay(20, () -> {
            InnerWorldService.leave(player, InnerWorldService.Exit.WOKE);
            // A new realm: the island is rebuilt larger, with a new soul.
            CultivationService.setCultivation(player, CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);
            QiService.refillQi(player);
            if (!MeditationService.isMeditating(player)) MeditationService.resume(player);
            helper.assertTrue(InnerWorldService.enter(player), "Could not open the inner world again");
            player.hasChangedDimension();
        });
        helper.runAfterDelay(140, () -> {
            ServerLevel inner = player.serverLevel();
            List<RemnantSoulEntity> souls = inner.getEntitiesOfClass(RemnantSoulEntity.class, new AABB(InnerWorldService.layoutOf(player).centre()).inflate(60));
            helper.assertTrue(souls.size() == 1, "Not one remnant soul after the rebuild: " + souls.stream().map(s -> s.getUUID() + "@" + s.blockPosition() + (s.isRemoved() ? " removed" : "")).toList());
            end(helper, player);
        });
    }

    /** A cultivator at the gate of Qi Guiding (the Peak of Qi Sensing, Qi Max full), seated with a full reserve. */
    private static ServerPlayer atTheGate(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        CultivationService.gainQiMax(player, 500, CultivationService.Source.COMMAND);
        helper.assertTrue(MeditationService.start(player), "Could not sit down");
        QiService.refillQi(player);
        return player;
    }

    private static List<HeartDemonEntity> demonsOf(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(HeartDemonEntity.class, player.getBoundingBox().inflate(60),
                d -> !d.shadow() && d.isAlive() && player.getUUID().equals(d.owner()));
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_demon_won", timeoutTicks = 220)
    public static void slainHeartDemonOpensTheNextRealm(GameTestHelper helper) {
        ServerPlayer player = atTheGate(helper);
        helper.assertTrue(InnerWorldService.enter(player), "Could not open the inner world at the gate");
        // A mock player never acknowledges its teleport: it would stay invulnerable, as one changing dimension.
        player.hasChangedDimension();
        helper.assertTrue(demonsOf(player).isEmpty(), "A Heart Demon rose before the trial was accepted");
        // A mock player keeps no chunk loaded (a real one's view does): the demon's spot held for the test.
        net.minecraft.world.level.ChunkPos spot = new net.minecraft.world.level.ChunkPos(InnerWorldService.layoutOf(player).demon());
        ServerLevel inner = player.serverLevel();
        inner.setChunkForced(spot.x, spot.z, true);
        helper.runAfterDelay(InnerWorldService.TRIAL_RISE_TICKS + 84, () -> inner.setChunkForced(spot.x, spot.z, false));
        helper.assertTrue(InnerWorldService.beginTrial(player), "The trial could not begin at the gate");
        helper.assertTrue(!InnerWorldService.beginTrial(player), "The trial began twice");
        // The demon rises, then comes into view (an entity is seen a few ticks after it is added).
        tickFor(helper, player, 1, InnerWorldService.TRIAL_RISE_TICKS + 30);
        helper.runAfterDelay(InnerWorldService.TRIAL_RISE_TICKS + 32, () -> {
            List<HeartDemonEntity> demons = demonsOf(player);
            int all = 0;
            for (var e : player.serverLevel().getAllEntities()) if (e instanceof HeartDemonEntity) all++;
            helper.assertTrue(demons.size() == 1, "No Heart Demon after the trial began: " + demons.size() + " (in the level: " + all + ", underway "
                    + InnerWorldService.trialUnderway(player) + ", inside " + InnerWorldService.isInside(player) + ", time " + player.level().getGameTime() + ")");
            HeartDemonEntity demon = demons.get(0);
            // A double with all its owner's stats.
            helper.assertTrue(Math.abs(demon.getMaxHealth() - player.getMaxHealth()) < 1e-3, "The demon should have the player's health: " + demon.getMaxHealth());
            helper.assertTrue(Math.abs(demon.getAttributeValue(Attributes.ARMOR) - player.getAttributeValue(Attributes.ARMOR)) < 1e-3, "The demon should have the player's armour");
            // Its blow lands as the preparation leaves it (none here: its full weight).
            float expected = (float) (4 * InnerWorldService.session(player).strength());
            float blow = InnerWorldService.demonBlow(player, player.damageSources().mobAttack(demon), 4);
            helper.assertTrue(Math.abs(blow - expected) < 1e-3, "The demon's blow should weigh " + expected + ", weighs " + blow);
            helper.assertTrue(InnerWorldService.demonBlow(player, player.damageSources().generic(), 4) == 4, "Another blow was weighed");
            demon.setHealth(1); // worn down first: no combo fells a double from full health (BurstGuard)
            demon.hurt(player.damageSources().playerAttack(player), 1000);
            helper.assertTrue(!demon.isAlive(), "The demon did not die");
        });
        tickFor(helper, player, InnerWorldService.TRIAL_RISE_TICKS + 33, 50);
        helper.runAfterDelay(InnerWorldService.TRIAL_RISE_TICKS + 85, () -> {
            helper.assertTrue(!InnerWorldService.isInside(player), "Still in the inner world after the victory");
            helper.assertTrue(CultivationService.getRealm(player) == CultivationRealm.QI_GUIDING, "No great breakthrough: " + CultivationService.getCultivation(player));
            helper.assertTrue(player.getMaxHealth() == 24, "Qi Guiding's bonuses not given");
            end(helper, player);
        });
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_trial_dialogue", timeoutTicks = 60)
    public static void theOldMasterOffersTheTrialAndWaitingCostsNothing(GameTestHelper helper) {
        ServerPlayer player = atTheGate(helper);
        helper.assertTrue(InnerWorldService.enter(player), "Could not open the inner world at the gate");
        player.hasChangedDimension();
        tickFor(helper, player, 1, 20);
        helper.runAfterDelay(22, () -> {
            RemnantSoulEntity soul = player.serverLevel().getEntitiesOfClass(RemnantSoulEntity.class, player.getBoundingBox().inflate(60)).get(0);
            // The first meeting, then the gate: the answers offered.
            SoulDialogue.greet(player, soul);
            helper.assertTrue(key(DialogueService.last(player)).endsWith(".first"), "The first words are not the first meeting: " + key(DialogueService.last(player)));
            SoulDialogue.greet(player, soul);
            var gate = DialogueService.last(player);
            helper.assertTrue(key(gate).endsWith(".gate") && options(gate).equals(List.of("beyond", "ready", "not_yet")), "At the gate he should offer the trial: " + key(gate) + options(gate));
            SoulDialogue.choose(player, soul, "beyond");
            SoulDialogue.choose(player, soul, "myself");
            SoulDialogue.choose(player, soul, "ready");
            helper.assertTrue(options(DialogueService.last(player)).equals(List.of("summon", "not_yet")), "No warning before the trial");
            // Not yet: no demon, and leaving now costs nothing.
            SoulDialogue.choose(player, soul, "not_yet");
            helper.assertTrue(!InnerWorldService.trialUnderway(player), "Declining began the trial");
            InnerWorldService.leave(player, InnerWorldService.Exit.WOKE);
            helper.assertTrue(!player.hasEffect(CultivationRegistries.QI_DEVIATION) && CultivationService.journal(player).failures() == 0,
                    "Leaving before the trial deviated the Qi");
            // No mission at the gate: the Qi can grow no more in this realm.
            end(helper, player);
        });
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_hold")
    public static void theInnerWorldOpensOnlyAfterSevenSecondsSeated(GameTestHelper helper) {
        ServerPlayer player = seated(helper);
        // A client claiming a full hold right after sitting down is not heard.
        MeditationService.held(player);
        helper.assertTrue(!InnerWorldService.isInside(player), "The inner world opened without the seven seconds");
        helper.assertTrue(MeditationService.ENTER_HOLD_TICKS == 140, "Entering should take seven seconds");
        helper.assertTrue(InnerWorldService.ENTRY_QI_SHARE == 0.8, "Entering should need 80% of the Qi");
        end(helper, player);
    }

    private static String key(com.murimblock.network.DialoguePayloads.Page page) {
        return page != null && page.line().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "";
    }

    private static List<String> options(com.murimblock.network.DialoguePayloads.Page page) {
        return page == null ? List.of() : page.options().stream().map(com.murimblock.network.DialoguePayloads.Option::id).toList();
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_demon_lost", timeoutTicks = 60)
    public static void beatenByTheHeartDemonTheQiDeviates(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        CultivationService.gainQiMax(player, 500, CultivationService.Source.COMMAND);
        MeditationService.start(player);
        QiService.refillQi(player);
        helper.assertTrue(InnerWorldService.enter(player), "Could not open the inner world at the gate");
        // A mock player never acknowledges its teleport: it would stay invulnerable, as one changing dimension.
        player.hasChangedDimension();
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().generic(), 1000);
        helper.assertTrue(player.isAlive(), "A cultivator died in their inner world");
        tickFor(helper, player, 1, 5);
        helper.runAfterDelay(7, () -> {
            helper.assertTrue(!InnerWorldService.isInside(player), "A defeat did not throw the player out");
            helper.assertTrue(Math.abs(QiService.getQiMax(player) - 212.5) < 1e-6, "A deviation should cost 15 % of Qi Max: " + QiService.getQiMax(player));
            helper.assertTrue(QiService.getQi(player) == 0 && player.hasEffect(CultivationRegistries.QI_DEVIATION), "No deviation (reserve, effect)");
            helper.assertTrue(CultivationService.journal(player).failures() == 1, "The failure was not counted");
            helper.assertTrue(CultivationService.getRealm(player) == CultivationRealm.QI_SENSING, "A realm was lost or won");
            end(helper, player);
        });
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_body_hit", timeoutTicks = 40)
    public static void aBlowOnTheBodyTearsTheCultivatorOut(GameTestHelper helper) {
        ServerPlayer player = seated(helper);
        ServerLevel home = player.serverLevel();
        BlockPos seat = player.blockPosition();
        helper.assertTrue(InnerWorldService.enter(player), "Could not open the inner world");
        // A mock player never acknowledges its teleport: it would stay invulnerable, as one changing dimension.
        player.hasChangedDimension();
        MeditatingBodyEntity body = home.getEntitiesOfClass(MeditatingBodyEntity.class, new AABB(seat).inflate(2)).get(0);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        body.hurt(home.damageSources().mobAttack(zombie), 3);
        tickFor(helper, player, 1, 4);
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(!InnerWorldService.isInside(player), "Not torn out by the blow on the body");
            helper.assertTrue(player.hasEffect(CultivationRegistries.QI_DEVIATION), "No deviation after being torn out");
            zombie.discard();
            end(helper, player);
        });
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_missions", timeoutTicks = 40)
    public static void theRemnantSoulGivesTheDaysMissions(GameTestHelper helper) {
        ServerPlayer player = seated(helper);
        helper.assertTrue(InnerWorldService.enter(player), "Could not open the inner world");
        // A mock player never acknowledges its teleport: it would stay invulnerable, as one changing dimension.
        player.hasChangedDimension();
        ServerLevel inner = player.serverLevel();
        // The chunks round the island come into view on the next ticks (as for a player arriving anywhere).
        RemnantSoulEntity[] soul = new RemnantSoulEntity[1];
        // A mock player keeps no chunk loaded (a real one's view does): the island's held for the motes the storm scatters.
        net.minecraft.world.level.ChunkPos island = new net.minecraft.world.level.ChunkPos(InnerWorldService.layoutOf(player).centre());
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) inner.setChunkForced(island.x + dx, island.z + dz, true);
        helper.runAfterDelay(38, () -> {
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) inner.setChunkForced(island.x + dx, island.z + dz, false);
        });
        tickFor(helper, player, 1, 20);
        helper.runAfterDelay(22, () -> {
            soul[0] = near(player, RemnantSoulEntity.class).get(0);
            SoulDialogue.choose(player, soul[0], "train");
            helper.assertTrue(options(DialogueService.last(player)).contains("storm"), "The first mission offered is not the storm");
            SoulDialogue.choose(player, soul[0], "storm");
        });
        helper.runAfterDelay(26, () -> {
            List<QiMoteEntity> motes = near(player, QiMoteEntity.class);
            helper.assertTrue(motes.size() == InnerMissions.MOTES, "Not ten motes for the storm: " + motes.size());
            double before = QiService.getQiMax(player);
            for (QiMoteEntity mote : motes) {
                InnerMissions.moteGathered(player, mote);
                mote.discard();
            }
            double reward = CultivationRules.missionReward(CultivationRealm.QI_SENSING, CultivationRules.STORM_SHARE);
            helper.assertTrue(Math.abs(QiService.getQiMax(player) - before - reward) < 1e-6, "The storm's reward not given");
            helper.assertTrue(CultivationService.journal(player).missionDone("storm", inner.getGameTime()), "The storm not marked done today");
            SoulDialogue.choose(player, soul[0], "sparring");
        });
        helper.runAfterDelay(31, () -> {
            List<HeartDemonEntity> shadows = near(player, HeartDemonEntity.class);
            helper.assertTrue(shadows.size() == 1 && shadows.get(0).shadow(), "The second mission is not the shadow sparring");
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().generic(), 1000);
            helper.assertTrue(InnerWorldService.isInside(player) && player.getHealth() == player.getMaxHealth(), "Losing the sparring should cost nothing");
            end(helper, player);
        });
    }

    // ---------------------------------------------------------------- Running on water

    /**
     * Running on water: from Qi Liquidation, sprinting, a source of water under open air is solid ground for a player, at
     * its surface (not walking, not below that realm, not without Qi); each second on it costs 3 Qi.
     */
    @GameTest(template = "foundation_arena", batch = "cultivation_water")
    public static void cultivatorsRunOnWaterFromQiLiquidation(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        CultivationService.setCultivation(player, CultivationRealm.QI_LIQUIDATION, CultivationStage.EARLY);
        QiService.setQiMax(player, 1500);
        QiService.refillQi(player);
        BlockPos pool = player.blockPosition().offset(0, 3, 0);
        helper.getLevel().setBlockAndUpdate(pool, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
        player.moveTo(pool.getX() + 0.5, pool.getY() + 1.2, pool.getZ() + 0.5);
        var water = helper.getLevel().getBlockState(pool);
        java.util.function.BooleanSupplier solid = () -> !water.getCollisionShape(helper.getLevel(), pool,
                net.minecraft.world.phys.shapes.CollisionContext.of(player)).isEmpty();
        player.setSprinting(false);
        helper.assertFalse(solid.getAsBoolean(), "Water holds a walking player up");
        player.setSprinting(true);
        helper.assertTrue(solid.getAsBoolean(), "Water does not hold a sprinting player at Qi Liquidation up");
        helper.assertTrue(water.getCollisionShape(helper.getLevel(), pool, net.minecraft.world.phys.shapes.CollisionContext.of(player)).max(net.minecraft.core.Direction.Axis.Y) > 0.889,
                "The runner is held in the water, not above it");
        CultivationService.setCultivation(player, CultivationRealm.QI_CONDENSATION, CultivationStage.PEAK);
        helper.assertFalse(solid.getAsBoolean(), "Water holds Qi Condensation up");
        CultivationService.setCultivation(player, CultivationRealm.QI_LIQUIDATION, CultivationStage.EARLY);
        // On its surface, a second costs 3 Qi.
        player.moveTo(pool.getX() + 0.5, pool.getY() + 0.9, pool.getZ() + 0.5);
        player.setOnGround(true);
        double before = QiService.getQi(player);
        player.tickCount = 20;
        com.murimblock.cultivation.WaterWalking.tick(player);
        helper.assertTrue(Math.abs(before - QiService.getQi(player) - com.murimblock.cultivation.WaterWalking.QI_PER_SECOND) < 1e-6, "A second on water did not cost 3 Qi");
        QiService.setQi(player, 0);
        player.moveTo(pool.getX() + 0.5, pool.getY() + 1.2, pool.getZ() + 0.5);
        helper.assertFalse(solid.getAsBoolean(), "Water holds a player without Qi up");
        helper.getLevel().setBlockAndUpdate(pool, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        end(helper, player);
    }

    // ---------------------------------------------------------------- Aura pressure

    /**
     * The aura's pressure: at Foundation Establishment a zombie near is crushed to the ground (far weaker); not at Qi
     * Crystallization, not while sneaking, never one's own wolf.
     */
    @GameTest(template = "foundation_arena", batch = "cultivation_pressure")
    public static void theAurasPressureSlowsTheWeakerNear(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        var wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, new BlockPos(1, 2, 3));
        wolf.tame(player);
        player.tickCount = 10;
        CultivationService.setCultivation(player, CultivationRealm.QI_CRYSTALLIZATION, CultivationStage.PEAK);
        com.murimblock.cultivation.AuraPressure.tick(player);
        helper.assertFalse(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Qi Crystallization weighs on the zombie");
        CultivationService.setCultivation(player, CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationStage.EARLY);
        player.setShiftKeyDown(true);
        com.murimblock.cultivation.AuraPressure.tick(player);
        helper.assertFalse(zombie.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "A sneaking player's aura weighs on the zombie");
        player.setShiftKeyDown(false);
        com.murimblock.cultivation.AuraPressure.tick(player);
        var slow = zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        helper.assertTrue(slow != null && slow.getAmplifier() == 3, "The zombie, far weaker, is not crushed to the ground");
        helper.assertFalse(wolf.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "One's own wolf is slowed");
        end(helper, player);
    }

    // ---------------------------------------------------------------- Methods

    /**
     * Methods: Ordinary Breath from the start; an unknown method cannot be practised; a scroll's method once learned can,
     * and the change puts the Qi out of tune (meditation halved); Frost Lotus Heart slows a melee attacker, once in a while.
     */
    @GameTest(template = "foundation_arena", batch = "cultivation_methods")
    public static void methodsAreLearnedPractisedAndPutTheQiOutOfTune(GameTestHelper helper) {
        com.murimblock.cultivation.MethodService.clearForTests();
        ServerPlayer player = cultivator(helper);
        CultivationService.setCultivation(player, CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);
        var frost = com.murimblock.cultivation.CultivationMethod.FROST_LOTUS_HEART;
        helper.assertTrue(com.murimblock.cultivation.MethodService.method(player) == com.murimblock.cultivation.CultivationMethod.ORDINARY_BREATH,
                "Ordinary Breath is not practised from the start");
        helper.assertTrue(com.murimblock.cultivation.MethodService.factor(player) == 1, "Ordinary Breath does not cultivate x1");
        helper.assertTrue(com.murimblock.cultivation.MethodService.practise(player, frost) == com.murimblock.cultivation.MethodService.Change.UNKNOWN,
                "An unknown method was practised");
        ItemStack scroll = new ItemStack(MurimItems.FROST_LOTUS_HEART_SCROLL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, scroll);
        scroll.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(com.murimblock.cultivation.MethodService.data(player).knows(frost), "The scroll did not teach Frost Lotus Heart");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "The scroll was not used up");
        helper.assertTrue(com.murimblock.cultivation.MethodService.practise(player, frost) == com.murimblock.cultivation.MethodService.Change.CHANGED,
                "A known method could not be practised");
        double expected = frost.factor(player) * com.murimblock.cultivation.MethodService.DISCORD_FACTOR;
        helper.assertTrue(Math.abs(com.murimblock.cultivation.MethodService.factor(player) - expected) < 1e-9, "The Qi is not out of tune after a change");
        // Struck in melee: the attacker is slowed; struck again at once, not (once every 8 s).
        Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        player.hurt(player.damageSources().mobAttack(attacker), 1);
        helper.assertTrue(attacker.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Frost Lotus Heart did not slow the attacker");
        attacker.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().mobAttack(attacker), 1);
        helper.assertFalse(attacker.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Frost Lotus Heart slowed twice in a row");
        com.murimblock.cultivation.MethodService.clearForTests();
        end(helper, player);
    }

    // ---------------------------------------------------------------- Qi Sense

    /**
     * Qi Sense's cost grows with its reach (2 Qi a second at Qi Condensation, 8 at Core Formation); held 40 s it closes
     * by itself, and cannot open again while the eyes rest (10 s).
     */
    @GameTest(template = "foundation_arena", batch = "cultivation_tired")
    public static void qiSenseCostsMoreFurtherAndTiresTheEyes(GameTestHelper helper) {
        QiSense.clearForTests();
        ServerPlayer reader = cultivator(helper);
        helper.assertTrue(CultivationRules.senseQiPerSecond(CultivationRealm.QI_CONDENSATION) == 2
                && CultivationRules.senseQiPerSecond(CultivationRealm.CORE_FORMATION) == 8, "Qi Sense's cost does not follow its reach");
        CultivationService.setCultivation(reader, CultivationRealm.CORE_FORMATION, CultivationStage.EARLY);
        QiService.setQiMax(reader, 5000);
        QiService.refillQi(reader);
        QiSense.setActive(reader, true);
        helper.assertTrue(QiSense.isActive(reader), "Qi Sense does not open");
        QiSense.backdateForTests(reader, CultivationRules.SENSE_HOLD_MAX - 5);
        QiSense.tick(reader);
        helper.assertTrue(QiSense.isActive(reader), "Qi Sense closed before 40 s");
        QiSense.backdateForTests(reader, 5);
        QiSense.tick(reader);
        helper.assertFalse(QiSense.isActive(reader), "Qi Sense still open after 40 s");
        QiSense.setActive(reader, true);
        helper.assertFalse(QiSense.isActive(reader), "Qi Sense opened again while the eyes rest");
        QiSense.clearForTests();
        end(helper, reader);
    }

    /** Hidden caches: a buried chest with something in it is felt; a buried empty one is not, nor a full one in sight. */
    @GameTest(template = "foundation_arena", batch = "cultivation_caches")
    public static void hiddenCachesWithSomethingInsideAreFelt(GameTestHelper helper) {
        ServerPlayer reader = cultivator(helper);
        CultivationService.setCultivation(reader, CultivationRealm.NASCENT_SOUL, CultivationStage.EARLY);
        BlockPos at = reader.blockPosition();
        BlockPos full = at.offset(4, -4, 0), empty = at.offset(-4, -4, 0), seen = at.offset(0, 0, 3);
        ServerLevel level = helper.getLevel();
        for (BlockPos b : BlockPos.betweenClosed(at.offset(-6, -6, -2), at.offset(6, -1, 2))) level.setBlockAndUpdate(b, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        for (BlockPos b : List.of(full, empty, seen)) level.setBlockAndUpdate(b, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        ((net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(full)).setItem(0, new ItemStack(Items.DIAMOND));
        ((net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(seen)).setItem(0, new ItemStack(Items.DIAMOND));
        List<BlockPos> caches = com.murimblock.cultivation.QiTraces.caches(reader);
        helper.assertTrue(caches.stream().anyMatch(c -> c.getX() == full.getX() && c.getZ() == full.getZ()), "The buried chest with a diamond is not felt");
        helper.assertTrue(caches.stream().noneMatch(c -> c.getX() == empty.getX() && c.getZ() == empty.getZ()), "The buried empty chest is felt");
        helper.assertTrue(caches.stream().noneMatch(c -> c.getX() == seen.getX() && c.getZ() == seen.getZ()), "The chest in sight is felt");
        for (BlockPos b : BlockPos.betweenClosed(at.offset(-6, -6, -2), at.offset(6, -1, 2))) level.setBlockAndUpdate(b, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(seen, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        end(helper, reader);
    }

    /**
     * Hidden traces: a cultivator from Qi Condensation leaves a point of trail each stride, a weaker one none; a reader at
     * Nascent Soul is sent the others' (in their art, one walker), never their own.
     */
    @GameTest(template = "foundation_arena", batch = "cultivation_traces")
    public static void thePowerfulLeaveTracesSeenFromNascentSoul(GameTestHelper helper) {
        com.murimblock.cultivation.QiTraces.clearForTests();
        ServerPlayer reader = cultivator(helper), strong = player(helper), weak = player(helper);
        CultivationService.setCultivation(reader, CultivationRealm.NASCENT_SOUL, CultivationStage.EARLY);
        CultivationService.setCultivation(strong, CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);
        CultivationService.setCultivation(weak, CultivationRealm.QI_GUIDING, CultivationStage.PEAK);
        helper.assertTrue(com.murimblock.cultivation.QiTraces.leavesTraces(strong), "Qi Condensation leaves no traces");
        helper.assertFalse(com.murimblock.cultivation.QiTraces.leavesTraces(weak), "Qi Guiding leaves traces");
        long now = helper.getLevel().getGameTime();
        double x = reader.getX(), y = reader.getY(), z = reader.getZ();
        for (int i = 0; i < 4; i++) {
            strong.moveTo(x + 2, y, z + i * 8.5);
            com.murimblock.cultivation.QiTraces.step(strong, now);
            // Less than a stride: no new footprint.
            strong.moveTo(x + 2, y, z + i * 8.5 + 3.5);
            com.murimblock.cultivation.QiTraces.step(strong, now);
        }
        com.murimblock.cultivation.QiTraces.step(reader, now);
        var seen = com.murimblock.cultivation.QiTraces.seen(reader);
        helper.assertTrue(seen.size() == 4, "The reader sees " + seen.size() + " points of trail, not 4");
        helper.assertTrue(seen.stream().allMatch(t -> t.art().equals("none") && t.walker() == 0), "The trail is not the walker's");
        com.murimblock.cultivation.QiTraces.clearForTests();
        end(helper, reader, strong, weak);
    }

    /**
     * Through walls: a weaker aura 8 blocks off behind a stone wall is not read at Foundation Establishment, and is from
     * Core Formation (within 24 blocks); 30 blocks off behind it, still not.
     */
    @GameTest(template = "foundation_arena", batch = "cultivation_walls")
    public static void qiSenseReadsThroughWallsFromCoreFormation(GameTestHelper helper) {
        ServerPlayer reader = cultivator(helper), hidden = player(helper);
        CultivationService.setCultivation(hidden, CultivationRealm.QI_SENSING, CultivationStage.LATE);
        double x = reader.getX(), y = reader.getY(), z = reader.getZ();
        hidden.moveTo(x, y, z + 8);
        BlockPos base = BlockPos.containing(x, y, z + 4);
        for (int dx = -3; dx <= 3; dx++) for (int dy = -2; dy <= 4; dy++)
            helper.getLevel().setBlockAndUpdate(base.offset(dx, dy, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        helper.assertFalse(reader.hasLineOfSight(hidden), "The wall does not hide the other player");
        CultivationService.setCultivation(reader, CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationStage.PEAK);
        helper.assertTrue(QiSense.readings(reader).stream().noneMatch(r -> r.entityId() == hidden.getId()), "Read through a wall at Foundation Establishment");
        CultivationService.setCultivation(reader, CultivationRealm.CORE_FORMATION, CultivationStage.EARLY);
        helper.assertTrue(QiSense.readings(reader).stream().anyMatch(r -> r.entityId() == hidden.getId()), "Not read through a wall at Core Formation");
        hidden.moveTo(x, y, z + 30);
        helper.assertTrue(QiSense.readings(reader).stream().noneMatch(r -> r.entityId() == hidden.getId()), "Read through walls beyond 24 blocks");
        for (int dx = -3; dx <= 3; dx++) for (int dy = -2; dy <= 4; dy++)
            helper.getLevel().setBlockAndUpdate(base.offset(dx, dy, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        end(helper, reader, hidden);
    }

    /**
     * Vital points: a perfect parry at Foundation Establishment, Qi Sense open, lights some of the attacker's points (not
     * below that realm); a blow aimed at one does 1.5x and puts it out, a blow aimed elsewhere does not.
     */
    @GameTest(template = "foundation_arena", batch = "cultivation_vital")
    public static void vitalPointsLightAfterAParryAndAStrikeOnOneHurtsMore(GameTestHelper helper) {
        ServerPlayer reader = cultivator(helper);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        target.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, ItemStack.EMPTY);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        CultivationService.setCultivation(reader, CultivationRealm.QI_CRYSTALLIZATION, CultivationStage.PEAK);
        QiSense.setActive(reader, true);
        com.murimblock.cultivation.VitalPoints.onPerfectParry(reader, target);
        helper.assertTrue(com.murimblock.cultivation.VitalPoints.open(reader, target) == 0, "Vital points light up below Foundation Establishment");
        CultivationService.setCultivation(reader, CultivationRealm.FOUNDATION_ESTABLISHMENT, CultivationStage.EARLY);
        QiSense.setActive(reader, true);
        com.murimblock.cultivation.VitalPoints.onPerfectParry(reader, target);
        int mask = com.murimblock.cultivation.VitalPoints.open(reader, target);
        helper.assertTrue(Integer.bitCount(mask) == com.murimblock.cultivation.VitalPoints.COUNT, "The parry lit " + Integer.bitCount(mask) + " points");
        int point = Integer.numberOfTrailingZeros(mask);
        reader.moveTo(target.getX(), target.getY(), target.getZ() - 2.5, 0, 0);
        target.setYRot(180);
        target.setYBodyRot(180);
        reader.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, com.murimblock.cultivation.VitalPoints.position(target, point));
        float before = target.getHealth();
        target.hurt(reader.damageSources().playerAttack(reader), 4);
        float struck = before - target.getHealth();
        helper.assertTrue(struck > 5.9F, "A blow on a vital point did " + struck + ", not 6");
        helper.assertTrue((com.murimblock.cultivation.VitalPoints.open(reader, target) & 1 << point) == 0, "The point struck is still lit");
        // Aimed at the sky: an ordinary blow.
        target.invulnerableTime = 0;
        reader.setXRot(-80);
        before = target.getHealth();
        target.hurt(reader.damageSources().playerAttack(reader), 4);
        helper.assertTrue(before - target.getHealth() < 4.1F, "A blow aimed elsewhere was made stronger");
        com.murimblock.cultivation.VitalPoints.clearForTests();
        end(helper, reader);
    }

    /**
     * Beyond the sense's reach (16 blocks at Qi Condensation), only the powerful are felt, through walls, out to its far
     * reach (48): one as strong or stronger is read, a weaker one is not.
     */
    @GameTest(template = "foundation_arena", batch = "cultivation_sense")
    public static void qiSenseFeelsThePowerfulFromFarAndThroughWalls(GameTestHelper helper) {
        ServerPlayer reader = cultivator(helper);
        ServerPlayer strong = player(helper), weak = player(helper);
        CultivationService.setCultivation(reader, CultivationRealm.QI_CONDENSATION, CultivationStage.MIDDLE);
        CultivationService.setCultivation(strong, CultivationRealm.QI_LIQUIDATION, CultivationStage.EARLY);
        CultivationService.setCultivation(weak, CultivationRealm.QI_SENSING, CultivationStage.LATE);
        double x = reader.getX(), y = reader.getY(), z = reader.getZ();
        helper.assertTrue(CultivationRules.senseRange(CultivationRealm.QI_CONDENSATION) == 16 && CultivationRules.farSenseRange(CultivationRealm.QI_CONDENSATION) == 48,
                "Qi Condensation's reaches are not 16 and 48");
        strong.moveTo(x + 30, y, z);
        weak.moveTo(x - 30, y, z);
        var readings = QiSense.readings(reader);
        helper.assertTrue(readings.stream().anyMatch(r -> r.entityId() == strong.getId()), "The stronger one, 30 blocks off, is not felt");
        helper.assertTrue(readings.stream().noneMatch(r -> r.entityId() == weak.getId()), "The weaker one, 30 blocks off, is felt");
        // A far stronger cultivator reaches much further.
        helper.assertTrue(CultivationRules.farSenseRange(CultivationRealm.CORE_FORMATION) > CultivationRules.farSenseRange(CultivationRealm.QI_LIQUIDATION),
                "A higher realm does not feel further");
        end(helper, reader, strong, weak);
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_sense")
    public static void qiSenseShowsEachReaderOnlyWhatTheirRealmAllows(GameTestHelper helper) {
        ServerPlayer reader = cultivator(helper);
        ServerPlayer weak = player(helper), equal = player(helper), above = player(helper), far = player(helper);
        CultivationService.setCultivation(reader, CultivationRealm.QI_CONDENSATION, CultivationStage.MIDDLE);
        CultivationService.setCultivation(weak, CultivationRealm.QI_SENSING, CultivationStage.LATE);
        CultivationService.setCultivation(equal, CultivationRealm.QI_CONDENSATION, CultivationStage.PEAK);
        CultivationService.setCultivation(above, CultivationRealm.QI_LIQUIDATION, CultivationStage.EARLY);
        CultivationService.setCultivation(far, CultivationRealm.CORE_FORMATION, CultivationStage.EARLY);
        double x = reader.getX(), y = reader.getY(), z = reader.getZ();
        weak.moveTo(x + 2, y, z);
        equal.moveTo(x - 2, y, z);
        above.moveTo(x, y, z + 2);
        far.moveTo(x, y, z - 2);
        var readings = QiSense.readings(reader);
        java.util.function.Function<ServerPlayer, CultivationPayloads.Reading> of = p -> readings.stream().filter(r -> r.entityId() == p.getId()).findFirst().orElse(null);
        helper.assertTrue(of.apply(weak) != null && of.apply(weak).kind() == CultivationRules.Reading.CLEAR.ordinal()
                && of.apply(weak).realm() == 0 && of.apply(weak).stage() == 2, "A weaker one should be read whole: " + of.apply(weak));
        helper.assertTrue(of.apply(equal).kind() == CultivationRules.Reading.FUZZY.ordinal() && of.apply(equal).stage() == -1, "The same realm, the stage hidden");
        helper.assertTrue(of.apply(above).kind() == CultivationRules.Reading.VEILED.ordinal() && of.apply(above).realm() == -1, "One realm above must stay veiled");
        helper.assertTrue(of.apply(far).kind() == CultivationRules.Reading.ABYSS.ordinal() && of.apply(far).realm() == -1, "Far above must be unreadable");
        // Below Qi Condensation, the sense does not open.
        CultivationService.setCultivation(reader, CultivationRealm.QI_GUIDING, CultivationStage.PEAK);
        QiSense.setActive(reader, true);
        helper.assertTrue(!QiSense.isActive(reader), "Qi Sense opened before Qi Condensation");
        end(helper, reader, weak, equal, above, far);
    }
}
