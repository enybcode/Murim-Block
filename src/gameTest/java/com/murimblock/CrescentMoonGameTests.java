package com.murimblock;

import com.murimblock.cultivation.CultivationRealm;
import com.murimblock.cultivation.CultivationService;
import com.murimblock.cultivation.CultivationStage;
import com.murimblock.integration.epicfight.EpicFightBridge;
import com.murimblock.integration.epicfight.EpicFightCombatDefaults;
import com.murimblock.item.MurimItems;
import com.murimblock.martial.CrescentMoonHalberd;
import com.murimblock.martial.MartialArts;
import com.murimblock.martial.MartialCombat;
import com.murimblock.martial.MartialData;
import com.murimblock.martial.MartialService;
import com.murimblock.martial.MartialService.CastResult;
import com.murimblock.martial.MoonQiEntity;
import com.murimblock.qi.QiService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.gameasset.Animations;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/**
 * The Crescent Moon Halberd (docs/combat/styles/crescent-moon-halberd.md, Enzo's choice of 19/10/2026): its scroll teaches
 * from Qi Condensation; Waxing Moon fills a phase a landed strike and, full, sends a crescent off the next; each technique
 * does what it should (the ring that slows, the hook that pulls or trips, the fan of three crescents, the mirror that
 * stops what comes from the front and sends it back, the full moon's beams on the enemies round).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrescentMoonGameTests {
    private CrescentMoonGameTests() { }

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);
    private static final MartialArts.Technique[] ALL = {MartialArts.MOONLIT_CIRCLE, MartialArts.TIDE_HOOK, MartialArts.FALLING_CRESCENT,
            MartialArts.MIRROR_LAKE_STANCE, MartialArts.NIGHT_OF_THE_FULL_MOON};

    /** A student of the art, every technique on the bar (slots 0 to 4), the halberd in hand, on stone, full Qi, facing south. */
    private static ServerPlayer adept(GameTestHelper helper) {
        return adept(helper, CENTRE);
    }

    /** At the near edge of streak_arena (16 by 16), facing south across it: room for the crescents and the hook. */
    private static ServerPlayer atTheEdge(GameTestHelper helper) {
        return adept(helper, new BlockPos(8, 1, 1));
    }

    private static ServerPlayer adept(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = player(helper);
        EpicFightCombatDefaults.enforce(player);
        CultivationService.setCultivation(player, MartialArts.CRESCENT_MOON_REALM, CultivationStage.EARLY);
        new ItemStack(MurimItems.CRESCENT_MOON_HALBERD_SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        MartialData data = MartialService.data(player);
        for (int i = 0; i < ALL.length; i++) data = data.assign(i, ALL[i].id());
        MartialService.setForTests(player, data);
        MartialService.setMainArt(player, MartialArts.CRESCENT_MOON_HALBERD);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MurimItems.CRESCENT_MOON_HALBERD.get()));
        BlockPos p = helper.absolutePos(at);
        player.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        player.setOnGround(true);
        MartialService.clearCooldownsForTests(player);
        QiService.setQiMax(player, 2000);
        QiService.setQi(player, 2000);
        return player;
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz, float health) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, helper.relativePos(player.blockPosition()));
        zombie.setNoAi(true);
        zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        zombie.setHealth(health);
        zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        zombie.moveTo(player.getX() + dx, player.getY(), player.getZ() + dz, 180, 0);
        return zombie;
    }

    private static Zombie dummy(GameTestHelper helper, ServerPlayer player, double dx, double dz) {
        return dummy(helper, player, dx, dz, 300);
    }

    private static void tickFor(GameTestHelper helper, ServerPlayer player, int ticks) {
        for (int t = 1; t <= ticks; t++) helper.runAfterDelay(t, player::doTick);
    }

    private static void end(GameTestHelper helper, ServerPlayer player, Zombie... zombies) {
        for (Zombie z : zombies) z.discard();
        helper.getLevel().getEntitiesOfClass(MoonQiEntity.class, new AABB(player.blockPosition()).inflate(120)).forEach(e -> e.discard());
        EpicFightBridge.patch(player).resetHolding();
        MartialCombat.clearForTests();
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "spear_arena", batch = "moon_scroll")
    public static void theScrollTeachesTheArtFromQiCondensation(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        CultivationService.setCultivation(player, CultivationRealm.QI_SENSING, CultivationStage.EARLY);
        new ItemStack(MurimItems.CRESCENT_MOON_HALBERD_SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertFalse(MartialService.data(player).knowsArt(MartialArts.CRESCENT_MOON_HALBERD), "The scroll taught below Qi Condensation");
        CultivationService.setCultivation(player, CultivationRealm.QI_CONDENSATION, CultivationStage.EARLY);
        new ItemStack(MurimItems.CRESCENT_MOON_HALBERD_SCROLL.get()).use(player.level(), player, InteractionHand.MAIN_HAND);
        MartialData data = MartialService.data(player);
        helper.assertTrue(data.knowsArt(MartialArts.CRESCENT_MOON_HALBERD), "The scroll did not teach the art");
        for (MartialArts.Technique t : ALL) helper.assertTrue(data.hasTechnique(t.id()), "Not taught: " + t.id());
        cleanup(player);
        helper.succeed();
    }

    @GameTest(template = "streak_arena", batch = "moon_waxing", timeoutTicks = 80)
    public static void aLandedStrikeFillsTheMoonAndAFullMoonSendsACrescent(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        Zombie close = dummy(helper, player, 0, 1.6), far = dummy(helper, player, 0, 7.5);
        EpicFightBridge.patch(player).playAnimationSynchronized(Animations.SPEAR_TWOHAND_AUTO1, 0.0F);
        tickFor(helper, player, 60);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(CrescentMoonHalberd.phases(player) == 1, "A landed strike did not fill a phase: " + CrescentMoonHalberd.phases(player));
            CrescentMoonHalberd.setPhasesForTests(player, CrescentMoonHalberd.PHASES);
            EpicFightBridge.patch(player).playAnimationSynchronized(Animations.SPEAR_TWOHAND_AUTO2, 0.0F);
        });
        helper.runAfterDelay(23, () -> helper.assertTrue(CrescentMoonHalberd.phases(player) == 0, "The full moon was not spent"));
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(far.getHealth() < 300, "The crescent did not reach 7 blocks");
            end(helper, player, close, far);
        });
    }

    @GameTest(template = "spear_arena", batch = "moon_circle", timeoutTicks = 80)
    public static void moonlitCircleStrikesAllRoundAndLeavesARingThatSlows(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie front = dummy(helper, player, 0, 2.5), back = dummy(helper, player, 0, -2.5), standing = dummy(helper, player, 0, 4.2);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Moonlit Circle was not cast");
        tickFor(helper, player, 40);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(front.getHealth() < 300 && back.getHealth() < 300, "The turn missed someone: " + front.getHealth() + ", " + back.getHealth());
            helper.assertTrue(CrescentMoonHalberd.ringForTests(player), "No ring on the ground");
            helper.assertTrue(standing.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The ring did not slow who stood in it");
            end(helper, player, front, back, standing);
        });
    }

    @GameTest(template = "streak_arena", batch = "moon_hook", timeoutTicks = 80)
    public static void tideHookPullsALightFoeInAndTripsAHeavyOne(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        Zombie light = dummy(helper, player, 0, 5.5, 20);
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Tide Hook was not cast");
        tickFor(helper, player, 60);
        helper.runAfterDelay(24, () -> {
            helper.assertTrue(light.getHealth() < 20, "The hook did not strike");
            helper.assertTrue(light.distanceTo(player) < 3.2, "The light foe was not pulled in: " + light.distanceTo(player));
            light.discard();
            MartialService.clearCooldownsForTests(player);
            EpicFightBridge.patch(player).playAnimationSynchronized(Animations.BIPED_IDLE, 0.0F);
        });
        Zombie[] heavy = new Zombie[1];
        helper.runAfterDelay(26, () -> {
            heavy[0] = dummy(helper, player, 0, 5.5);
            helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Tide Hook was not cast again");
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(heavy[0].getHealth() < 300, "The hook did not strike the heavy foe");
            helper.assertTrue(heavy[0].distanceTo(player) > 4.5, "The heavy foe was pulled: " + heavy[0].distanceTo(player));
            helper.assertTrue(heavy[0].hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The heavy foe was not tripped");
            end(helper, player, heavy[0]);
        });
    }

    @GameTest(template = "streak_arena", batch = "moon_fan", timeoutTicks = 80)
    public static void fallingCrescentSendsThreeCrescentsInAFan(GameTestHelper helper) {
        ServerPlayer player = atTheEdge(helper);
        Zombie middle = dummy(helper, player, 0, 7), left = dummy(helper, player, 2.3, 7), right = dummy(helper, player, -2.3, 7);
        helper.assertTrue(MartialService.castSlot(player, 2) == CastResult.CAST, "Falling Crescent was not cast");
        tickFor(helper, player, 40);
        helper.runAfterDelay(9, () -> helper.assertTrue(CrescentMoonHalberd.crescentsForTests(player) == 3, "Not three crescents: " + CrescentMoonHalberd.crescentsForTests(player)));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(middle.getHealth() < 300 && left.getHealth() < 300 && right.getHealth() < 300,
                    "The fan missed someone: " + middle.getHealth() + ", " + left.getHealth() + ", " + right.getHealth());
            end(helper, player, middle, left, right);
        });
    }

    @GameTest(template = "spear_arena", batch = "moon_mirror", timeoutTicks = 80)
    public static void mirrorLakeStopsWhatComesFromTheFrontAndSendsItBack(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie ahead = dummy(helper, player, 0, 3), behind = dummy(helper, player, 0, -3);
        helper.assertTrue(MartialService.aim(player, 3), "Mirror Lake Stance did not start");
        helper.assertTrue(CrescentMoonHalberd.mirroring(player), "Not in the stance");
        float health = player.getHealth();
        player.invulnerableTime = 0;
        player.hurt(helper.getLevel().damageSources().mobAttack(ahead), 4);
        helper.assertTrue(player.getHealth() == health, "A blow from the front got through the mirror");
        helper.assertTrue(CrescentMoonHalberd.crescentsForTests(player) == 1, "No crescent sent back");
        tickFor(helper, player, 30);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(ahead.getHealth() < 300, "The crescent sent back missed its sender");
            player.invulnerableTime = 0;
            player.hurt(helper.getLevel().damageSources().mobAttack(behind), 4);
            helper.assertTrue(player.getHealth() < health, "A blow from behind was stopped");
            helper.assertTrue(MartialService.castSlot(player, 3) == CastResult.CAST, "The stance was not let go");
            helper.assertFalse(CrescentMoonHalberd.mirroring(player), "Still in the stance after the key was let go");
            end(helper, player, ahead, behind);
        });
    }

    @GameTest(template = "spear_arena", batch = "moon_ult", timeoutTicks = 80)
    public static void theFullMoonsBeamsStrikeTheEnemiesRound(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie east = dummy(helper, player, 5, 0), north = dummy(helper, player, -3, -6), far = dummy(helper, player, 18, 0);
        helper.assertTrue(MartialService.castSlot(player, 4) == CastResult.CAST, "Night of the Full Moon was not cast");
        helper.assertTrue(CrescentMoonHalberd.calling(player), "No moon rose");
        CrescentMoonHalberd.finishUltForTests(player);
        helper.assertTrue(east.getHealth() < 300 && north.getHealth() < 300, "The beams missed someone: " + east.getHealth() + ", " + north.getHealth());
        helper.assertTrue(far.getHealth() == 300, "A beam struck beyond 12 blocks");
        helper.assertTrue(east.getHealth() < 300 - 1, "Only one beam fell");
        end(helper, player, east, north, far);
    }
}
