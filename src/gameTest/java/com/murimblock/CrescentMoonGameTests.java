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
import com.murimblock.martial.ReflectionEntity;
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
 * from Qi Condensation; Mirror Image leaves a reflection at a dodge that draws the mobs and shatters at the first blow;
 * each technique does what it should (the twin striking from behind, the reflections bursting, the swap, the hall of
 * four, the full moon's beams on the enemies round).
 */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrescentMoonGameTests {
    private CrescentMoonGameTests() { }

    private static final BlockPos CENTRE = new BlockPos(4, 1, 4);
    private static final MartialArts.Technique[] ALL = {MartialArts.TWIN_REFLECTION, MartialArts.SHATTERED_MIRROR, MartialArts.SWAP,
            MartialArts.HALL_OF_MIRRORS, MartialArts.NIGHT_OF_THE_FULL_MOON};

    /** A student of the art, every technique on the bar (slots 0 to 4), the halberd in hand, on stone, full Qi, facing south. */
    private static ServerPlayer adept(GameTestHelper helper) {
        return adept(helper, CENTRE);
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
        helper.getLevel().getEntitiesOfClass(ReflectionEntity.class, new AABB(player.blockPosition()).inflate(120)).forEach(e -> e.discard());
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

    @GameTest(template = "spear_arena", batch = "moon_image", timeoutTicks = 40)
    public static void aDodgeLeavesAReflectionThatDrawsTheMobsAndShattersAtTheFirstBlow(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie zombie = dummy(helper, player, 0, 3);
        zombie.setTarget(player);
        CrescentMoonHalberd.dodgedForTests(player);
        var reflections = CrescentMoonHalberd.reflections(player);
        helper.assertTrue(reflections.size() == 1, "No reflection after the dodge");
        ReflectionEntity r = reflections.get(0);
        helper.assertTrue(zombie.getTarget() == r, "The mob did not turn on the reflection");
        helper.assertFalse(r.hurt(player.damageSources().playerAttack(player), 4), "Its own cultivator shattered it");
        helper.assertTrue(r.hurt(player.damageSources().mobAttack(zombie), 4) && !r.isAlive(), "A blow did not shatter it");
        end(helper, player, zombie);
    }

    @GameTest(template = "spear_arena", batch = "moon_twin", timeoutTicks = 60)
    public static void twinReflectionStrikesTheFoeFromBehindWithTheCultivator(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie foe = dummy(helper, player, 0, 2.2);
        helper.assertTrue(MartialService.castSlot(player, 0) == CastResult.CAST, "Twin Reflection was not cast");
        var reflections = CrescentMoonHalberd.reflections(player);
        helper.assertTrue(reflections.size() == 1, "No reflection");
        helper.assertTrue(reflections.get(0).getZ() > foe.getZ(), "The reflection is not behind the foe");
        tickFor(helper, player, 20);
        var twin = reflections.get(0);
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(foe.getHealth() < 300, "The twin did not strike");
            helper.assertTrue(twin.isAlive(), "The twin did not stay");
            helper.assertTrue(MartialCombat.guardBroken(foe), "The blow from behind did not break the guard");
            end(helper, player, foe);
        });
    }

    @GameTest(template = "spear_arena", batch = "moon_shatter", timeoutTicks = 60)
    public static void shatteredMirrorBurstsEveryReflectionCuttingRoundIt(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie near = dummy(helper, player, 3.5, 0.5), far = dummy(helper, player, -3.5, 0.5);
        CrescentMoonHalberd.dodgedForTests(player);
        var r = CrescentMoonHalberd.reflections(player).get(0);
        r.moveTo(near.getX() - 1, near.getY(), near.getZ());
        helper.assertTrue(MartialService.castSlot(player, 1) == CastResult.CAST, "Shattered Mirror was not cast");
        tickFor(helper, player, 30);
        helper.runAfterDelay(24, () -> {
            helper.assertFalse(r.isAlive(), "The reflection did not burst");
            helper.assertTrue(CrescentMoonHalberd.reflections(player).isEmpty(), "A reflection was left");
            helper.assertTrue(near.getHealth() < far.getHealth(), "The shards did not cut round the reflection: " + near.getHealth() + ", " + far.getHealth());
            end(helper, player, near, far);
        });
    }

    @GameTest(template = "spear_arena", batch = "moon_swap", timeoutTicks = 40)
    public static void swapChangesPlacesWithTheNearestReflection(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        helper.assertTrue(MartialService.castSlot(player, 2) != CastResult.CAST, "Swap was cast with no reflection");
        MartialService.clearCooldownsForTests(player);
        CrescentMoonHalberd.dodgedForTests(player);
        var r = CrescentMoonHalberd.reflections(player).get(0);
        r.moveTo(player.getX() + 3, player.getY(), player.getZ());
        var mine = player.position();
        var theirs = r.position();
        com.murimblock.integration.epicfight.EpicFightBridge.patch(player).playAnimationSynchronized(yesman.epicfight.gameasset.Animations.BIPED_IDLE, 0.0F);
        helper.assertTrue(MartialService.castSlot(player, 2) == CastResult.CAST, "Swap was not cast");
        helper.assertTrue(player.position().distanceTo(theirs) < 0.1 && r.position().distanceTo(mine) < 0.1, "They did not change places");
        end(helper, player);
    }

    @GameTest(template = "spear_arena", batch = "moon_hall", timeoutTicks = 60)
    public static void hallOfMirrorsSendsFourReflectionsEachAtAFoe(GameTestHelper helper) {
        ServerPlayer player = adept(helper);
        Zombie a = dummy(helper, player, 3, 3), b = dummy(helper, player, -3, 3);
        helper.assertTrue(MartialService.castSlot(player, 3) == CastResult.CAST, "Hall of Mirrors was not cast");
        helper.assertTrue(CrescentMoonHalberd.reflections(player).size() == 4, "Not four reflections");
        CrescentMoonHalberd.hallStrikeForTests(player);
        helper.assertTrue(a.getHealth() < 300 && b.getHealth() < 300, "A foe was left alone: " + a.getHealth() + ", " + b.getHealth());
        end(helper, player, a, b);
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
