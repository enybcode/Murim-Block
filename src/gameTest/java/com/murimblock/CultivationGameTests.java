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
        // One minute in an ordinary place at Qi Sensing: 1.2 % of Qi Max.
        helper.assertTrue(Math.abs(gained - 1.2) < 0.01, "A minute of condensation should give 1.2 Qi Max, gave " + gained);
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
        helper.assertTrue(Math.abs(fromTechnique - 0.5) < 1e-6, "10 Qi spent should give 0.5 Qi Max, gave " + fromTechnique);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        before = QiService.getQiMax(player);
        QiRewardManager.awardKillReward(player, zombie);
        double fromKill = QiService.getQiMax(player) - before;
        helper.assertTrue(Math.abs(fromKill - 0.2) < 1e-6, "A zombie (4 Qi) should give 0.2 Qi Max, gave " + fromKill);
        zombie.discard();
        end(helper, player);
    }

    @GameTest(template = "foundation_arena", batch = "cultivation_bonuses")
    public static void eachRealmGivesItsBonusesAndBareHandsHitHarder(GameTestHelper helper) {
        ServerPlayer player = cultivator(helper);
        helper.assertTrue(player.getMaxHealth() == 20, "Qi Sensing has no bonus");
        CultivationService.setCultivation(player, CultivationRealm.QI_GUIDING, CultivationStage.EARLY);
        helper.assertTrue(player.getMaxHealth() == 24, "Qi Guiding should give 2 hearts: " + player.getMaxHealth());
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
    public static void theInnerWorldOpensOnlyAfterFifteenSecondsSeated(GameTestHelper helper) {
        ServerPlayer player = seated(helper);
        // A client claiming a full hold right after sitting down is not heard.
        MeditationService.held(player);
        helper.assertTrue(!InnerWorldService.isInside(player), "The inner world opened without the fifteen seconds");
        helper.assertTrue(MeditationService.ENTER_HOLD_TICKS == 300, "Entering should take fifteen seconds");
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
        tickFor(helper, player, 1, 20);
        helper.runAfterDelay(22, () -> {
            soul[0] = inner.getEntitiesOfClass(RemnantSoulEntity.class, player.getBoundingBox().inflate(40)).get(0);
            SoulDialogue.choose(player, soul[0], "train");
            helper.assertTrue(options(DialogueService.last(player)).contains("storm"), "The first mission offered is not the storm");
            SoulDialogue.choose(player, soul[0], "storm");
        });
        helper.runAfterDelay(26, () -> {
            List<QiMoteEntity> motes = inner.getEntitiesOfClass(QiMoteEntity.class, player.getBoundingBox().inflate(40));
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
            List<HeartDemonEntity> shadows = inner.getEntitiesOfClass(HeartDemonEntity.class, player.getBoundingBox().inflate(40));
            helper.assertTrue(shadows.size() == 1 && shadows.get(0).shadow(), "The second mission is not the shadow sparring");
            player.invulnerableTime = 0;
            player.hurt(player.damageSources().generic(), 1000);
            helper.assertTrue(InnerWorldService.isInside(player) && player.getHealth() == player.getMaxHealth(), "Losing the sparring should cost nothing");
            end(helper, player);
        });
    }

    // ---------------------------------------------------------------- Qi Sense

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
