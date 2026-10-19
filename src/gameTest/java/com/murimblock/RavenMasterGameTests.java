package com.murimblock;

import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationRules;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.integration.epicfight.MurimAnimations;
import com.murimblock.integration.epicfight.MurimWeaponCategories;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.MurimEntities;
import com.murimblock.mob.RavenMasterEntity;
import com.murimblock.mob.RavenMasterSightings;
import com.murimblock.testing.LootRolls;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Raven Master: he comes with the iron fan in hand and stands in his own motions; he leaves alone a player who does
 * not meet his eyes, and fights one who does (his boss bar shows then, not before); a fleeting one is gone when
 * approached; his swarm pecks; beaten, he always leaves the Raven Art's scroll; he shows himself only from Qi
 * Condensation, half as often once beaten, and his first defeat gives Qi Max.
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RavenMasterGameTests {
    private RavenMasterGameTests() { }

    private static RavenMasterEntity master(GameTestHelper helper, BlockPos at) {
        RavenMasterEntity master = MurimEntities.RAVEN_MASTER.get().spawn(helper.getLevel(), helper.absolutePos(at), MobSpawnType.COMMAND);
        helper.assertTrue(master != null, "No Raven Master spawned");
        return master;
    }

    /** The mock player stands at `at`, facing yaw (0: south, toward +z; 180: north), looking pitch down. */
    private static ServerPlayer playerAt(GameTestHelper helper, BlockPos at, float yaw, float pitch) {
        ServerPlayer player = player(helper);
        BlockPos pos = helper.absolutePos(at);
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, pitch);
        // A living thing looks where its head turns: the head with the body.
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        return player;
    }

    @GameTest(template = "foundation_arena", batch = "raven_master_fan")
    public static void theRavenMasterHoldsTheIronFanInHisOwnMotions(GameTestHelper helper) {
        RavenMasterEntity master = master(helper, new BlockPos(2, 1, 2));
        ItemStack held = master.getItemBySlot(EquipmentSlot.MAINHAND);
        helper.assertTrue(held.is(MurimItems.IRON_FAN.get()), "The Raven Master does not hold the iron fan: " + held);
        helper.assertTrue(EpicFightCapabilities.getItemStackCapability(held).getWeaponCategory() == MurimWeaponCategories.IRON_FAN,
                "Epic Fight does not take his fan for an iron fan");
        helper.runAfterDelay(3, () -> {
            LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(master, LivingEntityPatch.class);
            helper.assertTrue(patch != null, "No Epic Fight patch on the Raven Master");
            var idle = patch.getAnimator().getLivingAnimation(LivingMotions.IDLE, null);
            var walk = patch.getAnimator().getLivingAnimation(LivingMotions.WALK, null);
            helper.assertTrue(idle == MurimAnimations.RAVEN_MASTER_IDLE, "With his fan, he should stand in his own idle, not " + idle);
            helper.assertTrue(walk == MurimAnimations.RAVEN_MASTER_WALK, "With his fan, he should walk in his own stride, not " + walk);
            helper.assertTrue(master.getMaxHealth() >= 300, "The Raven Master is no hard boss: " + master.getMaxHealth() + " health");
            master.discard();
            helper.succeed();
        });
    }

    /** A player near him who looks elsewhere is left alone: he only watches, and shows no boss bar. */
    @GameTest(template = "streak_arena", batch = "raven_master_unseen", timeoutTicks = 100)
    public static void theRavenMasterLeavesAloneWhoDoesNotMeetHisEyes(GameTestHelper helper) {
        RavenMasterEntity master = master(helper, new BlockPos(8, 1, 12));
        ServerPlayer player = playerAt(helper, new BlockPos(8, 1, 3), 180, 0);
        master.startSeenByPlayer(player);
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(!master.hunting() && master.getTarget() == null, "He turned on a player who never met his eyes: " + master.getTarget());
            helper.assertTrue(!master.staredAtBy(player), "A player looking the other way stares at him");
            helper.assertTrue(!master.showsBossBarTo(player), "His boss bar shows while he only watches");
            master.discard();
            cleanup(player);
            helper.succeed();
        });
    }

    /** Looked in the eyes, he bursts into ravens, comes out beside the player and fights them; his boss bar shows. */
    @GameTest(template = "streak_arena", batch = "raven_master_stare", timeoutTicks = 100)
    public static void meetingHisEyesMakesHimFight(GameTestHelper helper) {
        RavenMasterEntity master = master(helper, new BlockPos(8, 1, 13));
        ServerPlayer player = playerAt(helper, new BlockPos(8, 1, 2), 0, 60);
        master.startSeenByPlayer(player);
        double before = master.distanceTo(player);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(!master.hunting(), "He fights before anyone met his eyes (the player looks at his feet)");
            player.lookAt(EntityAnchorArgument.Anchor.EYES, master.getEyePosition());
            helper.assertTrue(master.staredAtBy(player), "A player looking at his face does not stare at him");
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(master.hunting() && master.getTarget() == player, "Met in the eyes, he does not fight the player: " + master.mood() + ", " + master.getTarget());
                helper.assertTrue(master.distanceTo(player) < before - 3, "He did not come out beside the player: " + master.distanceTo(player) + " blocks (was " + before + ")");
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(master.showsBossBarTo(player), "Fighting, his boss bar does not show");
                    helper.assertTrue(master.isAggressive(), "Fighting, he should show it (his face)");
                    master.discard();
                    cleanup(player);
                    helper.succeed();
                });
            });
        });
    }

    /** Brought by the sightings, he is gone when the watched player comes within 20 blocks without meeting his eyes. */
    @GameTest(template = "streak_arena", batch = "raven_master_approach", timeoutTicks = 60)
    public static void aFleetingRavenMasterIsGoneWhenApproached(GameTestHelper helper) {
        ServerPlayer player = playerAt(helper, new BlockPos(8, 1, 2), 180, 0);
        RavenMasterEntity master = MurimEntities.RAVEN_MASTER.get().create(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(8, 1, 12));
        master.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
        master.watch(player, 600);
        helper.getLevel().addFreshEntity(master);
        helper.assertTrue(master.fleeting() && !master.shouldBeSaved(), "A fleeting Raven Master would be saved");
        helper.succeedWhen(() -> {
            helper.assertTrue(!master.hunting(), "Approached without meeting his eyes, he fights");
            helper.assertTrue(master.isRemoved(), "Approached within 20 blocks, the fleeting Raven Master is still there");
            cleanup(player);
        });
    }

    /** His raven swarm flies at his foe and pecks: each peck a blow from him. */
    @GameTest(template = "streak_arena", batch = "raven_master_swarm", timeoutTicks = 160)
    public static void hisRavenSwarmPecks(GameTestHelper helper) {
        RavenMasterEntity master = master(helper, new BlockPos(8, 1, 3));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, 1, 10));
        target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.setHealth(200);
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        master.engage(target);
        master.startSwarm();
        boolean[] flew = {false};
        helper.onEachTick(() -> flew[0] |= master.swarming());
        helper.succeedWhen(() -> {
            helper.assertTrue(flew[0], "His swarm has not taken off");
            helper.assertTrue(master.pecksLanded() > 0 && target.getHealth() < 200, "His swarm has not pecked yet");
            master.discard();
            target.discard();
        });
    }

    /** Killed by a player, he always leaves the Raven Art's scroll and some silver; otherwise nothing. */
    @GameTest(template = "foundation_arena", batch = "raven_master_loot")
    public static void theRavenMasterAlwaysLeavesTheRavenArtsScroll(GameTestHelper helper) {
        RavenMasterEntity master = master(helper, new BlockPos(2, 1, 2));
        ServerPlayer player = player(helper);
        try {
            var byPlayer = LootRolls.roll(helper.getLevel(), master, player, 500);
            helper.assertTrue(byPlayer.rollsWith(MurimItems.RAVEN_ART_SCROLL.get()) == 500, "The scroll should always drop, not " + byPlayer.rollsWith(MurimItems.RAVEN_ART_SCROLL.get()) + " in 500");
            helper.assertTrue(byPlayer.mostInOneRoll(MurimItems.RAVEN_ART_SCROLL.get()) == 1, "More than one scroll at once");
            helper.assertTrue(byPlayer.onlyContains(MurimItems.RAVEN_ART_SCROLL.get(), MurimItems.SILVER_TAEL.get()), "Something else dropped: " + byPlayer.rollsWith());
            var byNothing = LootRolls.roll(helper.getLevel(), master, null, 20);
            helper.assertTrue(byNothing.rollsWith().isEmpty(), "Something dropped without a player: " + byNothing.rollsWith());
        } finally {
            master.discard();
            cleanup(player);
        }
        helper.succeed();
    }

    /** He shows himself only from Qi Condensation; once beaten, half as often; his first defeat gives Qi Max. */
    @GameTest(template = "foundation_arena", batch = "raven_master_sightings")
    public static void theRavenMasterShowsHimselfFromQiCondensation(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            CultivationService.resetCultivation(player);
            helper.assertTrue(!RavenMasterSightings.realmAllows(player), "He shows himself to a player below Qi Condensation");
            helper.assertTrue(!RavenMasterSightings.eligible(player), "A player below Qi Condensation may see him");
            CultivationService.setCultivation(player, CultivationRealm.QI_GUIDING, CultivationStage.PEAK);
            helper.assertTrue(!RavenMasterSightings.realmAllows(player), "He shows himself to a player in Qi Guiding");
            CultivationService.setCultivation(player, CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);
            helper.assertTrue(RavenMasterSightings.realmAllows(player), "He does not show himself to a player in Qi Condensation");
            var random = helper.getLevel().getRandom();
            for (int i = 0; i < 50; i++) {
                int ticks = RavenMasterSightings.interval(player, random);
                helper.assertTrue(ticks >= 9000 && ticks <= 15000, "About ten minutes between his sightings, not " + ticks + " ticks");
            }
            player.getPersistentData().putBoolean("murimblock.raven_master.beaten", true);
            for (int i = 0; i < 50; i++) {
                int ticks = RavenMasterSightings.interval(player, random);
                helper.assertTrue(ticks >= 18000 && ticks <= 30000, "Once beaten, about twenty minutes between his sightings, not " + ticks + " ticks");
            }
            helper.assertTrue(CultivationRules.firstVictoryQiMax("murimblock:raven_master") > 0, "The first victory over him gives no Qi Max");
            helper.assertTrue(!RavenMasterSightings.abroad(helper.getLevel()), "A Raven Master is out with none spawned");
            RavenMasterEntity master = master(helper, new BlockPos(2, 1, 2));
            helper.assertTrue(RavenMasterSightings.abroad(helper.getLevel()), "The Raven Master out is not counted (he is unique)");
            master.discard();
        } finally {
            cleanup(player);
        }
        helper.succeed();
    }

    /** Fighting, he goes through a shadow door and comes out behind his foe. */
    @GameTest(template = "streak_arena", batch = "raven_master_door", timeoutTicks = 60)
    public static void hisShadowDoorSetsHimBehindHisFoe(GameTestHelper helper) {
        RavenMasterEntity master = master(helper, new BlockPos(8, 1, 13));
        Zombie target = helper.spawn(EntityType.ZOMBIE, new BlockPos(8, 1, 5));
        target.setNoAi(true);
        target.setYRot(0);
        target.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        master.engage(target);
        helper.assertTrue(master.shadowStep(helper.getLevel(), target), "No shadow door opened");
        Vec3 behind = target.position().add(Vec3.directionFromRotation(0, target.getYRot()).scale(-2.2));
        helper.assertTrue(master.position().distanceTo(behind) < 1.2, "He did not come out behind his foe: " + master.position() + " for " + behind);
        master.discard();
        target.discard();
        helper.succeed();
    }
}
