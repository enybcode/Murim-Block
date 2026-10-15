package com.murimblock;

import com.murimblock.integration.epicfight.MurimWeaponCategories;
import com.murimblock.integration.epicfight.SectWeaponAnimations;
import com.murimblock.item.MurimItems;
import com.murimblock.mob.FlyingNeedleEntity;
import com.murimblock.mob.MurimEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.List;

import static com.murimblock.testing.GameTestPlayers.cleanup;
import static com.murimblock.testing.GameTestPlayers.player;

/** The sect weapons' own Epic Fight types and movesets (MurimWeapons), and the flying needles' throw. */
@GameTestHolder(Murimblock.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SectWeaponGameTests {
    private SectWeaponGameTests() { }

    /** In a player's hand, each weapon is of its own type and strikes with its own four M1s, running and air attacks. */
    @GameTest(template = "foundation_arena", batch = "sect_weapons")
    public static void eachSectWeaponHasItsOwnTypeAndMoveset(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        try {
            check(helper, player, MurimItems.IRON_FAN.get(), MurimWeaponCategories.IRON_FAN, SectWeaponAnimations.FAN_AUTO1, SectWeaponAnimations.FAN_AIR);
            check(helper, player, MurimItems.CHAIN_WHIP.get(), MurimWeaponCategories.CHAIN_WHIP, SectWeaponAnimations.WHIP_AUTO1, SectWeaponAnimations.WHIP_AIR);
            check(helper, player, MurimItems.FLYING_NEEDLES.get(), MurimWeaponCategories.FLYING_NEEDLES, SectWeaponAnimations.NEEDLES_AUTO1, SectWeaponAnimations.NEEDLES_AIR);
        } finally {
            cleanup(player);
        }
        helper.succeed();
    }

    private static void check(GameTestHelper helper, ServerPlayer player, Item item, WeaponCategory category,
                              AnimationAccessor<?> first, AnimationAccessor<?> air) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        CapabilityItem capability = EpicFightCapabilities.getItemStackCapability(player.getMainHandItem());
        helper.assertTrue(capability.getWeaponCategory() == category, item + " is not of its type: " + capability.getWeaponCategory());
        PlayerPatch<?> patch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
        helper.assertTrue(patch != null, "No Epic Fight patch on the player");
        helper.assertTrue(capability.getStyle(patch) == CapabilityItem.Styles.ONE_HAND, item + " is not held in one hand");
        List<? extends AnimationAccessor<?>> moves = capability.getAutoAttackMotion(patch);
        // Four M1s, then the running attack and the air attack.
        helper.assertTrue(moves.size() == 6 && moves.get(0) == first && moves.get(5) == air, item + " has not its moveset: " + moves);
    }

    /** A throw sends its needles in a fan; one at a still zombie four blocks ahead (the arena is five across) hurts it, and the needles end. */
    @GameTest(template = "foundation_arena", batch = "needle_throw", timeoutTicks = 60)
    public static void thrownNeedlesFlyAndHurt(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, MurimItems.FLYING_NEEDLES.toStack());
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 1, 0)));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 1, 4));
        zombie.setNoAi(true);
        zombie.setBaby(false);
        zombie.setNoGravity(true);
        float health = zombie.getHealth();
        FlyingNeedleEntity.throwFan(player, 3, 12, 0);
        int thrown = helper.getLevel().getEntities(MurimEntities.FLYING_NEEDLE.get(), player.getBoundingBox().inflate(3), e -> true).size();
        helper.assertTrue(thrown == 3, "Not three needles thrown: " + thrown);
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.getHealth() < health, "The zombie is not hurt: zombie at " + zombie.position() + ", thrower at " + player.position()
                    + " eyes " + player.getEyePosition() + ", needles " + helper.getLevel().getEntities(MurimEntities.FLYING_NEEDLE.get(), zombie.getBoundingBox().inflate(20), e -> true)
                    .stream().map(n -> n.position().toString()).toList());
            helper.assertTrue(helper.getLevel().getEntities(MurimEntities.FLYING_NEEDLE.get(), zombie.getBoundingBox().inflate(20), e -> true).isEmpty(),
                    "Needles still flying");
            cleanup(player);
        });
    }
}
